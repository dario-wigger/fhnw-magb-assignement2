package imageprocessing.fourier;

import main.Picsi;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;

import gui.OptionPane;
import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import utils.Complex;
import utils.FrequencyDomain;
import utils.Parallel;

/**
 * 2D Discrete Fourier Transform (FFT, FHT)
 * 
 * @author Christoph Stamm
 *
 */
public class FFT implements IImageProcessor {
	final static double FilterFactor = 3.8; // 3.8 is a good value
	
	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		// let the user choose the operation
		Object[] operations = { "FFT", "FHT", "FFT Inverse Filtering", "FHT Inverse Filtering" };
		int f1 = OptionPane.showOptionDialog("Fourier Transform Operation", 
				SWT.ICON_INFORMATION, operations, 0);
		if (f1 < 0) return null;
		
		Object[] output = null;
		int f2;
		
		if (f1 < 2) {
			output = new Object[]{ "Power", "Phase", "Transformed Image" };
			f2 = OptionPane.showOptionDialog("Fourier Transform Output", 
					SWT.ICON_INFORMATION, output, 0);
		} else {
			output = new Object[]{ "Blurred Image", "Deconvoluted Image", "Deconvoluted Integer Image", "Deconvoluted Integer Image with Lowpass" };
			f2 = OptionPane.showOptionDialog("Inverse Filtering Output", 
					SWT.ICON_INFORMATION, output, 0);
		}
		if (f2 < 0) return null;
		
		FrequencyDomain fd = null;
		ImageData outData = null;
		
		switch(f1) {
		case 0:
			fd = fft2D(inData);
			switch(f2) {
			case 0:
				outData = getPowerSpectrum(fd.swapQuadrants());
				break;
			case 1:
				outData = getPhaseSpectrum(fd.swapQuadrants());
				break;
			case 2:
				outData = ifft2D(fd);
				break;
			}
			break;
		case 1:
			fd = fht2D(inData);
			switch(f2) {
			case 0:
				outData = getPowerSpectrum(fd.swapQuadrants());
				break;
			case 1:
				outData = getPhaseSpectrum(fd.swapQuadrants());
				break;
			case 2:
				outData = ifht2D(fd);
				break;
			}
			break;
		case 2:
			// Inverse FFT Filtering
			outData = fft2DInverseFiltering(inData, f2);
			break;
		case 3:
			// Inverse FHT Filtering
			outData = fht2DInverseFiltering(inData, f2);
			break;
		default:
			return null;
		}
		
