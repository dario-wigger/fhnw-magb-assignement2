package imageprocessing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import gui.OptionPane;
import main.Picsi;
import utils.Parallel;

/**
 * Morphologic filter and demo
 * @author Christoph Stamm
 *
 */
public class MorphologicFilter implements IImageProcessor {
	public static int s_background = 0; // white
	public static int s_foreground = 1; // black
	public static boolean[][] s_circle3 = new boolean[][] {{ false, true, false},{true, true, true},{false, true, false}};
	public static boolean[][] s_circle5 = new boolean[][] {
		{ false, true,  true, true,  false},
		{ true,  true,  true, true,  true},
		{ true,  true,  true, true,  true},
		{ true,  true,  true, true,  true},
		{ false, true,  true, true,  false}};
	public static boolean[][] s_circle7 = new boolean[][] {
		{ false, false, true, true, true, false, false},
		{ false, true,  true, true, true, true,  false},
		{ true,  true,  true, true, true, true,  true},
		{ true,  true,  true, true, true, true,  true},
		{ true,  true,  true, true, true, true,  true},
		{ false, true,  true, true, true, true,  false},
		{ false, false, true, true, true, false, false}};
	public static boolean[][] s_diamond5 = new boolean[][] {
		{ false, false, true, false, false},
		{ false, true , true, true , false},
		{ true,  true,  true, true,  true},
		{ false, true,  true, true,  false},
		{ false, false, true, false, false}};
	public static boolean[][] s_diamond7 = new boolean[][] {
		{ false, false, false, true, false, false, false},
		{ false, false, true,  true, true,  false, false},
		{ false, true,  true,  true, true,  true,  false},
		{ true,  true,  true,  true, true,  true,  true},
		{ false, true,  true,  true, true,  true,  false},
		{ false, false, true,  true, true,  false, false},
		{ false, false, false, true, false, false, false}};
	public static boolean[][] s_square2 = new boolean[][] {{ true, true},{true, true}};
	public static boolean[][] s_square3 = new boolean[][] {{ true, true, true},{true, true, true},{true, true, true}};
	public static boolean[][] s_square4 = new boolean[][] {{ true, true, true, true},{true, true, true, true},{true, true, true, true},{true, true, true, true}};
	public static boolean[][] s_square5 = new boolean[][] {{ true, true, true, true, true},{true, true, true, true, true},{true, true, true, true, true},{true, true, true, true, true},{true, true, true, true, true}};

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_BINARY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		Object[] operations = { "Erosion", "Dilation", "Opening", "Closing", "Inner Contour", "Outer Contour" };
		int ch = OptionPane.showOptionDialog("Morphological Operation", SWT.ICON_INFORMATION, operations, 0);
		if (ch < 0) return null;

		Object[] structure = { "None", "Dot", "Circle-3", "Circle-5", "Circle-7", "Diamond-5", "Diamond-7", "Square-2", "Square-3", "Square-4", "Square-5" };
		int s = OptionPane.showOptionDialog("Structure", SWT.ICON_INFORMATION, structure, 2);
		if (s < 0) return null;
		boolean[][] struct;
		int cx, cy;
		switch(s) {
		default:
		case 0: struct = new boolean[][] {{}}; cx = cy = 0; break;
		case 1: struct = new boolean[][] {{true}}; cx = cy = 0; break;
		case 2: struct = s_circle3; cx = cy = 1; break;
		case 3: struct = s_circle5; cx = cy = 2; break;
		case 4: struct = s_circle7; cx = cy = 3; break;
		case 5: struct = s_diamond5; cx = cy = 2; break;
		case 6: struct = s_diamond7; cx = cy = 3; break;
		case 7: struct = s_square2; cx = cy = 0; break;
		case 8: struct = s_square3; cx = cy = 1; break;
		case 9: struct = s_square4; cx = cy = 1; break;
		case 10: struct = s_square5; cx = cy = 2; break;
		}

