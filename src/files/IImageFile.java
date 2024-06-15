package files;

import javax.swing.JTextArea;

import org.eclipse.swt.graphics.ImageData;

/**
 * Image file interface
 * 
 * @author Christoph Stamm
 *
 */
public interface IImageFile {
	/**
	 * Loads an image file from the file system
	 * @param fileName
	 * @return device independent image data
	 * @throws Exception
	 */
	public ImageData read(String fileName) throws Exception;
	
	/**
	 * Saves an image file to the file system
	 * @param fileName
	 * @param fileType
	 * @param imageData
	 * @param imageType
	 * @throws Exception
	 */
	public void save(String fileName, int fileType, ImageData imageData, int imageType) throws Exception;
	
	/**
	 * Converts the given image data from binary to ASCII and writes it to the given text area 
	 * @param imageData
	 * @param text
	 */
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text);
	
	/**
	 * Returns true if this image is in binary format
	 * @return
	 */
	public boolean isBinaryFormat();
}
