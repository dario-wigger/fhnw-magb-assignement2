package files;

import java.io.PrintWriter;
import java.io.IOException;

/**
 * Image creator interface
 * 
 * @author Christoph Stamm
 *
 */
public interface IImageCreator {

	/**
	 * Write image matrix of given size and image type into the provided print writer.
	 * Usage: Inside Picsi image editor write: 
	 * 	P2
	 * 	w h
	 * 	max
	 * 	@files.ClassName
	 * where ClassName is an instance of IImageCreator
	 * 	
	 * @param pw print writer for writing image matrix in ASCII mode
	 * @param imageType one of the image types define in Globl.IMAGE_TYPE_XXX
	 * @param width image width in pixels
	 * @param height image height in pixels
	 * @param maxValue maximum intensity value
	 */
	void create(PrintWriter pw, int imageType, int width, int height, int maxValue) throws IOException;
}
