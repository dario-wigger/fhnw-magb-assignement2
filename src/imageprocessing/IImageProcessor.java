package imageprocessing;

import org.eclipse.swt.graphics.ImageData;

/**
 * Image processing interface
 * 
 * @author Christoph Stamm
 *
 */
public interface IImageProcessor {
	/**
	 * Enables/disables the menu entry
	 * @param imageType one of the image types define in Picsi.IMAGE_TYPE_XXX
	 * @return true if the menu entry should be enabled for the given image type
	 */
	public boolean isEnabled(int imageType);
	
	/**
	 * Runs the image processing routine
	 * @param inData input image data
	 * @param imageType one of the image types define in Picsi.IMAGE_TYPE_XXX
	 * @return output image or null if the image processing cannot produce a useful output
	 */
	public ImageData run(final ImageData inData, int imageType);
}
