package imageprocessing.colors;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import imageprocessing.ImageProcessing;
import main.Picsi;
import utils.Parallel;

/**
 * Color space visualizations
 * @author Christoph Stamm
 *
 */
public class ColorSpaces {
	private static final int ImageSize = 1000;
	private static boolean ImprovedLabDistance = false;
	
	public static ImageData grayscale() {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_GRAY);
		
		final int s = ImageSize*9/10;
		final int margin = (ImageSize - s)/2;
		final int steps = 10;
		final int q = s/steps;
		final int q2 = 2*q;
		final int q3 = q2 + q;

		Parallel.For(0, s, v -> {
			int p = 0;
			
			for (int u = 0; u < s; u++) {
				// blocks
				if (v < q) {
					p = (u/q)*255/(steps - 1);
				} else if (v >= s - q) {
					p = 255 - (u/q)*255/(steps - 1);					
				}
				if (u < q) {
					p = (v/q)*255/(steps - 1);
				} else if (u >= s - q) {
					p = 255 - (v/q)*255/(steps - 1);					
				}
				
				// sweeps
				if (q <= v && v < s - q && q <= u && u < s - q) {
					final int v2 = v - q;
					final int u2 = u - q;
					final int s2 = s - q2;
					
					if (v2 < q) {
						if (u2 >= v2 && u2 <= s2 - v2) {
							p = 256*u2/s2;
						}
					} else if (v2 >= s2 - q) {
						if (u2 >= s2 - v2 && u2 <= v2) {
							p = 255 - 256*u2/s2;
						}					
					}
					if (u2 < q) {
						if (v2 >= u2 && v2 <= s2 - u2) {
							p = 256*v2/s2;
						}
					} else if (u2 >= s2 - q) {
						if (v2 >= s2 - u2 && v2 <= u2) {
							p = 255 - 256*v2/s2;
						}					
					}
				}
				
				// center
				if (q2 <= v && v < s - q2 && q2 <= u && u < s - q2) {
					p = 0;
					
					// diagonal sweep
					if (q3 <= v && v < s - q3 && q3 <= u && u < s - q3) {
						final int v3 = v - q3;
						final int u3 = u - q3;
						final int s3 = 2*s - 4*q3;
					
						p = 256*(v3 + u3)/s3;
					}
					
				}					
				outData.setPixel(u + margin, v + margin, p);
			}
		});
		
