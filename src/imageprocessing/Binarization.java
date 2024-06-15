package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import main.Picsi;
import utils.Parallel;

/**
 * Image segmentation (binarization) using Otsu's method
 * Image foreground = black
 * Palette: background, foreground
 * @author Christoph Stamm
 *
 */
public class Binarization implements IImageProcessor {
	public static int s_background = 0; // white
	public static int s_foreground = 1; // black

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		final int threshold = otsuThreshold(inData);
		System.out.println(threshold);

		return binarize(inData, threshold, false, true);
	}

	/**
	 * Binarization of grayscale image
	 * @param inData grayscale image
	 * @param threshold
	 * @param smallValuesAreForeground true: Image foreground <= threshold, false: Image foreground > threshold
	 * @param binary true: output is binary image, false: output is grayscale image
	 * @return binarized image
	 */
	public static ImageData binarize(ImageData inData, int threshold, boolean smallValuesAreForeground, boolean binary) {
		assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;

		ImageData outData = ImageProcessing.createImage(inData.width, inData.height, (binary) ? Picsi.IMAGE_TYPE_BINARY : Picsi.IMAGE_TYPE_GRAY);
		final int fg = (smallValuesAreForeground) ? s_foreground : s_background;
		final int bg = (smallValuesAreForeground) ? s_background : s_foreground;

		Parallel.For(0, inData.height, v -> {
			for (int u=0; u < inData.width; u++) {
				outData.setPixel(u, v, (inData.getPixel(u,v) <= threshold) ? fg : bg);
			}
		});
		return outData;
	}

	/**
	 * Computes a global threshold for binarization using Otsu's method
	 * @param inData grayscale image
	 * @return threshold
	 */
	public static int otsuThreshold(ImageData inData) {
		assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;

		int classes = 1 << Math.min(8, inData.depth);
		int[] histogram = ImageProcessing.histogram(inData, classes);
		int total = inData.width * inData.height;

		int threshold = 0;
		double maxVariance = 0.0;

		for (int t = 0; t < classes; t++) {
			// Calculate weights and means
			int sumForeground = 0;
			int countForeground = 0;
			int sumBackground = 0;
			int countBackground = 0;

			for (int i = 0; i < 256; i++) {
				if (i <= t) {
					countForeground += histogram[i];
					sumForeground += i * histogram[i];
				} else {
					countBackground += histogram[i];
					sumBackground += i * histogram[i];
				}
			}

			double meanForeground = (countForeground == 0) ? 0 : (double) sumForeground / countForeground;
			double meanBackground = (countBackground == 0) ? 0 : (double) sumBackground / countBackground;

			// Calculate between-class variance
			double variance = countForeground * countBackground * Math.pow((meanForeground - meanBackground), 2) / Math.pow(total, 2);

			// Update threshold if variance is maximum
			if (variance > maxVariance) {
				maxVariance = variance;
				threshold = t;
			}
		}

		return threshold;
	}
}
