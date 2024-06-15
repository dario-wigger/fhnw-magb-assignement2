package imageprocessing.fourier;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import imageprocessing.ImageProcessing;
import utils.Complex;

/**
 * 2D Fast Hartley Transform
 * @author Christoph Stamm
 *
 */
public class FHT extends FHT1D implements Cloneable {
	private int m_width, m_height;
	private boolean m_isFrequencyDomain;
	private int m_maxN;
	private float[] m_pixels;
	private int m_depth;
	private PaletteData m_palette;
	
	public FHT(ImageData inData) {
		this(inData, 1);
	}

	/**
	 * Constructor for forward transform
	 * @param inData
	 * @param norm
	 */
	public FHT(ImageData inData, int norm) {
		m_width = inData.width;
		m_height = inData.height;
		m_depth = inData.depth;
		m_palette = inData.palette;
		m_isFrequencyDomain = false;
		
		int l = Math.max(inData.width, inData.height) - 1;
		m_maxN = 1;
		while(l > 0) {
			l >>= 1;
			m_maxN <<= 1;
		}
		m_pixels = new float[m_maxN*m_maxN];
		
		int iPos = 0, oPos = 0;
		for (int v = 0; v < inData.height; v++) {
			for (int u = 0; u < inData.width; u++) {
				//pixels[oPos++] = inData.data[iPos++]/(float)norm; // signed values
				m_pixels[oPos++] = (0xFF & inData.data[iPos++])/(float)norm; // unsigned values
			}
			iPos += inData.bytesPerLine - inData.width;
			oPos += m_maxN - inData.width;
		}
	}

	/**
	 * Constructor for inverse transform
	 * @param G
	 * @param w
	 * @param h
	 * @param depth
	 * @param palette
	 */
	public FHT(Complex[][] G, int w, int h, int depth, PaletteData palette) {
		m_width = w;
		m_height = h;
		this.m_depth = depth;
		this.m_palette = palette;
		m_maxN = G.length;
		m_maxN = G[0].length;
		m_pixels = new float[m_maxN*m_maxN];
		m_isFrequencyDomain = true;
		
		int base = 0;
		for (int row = 0; row < m_maxN; row++) {
	        int offs = ((m_maxN - row)%m_maxN)*m_maxN;
	        
	        for (int col = 0; col < m_maxN; col++) {
	        	int omegaPlus = base + col;
	        	int omegaNeg = offs + ((m_maxN - col)%m_maxN);
	        	Complex c = G[row][col];
	        	
	        	// compute FHT using FT
	        	m_pixels[omegaPlus] = (float)(c.m_re - c.m_im);
	        	m_pixels[omegaNeg]  = (float)(c.m_re + c.m_im);
	        }
	        base += m_maxN;
		}
	}

	private FHT(FHT fht2D, float[] fht) {
		m_maxN = fht2D.m_maxN;
		m_width = fht2D.m_width;
		m_height = fht2D.m_height;
		m_depth = fht2D.m_depth;
		m_palette = fht2D.m_palette;
		
		assert fht.length == m_maxN*m_maxN : "fht has wrong length";
		m_pixels = fht;
		m_isFrequencyDomain = true;		
	}
	
	@Override
	public FHT clone() {
		FHT res = new FHT(this, m_pixels.clone());
		m_isFrequencyDomain = res.m_isFrequencyDomain;
		return res;
	}

	/**
	 * Performs a forward transform, converting this image into the frequency
	 * domain. The image contained in this FHT2D must be square and its width must
	 * be a power of 2.
	 */
	public void transform() {
		rc2DFHT(m_pixels, false, m_maxN);
		m_isFrequencyDomain = true;
	}

	/**
	 * Performs an inverse transform, converting this image into the space
	 * domain. The image contained in this FHT2D must be square and its width must
	 * be a power of 2.
	 */
	public void inverseTransform() {
		rc2DFHT(m_pixels, true, m_maxN);
		m_isFrequencyDomain = false;
	}

	/** Performs a 2D FHT (Fast Hartley Transform). */
	private void rc2DFHT(float[] x, boolean inverse, int maxN) {
		initializeTables(maxN);
		for (int row = 0; row < maxN; row++)
			dfht3(x, row*maxN, inverse, maxN);
		transposeR(x, maxN);
		for (int row = 0; row < maxN; row++)
			dfht3(x, row * maxN, inverse, maxN);
		transposeR(x, maxN);

		int mRow, mCol;
		float A, B, C, D, E;
		for (int row = 0; row <= maxN / 2; row++) { // Now calculate actual Hartley transform
			for (int col = 0; col <= maxN / 2; col++) {
				mRow = (maxN - row) % maxN;
				mCol = (maxN - col) % maxN;
				A = x[row * maxN + col]; // see Bracewell, 'Fast 2D Hartley Transf.' IEEE Procs. 9/86
				B = x[mRow * maxN + col];
				C = x[row * maxN + mCol];
				D = x[mRow * maxN + mCol];
				E = ((A + D) - (B + C)) / 2;
				x[row * maxN + col] = A - E;
				x[mRow * maxN + col] = B + E;
				x[row * maxN + mCol] = C + E;
				x[mRow * maxN + mCol] = D - E;
			}
		}
	}