		switch(ch) {
		case 0: return erosion(inData, struct, cx, cy);
		case 1: return dilation(inData, struct, cx, cy);
		case 2: return opening(inData, struct, cx, cy, 1);
		case 3: return closing(inData, struct, cx, cy, 1);
		case 4: return contour(inData, imageType, struct, cx, cy, true);
		case 5: return contour(inData, imageType, struct, cx, cy, false);
		}
		return null;
	}

	/**
	 * Erosion: if the structure element is empty, then the eroded image only contains foreground pixels
	 * @param inData binary image or binarized grayscale image
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @return new eroded binary image
	 */
	public static ImageData erosion(ImageData inData, boolean[][] struct, int cx, int cy) {
		assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_BINARY || ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;

		ImageData outData = (ImageData)inData.clone();

		Parallel.For(0, outData.height, v -> {
			for (int u=0; u < outData.width; u++) {
				boolean set = true;

				for (int j=0; set && j < struct.length; j++) {
					final int v0 = v + j - cy;

					for (int i=0; set && i < struct[j].length; i++) {
						final int u0 = u + i - cx;

						if (struct[j][i] && (v0 < 0 || v0 >= inData.height || u0 < 0 || u0 >= inData.width || inData.getPixel(u0, v0) != s_foreground)) {
							set = false;
						}
					}
				}
				if (set) outData.setPixel(u, v, s_foreground); // foreground
				else outData.setPixel(u, v, s_background); // background
			}
		});
		return outData;
	}

	/**
	 * Dilation: if the structure element is empty, then the dilated image is empty, too
	 * @param inData binary image or binarized grayscale image
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @return new dilated binary image
	 */
	public static ImageData dilation(ImageData inData, boolean[][] struct, int cx, int cy) {
		assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_BINARY || ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;

		ImageData outData = new ImageData(inData.width, inData.height, inData.depth, inData.palette); // outData is initialized with 0

		Parallel.For(0, outData.height, v -> {
			for (int u=0; u < outData.width; u++) {
				boolean set = false;

				for (int j=0; !set && j < struct.length; j++) {
					final int v0 = v + j - cy;

					for (int i=0; !set && i < struct[j].length; i++) {
						final int u0 = u + i - cx;

						if (struct[j][i] && v0 >= 0 && v0 < inData.height && u0 >= 0 && u0 < inData.width && inData.getPixel(u0, v0) == s_foreground) {
							set = true;
						}
					}
				}
				if (set) outData.setPixel(u, v, s_foreground); // foreground
				else outData.setPixel(u, v, s_background); // background
			}
		});

		return outData;
	}

	/**
	 * Opening
	 * @param inData not an indexed-color image
	 * @param imageType
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @param multiplicity
	 * @return new opened binary image
	 */
	public static ImageData opening(ImageData inData, boolean[][] struct, int cx, int cy, int multiplicity) {

		ImageData outData = (ImageData)inData.clone();

		for(int i = 0; i < multiplicity; i++) {
			outData = erosion(outData, struct, cx, cy);
		}

		for(int i = 0; i < multiplicity; i++) {
			outData = dilation(outData, struct, cx, cy);
		}
		return outData;
	}

	/**
	 * Closing
	 * @param inData not an indexed-color image
	 * @param imageType
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @param multiplicity
	 * @return new closed binary image
	 */
	public static ImageData closing(ImageData inData, boolean[][] struct, int cx, int cy, int multiplicity) {

		ImageData outData = (ImageData)inData.clone();

		for(int i = 0; i < multiplicity; i++) {
			outData = dilation(outData, struct, cx, cy);
		}

		for(int i = 0; i < multiplicity; i++) {
			outData = erosion(outData, struct, cx, cy);
		}
		return outData;
	}

	/**
	 * Contour
	 * @param inData not an indexed-color image
	 * @param imageType
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @param inner: true = inner contour, false = outer contour
	 * @return new closed binary image
	 */
	public static ImageData contour(ImageData inData, int imageType, boolean[][] struct, int cx, int cy, boolean inner) {
		if (inner) {
			// TODO
		} else {
			// TODO
		}
		return null;
	}

}