		return outData;
	}

	/**
	 * 2D Fast Fourier Transform (forward transform)
	 * @param inData input data
	 * @return frequency domain object
	 */
	public static FrequencyDomain fft2D(ImageData inData) {
		return fft2D(inData, 1);
	}
	
	/**
	 * 2D Fast Fourier Transform (forward transform)
	 * @param inData input data
	 * @param norm
	 * @return frequency domain object
	 */
	public static FrequencyDomain fft2D(ImageData inData, double norm) {
		return fft2D(inData, inData.width, inData.height, norm);
	}
	
	/**
	 * 2D Fast Fourier Transform (forward transform)
	 * @param inData input data
	 * @param width output width
	 * @param height output height
	 * @return frequency domain object
	 */
	public static FrequencyDomain fft2D(ImageData inData, int width, int height) {
		return fft2D(inData, width, height, 1);
	}
	
	/**
	 * 2D Fast Fourier Transform (forward transform)
	 * @param inData input data
	 * @param width output width
	 * @param height output height
	 * @param norm
	 * @return frequency domain object
	 */
	public static FrequencyDomain fft2D(ImageData inData, int width, int height, double norm) {
		int l = width - 1;
		int w = 1;
		while(l > 0) {
			l >>= 1;
			w <<= 1;
		}
		l = height - 1;
		int h = 1;
		while(l > 0) {
			l >>= 1;
			h <<= 1;
		}
		
		Complex[] row = new Complex[w];
		Complex[] col = new Complex[h];
		Complex[][] G = new Complex[h][];
		
		// create arrays
		for (int i=0; i < row.length; i++) {
			row[i] = new Complex();
		}
		
		// forward transform rows
		int rowPos = 0;
		for (int v=0; v < h; v++) {
			if (v < inData.height) {
				final int rP = rowPos;
				Parallel.For(0, Math.min(w, inData.width), u -> {
					row[u].m_re = (0xFF & inData.data[rP + u])/norm;
				});
				G[v] = FFT1D.fft(row);
				rowPos += inData.bytesPerLine;
			} else {
				G[v] = new Complex[w];
				Arrays.fill(G[v], new Complex());
			}
		}
		
		// forward transform columns
		for (int u=0; u < w; u++) {
			final int u_ = u;
			Parallel.For(0, h, v -> {
				col[v] = G[v][u_];
			});
			Complex[] Gcol = FFT1D.fft(col);
			Parallel.For(0, h, v -> {
				G[v][u_] = Gcol[v];
			});
		}
		return new FrequencyDomain(inData, width, height, G);
	}
	
	/**
	 * 2D Inverse Fast Fourier Transform
	 * @param fd frequency domain object
	 * @return output image
	 */
	public static ImageData ifft2D(FrequencyDomain fdOrig) {
		FrequencyDomain fd = fdOrig.clone();
		ImageData outData = new ImageData(fd.m_width, fd.m_height, fd.m_depth, fd.m_palette);
		Complex[] col = new Complex[fd.getSpectrumHeight()];
	
		// inverse transform rows
		for (int v=0; v < fd.getSpectrumHeight(); v++) {
			fd.m_g[v] = FFT1D.ifft(fd.m_g[v]);
		}
		
		// inverse transform columns
		for (int u=0; u < Math.min(outData.width, fd.getSpectrumWidth()); u++) {
			final int u_ = u;
			Parallel.For(0, fd.getSpectrumHeight(), v -> {
				col[v] = fd.m_g[v][u_];
			});
			Complex[] Gcol = FFT1D.ifft(col);
			Parallel.For(0, Math.min(outData.height, fd.getSpectrumHeight()), v -> {
				outData.data[u_ + v*outData.bytesPerLine] = (byte)ImageProcessing.clamp8(Gcol[v].m_re);
			});
		}
		return outData;
	}

	/**
	 * 2D Inverse Fast Fourier Transform
	 * @param fd frequency domain object
	 * @return output real part
	 */
	public static double[][] ifft2Dreal(FrequencyDomain fdOrig) {
		FrequencyDomain fd = fdOrig.clone();
		double[][] outData = new double[fd.m_height][fd.m_width];
		Complex[] col = new Complex[fd.getSpectrumHeight()];
	
		// inverse transform rows
		for (int v=0; v < fd.getSpectrumHeight(); v++) {
			fd.m_g[v] = FFT1D.ifft(fd.m_g[v]);
		}
		
		// inverse transform columns
		for (int u=0; u < Math.min(fd.m_width, fd.getSpectrumWidth()); u++) {
			final int u_ = u;
			Parallel.For(0, fd.getSpectrumHeight(), v -> {
				col[v] = fd.m_g[v][u_];
			});
			Complex[] Gcol = FFT1D.ifft(col);
			Parallel.For(0, Math.min(fd.m_height, fd.getSpectrumHeight()), v -> {
				outData[v][u_] = Gcol[v].m_re;
			});
		}
		return outData;
		
	}
	
	/**
	 * 2D Fast Hartley Transform (forward transform)
	 * @param inData input data
	 * @return frequency domain object
	 */
	public static FrequencyDomain fht2D(ImageData inData) {
		FHT fht2D = new FHT(inData);
		
		fht2D.transform();
		return new FrequencyDomain(inData, fht2D.getSpectrum());
	}
		
	/**
	 * 2D Inverse Fast Hartley Transform
	 * @param fd frequency domain object
	 * @return output image
	 */
	public static ImageData ifht2D(FrequencyDomain fd) {
		FHT fht2D = new FHT(fd.m_g, fd.m_width, fd.m_height, fd.m_depth, fd.m_palette);
		
		fht2D.inverseTransform();
		return fht2D.getImage();
	}

	/**
	 * Experiment: inverse image filtering
	 * @param inData
	 * @param option (0: blurred image, 1: deconvoluted, 2: deconvoluted integral parts, 3: lowpass of deconvoluted integral parts)
	 * @return output image
	 */
	public static ImageData fft2DInverseFiltering(ImageData inData, int option) {
		final int fsize = ((int)(Math.min(inData.width, inData.height)/FilterFactor))/2*2 + 1; assert((fsize&1) == 1);
		final int hstart = (inData.height - fsize + 1)/2; 	// hstart = ceil((h - fsize)/2), ceil corresponds to odd fsize and swapImageQuadrants
		final int wstart = (inData.width - fsize + 1)/2; 	// wstart = ceil((w - fsize)/2)
		
		ImageData filter = ImageProcessing.createImage(inData.width, inData.height, inData);

		// set box-filter to the center of filter image
		for (int v=hstart; v < hstart + fsize; v++) {
			for (int u=wstart; u < wstart + fsize; u++) {
				filter.setPixel(u, v, 1);
			}
		}
		swapImageQuadrants(filter); // makes sure that the filter center is in pos(0,0)
		
		// forward FFT
		FrequencyDomain fdf = fft2D(filter, fsize*fsize);
		FrequencyDomain fdi = fft2D(inData);
		
		// convolve inData with filter
		FrequencyDomain fdif = fdi.mul(fdf);
		
		// inverse fft 
		ImageData blurredData = ifft2D(fdif); // image with integer values
				
		switch(option) {
		default:
		case 0:
			// get and show blurred image
			return blurredData;
		case 1:
			// inverse filtering using non-integral data
			return ifft2D(fdif.div(fdf));			
		case 2:
			// inverse filtering using integral blurred data
			return ifft2D(fft2D(blurredData).div(fdf));
		case 3:
		{
			// using integral blurred data
			FrequencyDomain fdif2 = fft2D(blurredData);

			final int width = fdif.getSpectrumWidth();
        	final int height = fdif.getSpectrumHeight();
        	final int sigmoidScale = 2;
    		final int sigmoidDomain = 16*sigmoidScale;
			final int r = Math.min(width, height)/35 - sigmoidDomain/2;
    		final int hD2 = height/2;
    		final int wD2 = width/2;

    		// lowpass filter: TODO implement and improve in FrequencyDomain
        	Parallel.For(-hD2, height - hD2, v2 -> {
        		final int v = (v2 < 0) ? v2 + height : v2;
        		
    			for (int u2=-wD2; u2 < width - wD2; u2++) {
    				final int u = (u2 < 0) ? u2 + width : u2;
            		final double dist = Math.hypot(u2, v2);
            		
            		if ((u != 0 || v != 0) && dist > r) {
            			if (dist < r + sigmoidDomain) {
            				final double t = dist - r - sigmoidDomain/2;
            				fdif2.multiply(u, v, 1 - sigmoid(t/sigmoidScale));
            			} else {
            				fdif2.setValue(u, v, 0, 0);
            			}
            		}
    			}
      		});
        	
			// inverse filtering using non-integral data
			return ifft2D(fdif2.div(fdf));
		}
		}
	}
	
	private static double sigmoid(double t) {
		return 0.5 + Math.tanh(t/2)/2;
	}
	
	/**
	 * Experiment: inverse image filtering
	 * @param inData
	 * @param option (0: blurred image, 1: inverse filtered, 2: inverse filtered and integral)
	 * @return output image
	 */
	public static ImageData fht2DInverseFiltering(ImageData inData, int option) {
		final int fsize = ((int)(Math.min(inData.width, inData.height)/FilterFactor))/2*2 + 1; assert((fsize&1) == 1);
		final int hstart = (inData.height - fsize + 1)/2; 	// hstart = ceil((h - fsize)/2), ceil corresponds to odd fsize and swapImageQuadrants
		final int wstart = (inData.width - fsize + 1)/2; 	// wstart = ceil((w - fsize)/2)
		
		ImageData filter = ImageProcessing.createImage(inData.width, inData.height, inData);

		for (int v=hstart; v < hstart + fsize; v++) {
			for (int u=wstart; u < wstart + fsize; u++) {
				filter.setPixel(u, v, 1);
			}
		}
		swapImageQuadrants(filter);
		
		FHT fht2Df = new FHT(filter, fsize*fsize); // filter coefficients: 1/(fsize*fsize)
		FHT fht2Di = new FHT(inData);
		
		// forward fht 
		fht2Di.transform();			
		fht2Df.transform(); 
		
		FHT blurred = fht2Di.multiply(fht2Df); 
		
		// inverse fht transform
		blurred.inverseTransform(); 

		switch(option) {
		default:
		case 0:
			// get and show blurred image
			ImageData outData = blurred.getImage();
			return outData;
		case 1:
			blurred.transform();
			
			// inverse filtering
			FHT div = blurred.divide(fht2Df); 
			
			// inverse fht 
			div.inverseTransform(); 
			return div.getImage();
		case 2:
			blurred.round();						// convert FHT coefficients from float to int
			blurred.transform();
			
			// inverse filtering
			div = blurred.divide(fht2Df); 
			
			// inverse fht 
			div.inverseTransform(); 
			return div.getImage();
		}
	}

	/**
	 * 2D power spectrum image: log(re^2 + im^2)
	 * @param fd frequency domain object
	 * @return output image of the power spectrum
	 */
	public static ImageData getPowerSpectrum(FrequencyDomain fd) {
		final int height = fd.getSpectrumHeight();
		final int width = fd.getSpectrumWidth();

		if (fd.m_powerScale == 0) {
			final double delta = 50;
			
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
	
	  		for (int row=0; row < height; row++) {
				for (int col=0; col < width; col++) {
					final double power = fd.m_g[row][col].abs2();
					if (power < min) min = power;
					if (power > max) max = power;
				}
			}
	
	  		//System.out.println("min = " + min + ", max = " + max);
			max = Math.log(max)/2;
			min = Math.log(min)/2;
			if (Double.isNaN(min) || max - min > delta)
				min = max - delta; //display range not more than approx. e^delta
			fd.m_powerScale = 253.999/(max - min);
			fd.m_min = min;
		}

 		byte[] ps = new byte[height*width];
 		
		Parallel.For(0, height, row -> {
			final int offset = row*width;
			
			for (int col=0; col < width; col++) {
				double power = fd.m_g[row][col].abs2();
				power = (Math.log(power)/2 - fd.m_min)*fd.m_powerScale;
				if (Double.isNaN(power) || power < 0) power = 0;
				ps[offset + col] = (byte)ImageProcessing.clamp8(power + 1); // 1 is min value
			}
  		});
		
		ImageData outData = new ImageData(width, height, fd.m_depth, fd.m_palette, 1, ps);
		return outData;		
	}
	
	/**
	 * 2D phase spectrum image
	 * @param fd frequency domain object
	 * @return output image of the phase spectrum
	 */
	public static ImageData getPhaseSpectrum(FrequencyDomain fd) {
		final double PID2 = Math.PI/2;
		final int height = fd.getSpectrumHeight();
		final int width = fd.getSpectrumWidth();
		final double scale = 255/Math.PI;
		
 		byte[] ps = new byte[height*width];
 		
		Parallel.For(0, height, row -> {
			final int offset = row*width;

			for (int col=0; col < width; col++) {
				double phi = fd.m_g[row][col].arg();
				ps[offset + col] = (byte)ImageProcessing.clamp8((phi + PID2)*scale);
			}
		});
		
		ImageData outData = new ImageData(width, height, fd.m_depth, fd.m_palette, 1, ps);
		return outData;
	}
	
	/**	
	 * Swap quadrants B and D and A and C of the specified image data 
	 * so the power spectrum origin is at the center of the image.
	<pre>
	    B A
	    C D
	</pre>
	 * B.w = ceil(w/2) = w1
	 * B.h = ceil(h/2) = h1
	 * D.w = floor(w/2) = w2
	 * D.h = floor(h/2) = h2
	 */
	public static void swapQuadrants(ImageData inData) {
		final int w2 = inData.width/2,  w1 = inData.width - w2;
		final int h2 = inData.height/2, h1 = inData.height - h2;

		ImageData tA = ImageProcessing.crop(inData, w1, 0, w2, h1);
		ImageData tB = ImageProcessing.crop(inData, 0, 0, w1, h1);
		ImageData tC = ImageProcessing.crop(inData, 0, h1, w1, h2);
		ImageData tD = ImageProcessing.crop(inData, w1, h1, w2, h2);
		
		ImageProcessing.insert(inData, tA, 0, h2);
		ImageProcessing.insert(inData, tB, w2, h2);
		ImageProcessing.insert(inData, tC, w2, 0);
		ImageProcessing.insert(inData, tD, 0, 0);
	}

	/**	
	 * Swap quadrants B and D and A and C of the specified image data 
	 * so the power spectrum origin is at the center of the image.
	<pre>
	    B A
	    C D
	</pre>
	 * B.w = floor(w/2) = w1
	 * B.h = floor(h/2) = h1
	 * D.w = ceil(w/2) = w2
	 * D.h = ceil(h/2) = h2
	 */
	public static void swapImageQuadrants(ImageData inData) {
		final int w1 = inData.width/2,  w2 = inData.width - w1;
		final int h1 = inData.height/2, h2 = inData.height - h1;

		ImageData tA = ImageProcessing.crop(inData, w1, 0, w2, h1);
		ImageData tB = ImageProcessing.crop(inData, 0, 0, w1, h1);
		ImageData tC = ImageProcessing.crop(inData, 0, h1, w1, h2);
		ImageData tD = ImageProcessing.crop(inData, w1, h1, w2, h2);
		
		ImageProcessing.insert(inData, tA, 0, h2);
		ImageProcessing.insert(inData, tB, w2, h2);
		ImageProcessing.insert(inData, tC, w2, 0);
		ImageProcessing.insert(inData, tD, 0, 0);
	}
}