	public ImageData getImage() {
		ImageData outData = new ImageData(m_width, m_height, m_depth, m_palette);
		
		int iPos = 0, oPos = 0;
		for(int v = 0; v < outData.height; v++) {
			for(int u = 0; u < outData.width; u++) {
				outData.data[oPos++] = (byte)ImageProcessing.clamp8(m_pixels[iPos++]);	// unsigned values
				//outData.data[oPos++] = (byte)ImageProcessing.signedClamp8(pixels[iPos++]);	// signed values				
			}
			iPos += m_maxN - outData.width;
			oPos += outData.bytesPerLine - outData.width;
		}
		
		return outData;
	}
	
	public Complex[][] getSpectrum() {
		if (!m_isFrequencyDomain)
			throw new  IllegalArgumentException("Frequency domain image required");
		
		Complex[][] G = new Complex[m_maxN][m_maxN];

		int base = 0;
		for (int row = 0; row < m_maxN; row++) {
	        final int offs = ((m_maxN - row)%m_maxN)*m_maxN;
	        
	        for (int col = 0; col < m_maxN; col++) {
	        	final int omegaPlus = base + col;
	        	final int omegaNeg = offs + ((m_maxN - col)%m_maxN);
	        	
	        	// compute FT using FHT
	        	G[row][col] = new Complex((m_pixels[omegaPlus] + m_pixels[omegaNeg])*0.5, (-m_pixels[omegaPlus] + m_pixels[omegaNeg])*0.5);
	        }
	        base += m_maxN;
		}
		return G;
	}

	/*void changeValues(ImageData inData, int v1, int v2, int v3) {
		for (int i=0; i < pixels.length; i++) {
			int v = inData.data[i] & 0xFF;
			if (v >= v1 && v <= v2)
				pixels[i] = (byte)v3;
		}
	}*/

	/** Returns the image resulting from the point by point Hartley multiplication
		of this image and the specified image. Both images are assumed to be in
		the frequency domain. Multiplication in the frequency domain is equivalent 
		to convolution in the space domain. */
	public FHT multiply(FHT fht) {
		return multiply(fht, false);
	}

	/** Returns the image resulting from the point by point Hartley conjugate 
		multiplication of this image and the specified image. Both images are 
		assumed to be in the frequency domain. Conjugate multiplication in
		the frequency domain is equivalent to correlation in the space domain. */
	public FHT conjugateMultiply(FHT fht) {
		return multiply(fht, true);
	}

	FHT multiply(FHT fht, boolean conjugate) {
		float[] p1 = m_pixels;
		float[] p2 = fht.m_pixels;
		float[] tmp = new float[m_maxN*m_maxN];
		
		for (int r = 0; r < m_maxN; r++) {
			final int rowMod = (m_maxN - r) % m_maxN;
			
			for (int c = 0; c < m_maxN; c++) {
				final int colMod = (m_maxN - c) % m_maxN;
				final double h2e = (p2[r*m_maxN + c] + p2[rowMod*m_maxN + colMod])/2;
				final double h2o = (p2[r*m_maxN + c] - p2[rowMod*m_maxN + colMod])/2;
				if (conjugate) 
					tmp[r*m_maxN + c] = (float)(p1[r*m_maxN + c]*h2e - p1[rowMod*m_maxN + colMod]*h2o);
				else
					tmp[r*m_maxN + c] = (float)(p1[r*m_maxN + c]*h2e + p1[rowMod*m_maxN + colMod]*h2o);
			}
		}
		return new FHT(this, tmp);
	}
		
	/** Returns the image resulting from the point by point Hartley division
		of this image by the specified image. Both images are assumed to be in
		the frequency domain. Division in the frequency domain is equivalent 
		to deconvolution in the space domain. */
	public FHT divide(FHT fht) {
		float[] p1 = m_pixels;
		float[] p2 = fht.m_pixels;
		float[] out = new float[m_maxN*m_maxN];
		
		for (int r = 0; r < m_maxN; r++) {
			final int rowMod = (m_maxN - r) % m_maxN;
			
			for (int c = 0; c < m_maxN; c++) {
				final int colMod = (m_maxN - c) % m_maxN;
				
				double mag = p2[r*m_maxN + c] * p2[r*m_maxN + c] + p2[rowMod*m_maxN + colMod]*p2[rowMod*m_maxN + colMod];
				if (mag < 1e-20) mag = 1e-20;
				final double h2e = (p2[r*m_maxN + c] + p2[rowMod*m_maxN + colMod]);
				final double h2o = (p2[r*m_maxN + c] - p2[rowMod*m_maxN + colMod]);
				final double tmp = (p1[r*m_maxN + c]*h2e - p1[rowMod*m_maxN + colMod]*h2o);
				out[r*m_maxN + c] = (float)(tmp/mag);
			}
		}
		return new FHT(this, out);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FHT) {
			FHT fht = (FHT)o;
			if (m_width != fht.m_width) return false;
			if (m_height != fht.m_height) return false;
			if (m_isFrequencyDomain != fht.m_isFrequencyDomain) return false;
			if (m_maxN != fht.m_maxN) return false;
			if (m_depth != fht.m_depth) return false;
			final int size = m_height*m_width;
			for(int i = 0; i < size; i++) {
				if (m_pixels[i] != fht.m_pixels[i]) return false;
			}
			return true;
		} else {
			return false;
		}
	}
	
	public void round() {
		assert !m_isFrequencyDomain : "frequency domain is not expected";
	
		final int size = m_height*m_width;
		for(int i = 0; i < size; i++) {
			m_pixels[i] = Math.round(m_pixels[i]);
		}
	}
}