		return outData;
	}
	
	public static ImageData rgbTestImage() {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int steps = 14;
		final int s = (ImageSize*9/10/steps)*steps;
		final int q = s/steps;
		final int margin = (ImageSize - s)/2;

		for(int i = 0; i < steps; i += 2) {
			final int channels = 1 + i/2;
			
			// blocks
			Parallel.For(i*q, i*q + q, v -> {
				RGB rgb = new RGB(0,0,0);
				
				for (int u = 0; u < s; u++) {
					final int p = ((channels & 1) != 0) ? (1 + u/q)*255/(steps) : (1 + (s - u)/q)*255/(steps);
					if ((channels & 1) != 0) rgb.red = p;
					if ((channels & 2) != 0) rgb.green = p;
					if ((channels & 4) != 0) rgb.blue = p;
					final int px = outData.palette.getPixel(rgb);
					outData.setPixel(u + margin, v + margin, px);
				}
			});
			
			// sweep
			Parallel.For(i*q + q, i*q + 2*q, v -> {
				RGB rgb = new RGB(0,0,0);
				
				for (int u = 0; u < s; u++) {
					final int p = ((channels & 1) != 0) ? 256*u/s : 256*(s - u)/s;
					if ((channels & 1) != 0) rgb.red = p;
					if ((channels & 2) != 0) rgb.green = p;
					if ((channels & 4) != 0) rgb.blue = p;
					final int px = outData.palette.getPixel(rgb);
					outData.setPixel(u + margin, v + margin, px);
				}
			});
		}
				
		return outData;
	}
	
	public static ImageData rgbCube(boolean whiteOnTop) {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final double f = Math.sqrt(3)/2;
		final double mB = f*2;
		final int center = ImageSize/2;
		final int s = center*9/10;
		final double h = f*s;
		
		Parallel.For(center - (int)h, center + (int)h, v -> {
			final int y = center - v;
			final double yh = y/h;
			
			double[] rgb = new double[3];
			RGB c = new RGB(0,0,0);
			
			for (int u = center - s; u < center + s; u++) {
				final int x = u - center;
				final double xs = (double)x/s;
				
				if (y <= 0) {
					if (y <= -mB*x) {
						// RG
						rgb[1] = -yh;
						rgb[0] = rgb[1]/2 - xs;
						rgb[2] = 0;
					} else {
						// GB
						rgb[0] = 0;
						rgb[1] = xs - yh/2;
						rgb[2] = xs + yh/2;	
					}
				} else {
					if (y > mB*x) {
						// RB
						rgb[1] = 0;
						rgb[2] = yh;
						rgb[0] = rgb[2]/2 - xs;
					} else {
						// GB
						rgb[0] = 0;
						rgb[1] = xs - yh/2;
						rgb[2] = xs + yh/2;	
					}
				}
				if (whiteOnTop) {
					rgb[0] = 1 - rgb[0];
					rgb[1] = 1 - rgb[1];
					rgb[2] = 1 - rgb[2];
				}
				
				if (rgb[0] >= 0 && rgb[0] <= 1 && rgb[1] >= 0 && rgb[1] <= 1 && rgb[2] >= 0 && rgb[2] <= 1) {
					c.red = ImageProcessing.normalized2byte(rgb[0]);
					c.green = ImageProcessing.normalized2byte(rgb[1]);
					c.blue = ImageProcessing.normalized2byte(rgb[2]);
					outData.setPixel(u, v, outData.palette.getPixel(c));
				}
			}
		});
		
		return outData;
	}
	
	public static ImageData sRGBGamut() {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		final int yResolution = 100;
		
		// draw sRGB gamut
		for(int w = 0; w < yResolution; w++) {
			final double Y = (double)w/yResolution;
			
			Parallel.For(0, ImageSize + 1, v -> {
				final double y = 1 - (double)v/ImageSize;
				RGB c = new RGB(0,0,0);
				double[] rgb = new double[3];
				
				for (int u=0; u <= ImageSize; u++) {
					final double x = (double)u/ImageSize;
					final double z = 1 - x - y;
					
					if (z >= 0) {
						final double[] XYZ = { Y*x/y, Y, Y*z/y};
						
						xyz2rgb(XYZ, rgb);
						rgb2sRGB(rgb, rgb);
						
						if (rgb[0] >= 0 && rgb[0] <= 1 && rgb[1] >= 0 && rgb[1] <= 1 && rgb[2] >= 0 && rgb[2] <= 1) {
							c.red = ImageProcessing.normalized2byte(rgb[0]);
							c.green = ImageProcessing.normalized2byte(rgb[1]);
							c.blue = ImageProcessing.normalized2byte(rgb[2]);
							outData.setPixel(u, v, outData.palette.getPixel(c));
						}
					}
				}
			});
		}
		
		return outData;
	}

	public static ImageData yuv(boolean whiteOnTop) {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int yResolution = 200;
		final int center = ImageSize/2;
		final double s = center*0.9;
		final double uMax = 0.436;
		final double vMax = 0.615;
		
		// draw YUV
		for(int w = 0; w < yResolution; w++) {
			final double Y = (double)((whiteOnTop) ? w : (yResolution - w - 1))/yResolution;
			
			Parallel.For(0, ImageSize, v -> {
				final int y = center - v;
				RGB c = new RGB(0,0,0);
				double[] rgb = new double[3];
				
				for (int u=0; u < ImageSize; u++) {
					rgb[0] = Y;
					rgb[1] = (u - center)*uMax/s;
					rgb[2] = y*vMax/s;
						
					yuv2rgb(rgb, rgb);
					//rgb2yuv(rgb, rgb);
					//rgb[0] = gammaCorrection(rgb[0]);
					//System.out.println(Arrays.toString(rgb));
						
					if (rgb[0] >= 0 && rgb[0] <= 1 && rgb[1] >= 0 && rgb[1] <= 1 && rgb[2] >= 0 && rgb[2] <= 1) {
						c.red = ImageProcessing.normalized2byte(rgb[0]);
						c.green = ImageProcessing.normalized2byte(rgb[1]);
						c.blue = ImageProcessing.normalized2byte(rgb[2]);
						outData.setPixel(u, v, outData.palette.getPixel(c));
					}
				}
			});
		}
		
		return outData;
	}

	public static ImageData xyz(boolean whiteOnTop) {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int yResolution = 200;
		final int s = ImageSize*9/10;
		final int margin = (ImageSize - s)/2;
		final int sMax = s - 1;
		final double zFactor = 1.1;
		
		// draw XYZ
		for(int y = 0; y < yResolution; y++) {
			final double Y = (double)((whiteOnTop) ? y : (yResolution - y - 1))/yResolution;
			//final double Y = 0.5;
			
			Parallel.For(0, s, v -> {
				RGB c = new RGB(0,0,0);
				double[] xyz = new double[] { 0, Y, zFactor*(sMax - v)/sMax };
				double[] rgb = new double[3];
				
				for (int u=0; u < s; u++) {
					xyz[0] = (double)u/sMax;
						
					xyz2rgb(xyz, rgb);
					//System.out.println(Arrays.toString(rgb));
						
					if (rgb[0] >= 0 && rgb[0] <= 1 && rgb[1] >= 0 && rgb[1] <= 1 && rgb[2] >= 0 && rgb[2] <= 1) {
						c.red = ImageProcessing.normalized2byte(rgb[0]);
						c.green = ImageProcessing.normalized2byte(rgb[1]);
						c.blue = ImageProcessing.normalized2byte(rgb[2]);
						outData.setPixel(margin + u, margin + v, outData.palette.getPixel(c));
					}
				}
			});
		}
		
		return outData;
	}

	public static ImageData hsv(boolean whiteOnTop) {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int center = ImageSize/2;
		final int maxRadius = center*9/10;
		final int maxRadius2 = maxRadius*maxRadius;
		
		// draw HSV cone
		Parallel.For(0, ImageSize, v -> {
			final int y = center - v;
			final int y2 = y*y;
			RGB c = new RGB(0,0,0);
			double[] hsv = new double[3];
			double[] rgb = new double[3];

			hsv[2] = 1;
			
			for (int u=0; u < ImageSize; u++) {
				final int x = u - center;
				final int x2 = x*x;
				final int r2 = x2 + y2;
				
				if (r2 <= maxRadius2) {
					hsv[0] = Math.atan2(y,x)/Math.PI/2;
					if (hsv[0] < 0) hsv[0] += 1;
					final double chroma = Math.sqrt(r2)/maxRadius; 
					hsv[2] = (whiteOnTop) ? 1 : chroma; // value V
					//hsv[1] = (hsv[2] == 0) ? 0: chroma/hsv[2]; // saturation S (here always 1 for whiteOnTop = false)
					hsv[1] = chroma; // chroma C instead of saturation S (here: same visualization, but simpler computation)
					
					hsv2rgb(hsv, rgb);
					
					c.red = ImageProcessing.normalized2byte(rgb[0]);
					c.green = ImageProcessing.normalized2byte(rgb[1]);
					c.blue = ImageProcessing.normalized2byte(rgb[2]);
					outData.setPixel(u, v, outData.palette.getPixel(c));
				}
			}
		});			
				
		return outData;
	}

	public static ImageData hsvWheel() {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int center = ImageSize/2;
		final int maxRadius = center*9/10;
		final int maxRadius2 = maxRadius*maxRadius;
		final int minRadius = maxRadius*7/10;
		final int minRadius2 = minRadius*minRadius;
		final int nCircleColors = 36;
		final double PIT2 = Math.PI*2;
		final double deltaRad = PIT2/nCircleColors;
		final double marginRad = PIT2/360;
		final int startOffset = 0;
		
		// collect nColors fully saturated colors along a color circle
		double[][] circle = new double[nCircleColors][3];
		double[] hsv = new double[] { 0, 1, 1 };
		
		for(int i = 0; i < nCircleColors; i++) {
			hsv[0] = (i + startOffset)%nCircleColors/(double)nCircleColors;
			hsv2rgb(hsv, circle[i]);
		}

		// visualize color circle
		Parallel.For(0, ImageSize, v -> {
			final int dy = center - v;
			final int dy2 = dy*dy;
			RGB c = new RGB(0,0,0);

			for (int u=0; u < ImageSize; u++) {
				final int dx = u - center;
				final int r2 = dx*dx + dy2;
				
				if (minRadius2 <= r2 && r2 <= maxRadius2) {
					double rad = Math.atan2(dy, dx);
					if (rad < 0) rad += PIT2;
					
					int index = (int)Math.round(rad/deltaRad);
					if (index == nCircleColors) index = 0;
					final double indexRad = index*deltaRad;
					final double maxRad = indexRad + deltaRad/2 - marginRad;
					final double minRad = indexRad - deltaRad/2 + marginRad;
					
					if (index == 0 && rad > Math.PI) rad -= PIT2;
					if (minRad <= rad && rad <= maxRad) {
						double[] col = circle[index];
						
						c.red = ImageProcessing.normalized2byte(col[0]);
						c.green = ImageProcessing.normalized2byte(col[1]);
						c.blue = ImageProcessing.normalized2byte(col[2]);
	
						outData.setPixel(u, v, outData.palette.getPixel(c));
					}
				}
			}
		});

		return outData;
	}
	
	public static ImageData lab(boolean whiteOnTop) {
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int center = ImageSize/2;
		final int lResolution = 200;
		final double sizeFactor = 1.1;
		
		// draw Lab color space
		for(int l = 0; l < lResolution; l++) {
			final double L = (double)((whiteOnTop) ? l : (lResolution - l - 1))/lResolution;
			
			Parallel.For(0, ImageSize, v -> {
				final int y = center - v;
				RGB c = new RGB(0,0,0);
				double[] lab = new double[3];
				double[] rgb = new double[3];
	
				lab[0] = L;
				lab[2] = sizeFactor*(double)y/center;
				
				for (int u=0; u < ImageSize; u++) {
					final int x = u - center;
					
					lab[1] = sizeFactor*(double)x/center;
					lab2xyz(lab, rgb);
					xyz2rgb(rgb, rgb);
					rgb2sRGB(rgb, rgb);
					
					if (rgb[0] >= 0 && rgb[0] <= 1 && rgb[1] >= 0 && rgb[1] <= 1 && rgb[2] >= 0 && rgb[2] <= 1) {
						c.red = ImageProcessing.normalized2byte(rgb[0]);
						c.green = ImageProcessing.normalized2byte(rgb[1]);
						c.blue = ImageProcessing.normalized2byte(rgb[2]);
						outData.setPixel(u, v, outData.palette.getPixel(c));
					}
				}
			});
		}
		
		return outData;
	}

	public static ImageData labWheel() {
		// TODO zwischen magenta und blau die Abstände von H übernehmen
		ImageData outData = ImageProcessing.createImage(ImageSize, ImageSize, Picsi.IMAGE_TYPE_RGB);
		
		final int center = ImageSize/2;
		final int maxRadius = center*9/10;
		final int maxRadius2 = maxRadius*maxRadius;
		final int minRadius = maxRadius*7/10;
		final int minRadius2 = minRadius*minRadius;
		final int nColors = 3600;
		final int nCircleColors = 36;
		final int maxIterations = 100;
		final double relError = 0.01;
		final double PIT2 = Math.PI*2;
		final double deltaRad = PIT2/nCircleColors;
		final double marginRad = PIT2/360;
		final int startOffset = 0;
		
		// collect nColors fully saturated colors along a color circle
		double[][] colors = new double[nColors + 1][3];
		double[][] circle = new double[nCircleColors][];
		double[] hsv = new double[] { 0, 1, 1 };
		
		for(int i = 0; i < nColors; i++) {
			hsv[0] = (i + startOffset)%nColors/(double)nColors;
			//if (hsv[0] > 0.6 && hsv[0] < 0.8) System.out.println("HSV = " + Arrays.toString(hsv));
			hsv2rgb(hsv, colors[i]);
			rgb2xyz(colors[i], colors[i]);
			//if (hsv[0] > 0.6 && hsv[0] < 0.8) System.out.println("XYZ = " + Arrays.toString(colors[i]));
			xyz2lab(colors[i], colors[i]);
			//if (hsv[0] > 0.6 && hsv[0] < 0.8) System.out.println("Hue = " + i/(double)nColors + ", LAB = " + Arrays.toString(colors[i]) + ", diff = " + distance2(colors[i], colors[i-1]));
		}
		colors[nColors] = colors[0]; // reference to first color

		// compute mean squared distance
		double mse = 0;
		for(int i = 0; i < nColors; i++) {
			mse += distance2(colors[i], colors[i+1]);
		}		
		//mse /= nColors;
		
		// choose nCircleColors colors with equidistant distances between neighboring colors, starting with red
		double d2 = mse/nCircleColors; // expected mean square distance between two colors in the circle
		boolean searching = true;
		int k = 0;
		
		do {
			//System.out.println(d2);
			final double d = Math.sqrt(d2);
			int i = 1, j = 1;
			
			circle[0] = colors[0]; // reference only
			while(i < nColors && j < nCircleColors) {
				while(i < nColors && distance2(circle[j-1], colors[i]) < d2) i++;
				
				final double delta1 = Math.abs(d2 - distance2(circle[j-1], colors[i-1]));
				final double delta2 = Math.abs(d2 - distance2(circle[j-1], colors[i]));
				//System.out.println("delta1 = " + delta1 + ", delta2 = " + delta2);
				
				if (delta1 < delta2) {
					circle[j++] = colors[i-1];
				} else {
					circle[j++] = colors[i++];
				}
			}
			
			if (j < nCircleColors) {
				// circle is not full -> reduce d2
				// j colors have been set
				d2 *= (double)j/nCircleColors;
			} else {
				final double relDiff = Math.sqrt(distance2(circle[nCircleColors - 1], circle[0]))/d;
				
				if (relDiff < 1.0 - relError) {
					// dist between last and first color is too small -> reduce slightly d2
					d2 *= 0.99;
				} else if (relDiff > 1.0 + relError) {
					// dist between last and first color is too large -> increase d2
					// only i colors have been visited
					d2 *= (double)nColors/i;
				} else {
					double delta1 = distance2(circle[nCircleColors - 1], circle[0]);
					double delta2 = distance2(circle[0], circle[1]);
					double delta;

					if (relDiff > 1.0) {
						// dist between last and first color is larger than d
						// choose another first color
						i = nColors;
						do {
							i--;
							delta = delta1 + delta2;
							delta1 = Math.abs(d - Math.sqrt(distance2(circle[nCircleColors - 1], colors[i])));
							delta2 = Math.abs(d - Math.sqrt(distance2(circle[1], colors[i])));
						} while(delta1 + delta2 < delta);
						circle[0] = colors[i+1];
					} else if (relDiff < 1.0) {
						// dist between last and first color is smaller than d
						// choose another first color
						i = 0;
						do {
							i++;
							delta = delta1 + delta2;
							delta1 = Math.abs(d - Math.sqrt(distance2(circle[nCircleColors - 1], colors[i])));
							delta2 = Math.abs(d - Math.sqrt(distance2(circle[1], colors[i])));
						} while(delta1 + delta2 < delta);
						circle[0] = colors[i-1];
					}
					searching = false;
				}
			}
			k++;
		} while(searching && k < maxIterations);
		
		final double d = Math.sqrt(d2);
		System.out.println("expected distance = " + d + ", iterations = " + k);
		
		//for(double[] lab: circle) System.out.println(lab[0] + "," + lab[1] + "," + lab[2]);
		
		// compute measures
		double mean = 0;
		double var = 0;
		mse = 0;
		//System.out.println(mse);
		for(int j = 0; j < nCircleColors; j++) {
			final double dd = Math.sqrt(distance2(circle[j], circle[(j+1)%nCircleColors]));
			//System.out.println(distance2(d, dd));
			mean += dd;
			var += dd*dd;
			mse += distance2(d, dd);
		}
		System.out.println("MSE = " + mse/nCircleColors);
		System.out.println("mean distance = " + mean/nCircleColors);
		System.out.println("variance = " + (var/nCircleColors - mean*mean/(nCircleColors*nCircleColors)));
		
		// convert colors 
		for(int j = 0; j < nCircleColors; j++) {
			double[] col = circle[j];

			lab2xyz(col, col);
			xyz2rgb(col, col);
			rgb2sRGB(col, col);
			//System.out.println(col[0] + "," + col[1] + "," + col[2]);
		}

		// visualize color circle
		Parallel.For(0, ImageSize, v -> {
			final int dy = center - v;
			final int dy2 = dy*dy;
			RGB c = new RGB(0,0,0);

			for (int u=0; u < ImageSize; u++) {
				final int dx = u - center;
				final int r2 = dx*dx + dy2;
				
				if (minRadius2 <= r2 && r2 <= maxRadius2) {
					double rad = Math.atan2(dy, dx);
					if (rad < 0) rad += PIT2;
					
					int index = (int)Math.round(rad/deltaRad);
					if (index == nCircleColors) index = 0;
					final double indexRad = index*deltaRad;
					final double maxRad = indexRad + deltaRad/2 - marginRad;
					final double minRad = indexRad - deltaRad/2 + marginRad;
					
					if (index == 0 && rad > Math.PI) rad -= PIT2;
					if (minRad <= rad && rad <= maxRad) {
						double[] col = circle[index];
						
						c.red = ImageProcessing.normalized2byte(col[0]);
						c.green = ImageProcessing.normalized2byte(col[1]);
						c.blue = ImageProcessing.normalized2byte(col[2]);
	
						outData.setPixel(u, v, outData.palette.getPixel(c));
					}
				}
			}
		});
		return outData;
	}
	
	/**
	 * HSV to RGB
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/HSL_and_HSV
	 * input and output can be the same array
	 * @param hsv input
	 * @param rgb output
	 */
	public static void hsv2rgb(double[] hsv, double[] rgb) {
		final double h = (6*hsv[0])%6;
		final double s = hsv[1];
		final double v = hsv[2];
		final int c1 = (int)h;
		final double c2 = h - c1;
		final double x = (1 - s)*v;
		final double y = (1 - c2*s)*v;
		final double z = (1 - s*(1 - c2))*v;
		
		switch(c1) {
		case 0: rgb[0] = v; rgb[1] = z; rgb[2] = x; break;
		case 1: rgb[0] = y; rgb[1] = v; rgb[2] = x; break;
		case 2: rgb[0] = x; rgb[1] = v; rgb[2] = z; break;
		case 3: rgb[0] = x; rgb[1] = y; rgb[2] = v; break;
		case 4: rgb[0] = z; rgb[1] = x; rgb[2] = v; break;
		case 5: rgb[0] = v; rgb[1] = x; rgb[2] = y; break;
		}
	}

	/**
	 * RGB to HSV
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/HSL_and_HSV
	 * input and output can be the same array
	 * @param rgb input
	 * @param hsv output
	 */
	public static void rgb2hsv(double[] rgb, double[] hsv) {
		final double R = rgb[0];
		final double G = rgb[1];
		final double B = rgb[2];
		final double M = Math.max(Math.max(R, G), B);
		final double m = Math.min(Math.min(R, G), B);
		final double c = M - m;
		double h;
		
		if (c > 0) {
			if (M == R) {
				h = (G - B)/c;
				if (h < 0) h += 6;
			} else if (M == G) {
				h = 2 + (B - R)/c;
			} else {
				h = 4 + (R - G)/c;
			}
			hsv[0] = h/6;
		}
		hsv[1] = (M == 0) ? 0 : c/M;
		hsv[2] = M;
	}
	
	/**
	 * CIE L*a*b* to CIE XYZ tristimulus values, white point: D65
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/CIELAB_color_space
	 * input and output can be the same array
	 * @param Lab input
	 * @param XYZ output
	 */
	public static void lab2xyz(double[] Lab, double[] XYZ) {
		final double Xn = 0.950489;
		final double Yn = 1;
		final double Zn = 1.088840;
		final double l = (Lab[0] + 0.16)/1.16;
		
		XYZ[0] = Xn*inverseLabF(l + Lab[1]/5);
		XYZ[1] = Yn*inverseLabF(l);
		XYZ[2] = Zn*inverseLabF(l - Lab[2]/2);
	}

	/**
	 * CIE XYZ tristimulus values to CIE L*a*b*, white point: D65
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/CIELAB_color_space
	 * input and output can be the same array
	 * @param XYZ input
	 * @param Lab output
	 */
	public static void xyz2lab(double[] XYZ, double[] Lab) {
		final double Xn = 0.950489;
		final double Yn = 1;
		final double Zn = 1.088840;
		final double fY = labF(XYZ[1]/Yn);
		
		Lab[1] = 5*(labF(XYZ[0]/Xn) - fY);
		Lab[0] = fY*1.16 - 0.16;
		Lab[2] = 2*(fY - labF(XYZ[2]/Zn));
	}
	
	/**
	 * linear RGB to CIE XYZ tristimulus values, white point: D65
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/SRGB
	 * input and output can be the same array
	 * @param RGB input
	 * @param XYZ output
	 */
	public static void rgb2xyz(double[] RGB, double[] XYZ) {
		final double R = RGB[0];
		final double G = RGB[1];
		final double B = RGB[2];
	
		XYZ[0] = 0.41239080*R + 0.35758434*G + 0.18048079*B;
		XYZ[1] = 0.21263901*R + 0.71516868*G + 0.07219232*B;
		XYZ[2] = 0.01933082*R + 0.11919478*G + 0.95053215*B;
	}
	
	/**
	 * CIE XYZ tristimulus values to linear RGB, white point: D65
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/SRGB
	 * input and output can be the same array
	 * @param XYZ input
	 * @param RGB output
	 */
	public static void xyz2rgb(double[] XYZ, double[] RGB) {
		final double X = XYZ[0];
		final double Y = XYZ[1];
		final double Z = XYZ[2];
		
		RGB[0] = +3.24096994*X -1.53738318*Y -0.49861076*Z;
		RGB[1] = -0.96924364*X +1.8759675*Y  +0.04155506*Z;
		RGB[2] = +0.05563008*X -0.20397696*Y +1.05697151*Z;
	}

	/**
	 * linear RGB to YUV (HDTV with BT.709)
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/YUV
	 * input and output can be the same array
	 * @param RGB input
	 * @param YUV output
	 */
	public static void rgb2yuv(double[] RGB, double[] YUV) {
		final double R = RGB[0];
		final double G = RGB[1];
		final double B = RGB[2];
		
		YUV[0] = +0.21260*R +0.71520*G +0.07220*B;
		YUV[1] = -0.09991*R -0.33609*G +0.43600*B;
		YUV[2] = +0.61500*R -0.55861*G -0.05639*B;
	}

	/**
	 * YUV (HDTV with BT.709) to linear RGB
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/YUV
	 * input and output can be the same array
	 * @param YUV input
	 * @param RGB output
	 */
	public static void yuv2rgb(double[] YUV, double[] RGB) {
		final double Y = YUV[0];
		final double U = YUV[1];
		final double V = YUV[2];
		
		RGB[0] = +Y +0.00000*U +1.28033*V;
		RGB[1] = +Y -0.21482*U -0.38059*V;
		RGB[2] = +Y +2.12798*U +0.00000*V;
	}

	/**
	 * linear RGB to sRGB, applying gamma correction
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/SRGB
	 * input and output can be the same array
	 * @param RGB input
	 * @param sRGB output
	 */
	public static void rgb2sRGB(double[] RGB, double[] sRGB) {
		sRGB[0] = gammaCorrection(RGB[0]);
		sRGB[1] = gammaCorrection(RGB[1]);
		sRGB[2] = gammaCorrection(RGB[2]);
	}
	
	/**
	 * sRGB to linear RGB, applying inverse gamma correction
	 * normalized input and output values
	 * https://en.wikipedia.org/wiki/SRGB
	 * input and output can be the same array
	 * @param sRGB input
	 * @param RGB output
	 */
	public static void sRGB2rgb(double[] sRGB, double[] RGB) {
		RGB[0] = inverseGammaCorrection(sRGB[0]);
		RGB[1] = inverseGammaCorrection(sRGB[1]);
		RGB[2] = inverseGammaCorrection(sRGB[2]);
	}

	private static double labF(double t) {
		final double delta = 6.0/29;
		final double delta2 = delta*delta;
		final double delta3 = delta2*delta;
		
		if (t > delta3) {
			return Math.pow(t, 1.0/3);
		} else {
			return 4.0/29 + t/3/delta2;
		}
		
	}
	
	private static double inverseLabF(double t) {
		final double delta = 6.0/29;
		
		if (t > delta) {
			return t*t*t;
		} else {
			return 3*delta*delta*(t - 4.0/29);
		}
	}
	
	/**
	 * linear XYZ to sRGB
	 * @param x
	 * @return
	 */
	private static double gammaCorrection(double x) {
		return (x <= 0.0031308) ? x*12.92 : (1.055*Math.pow(x, 1.0/2.4) - 0.055);
	}

	private static double inverseGammaCorrection(double x) {
		return (x <= 0.04045) ? x/12.92 : Math.pow((x + 0.055)/1.055, 2.4);
	}
	
	private static double distance2(double[] a, double[] b) {
		final double dx = a[0] - b[0];
		final double dy = a[1] - b[1];
		final double dz = a[2] - b[2];
		final double magentaCorrection = (ImprovedLabDistance && b[2] < -0.9 && b[1] > 0.792 && b[1] < 0.95) ? 5 : 1;
		
		return (dx*dx + dy*dy + dz*dz)*magentaCorrection;
	}
	
	private static double distance2(double a, double b) {
		return (a - b)*(a - b);
	}
	
}
