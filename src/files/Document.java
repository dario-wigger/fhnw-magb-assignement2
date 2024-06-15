package files;
import javax.swing.JTextArea;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import imageprocessing.ImageProcessing;

/**
 * Image document class
 * 
 * @author Christoph Stamm
 *
 */
public class Document {
	private String m_fileName;	// image file path
	private IImageFile m_file;	// image file
	private int m_fileType;		// image file type
	private ImageData m_image;	// image data
	private int m_imageType;	// image type
	
	public Document() {
		m_fileType = SWT.IMAGE_UNDEFINED;
	}
	
	/**
	 * Clear document.
	 */
	public void clear() {
		m_fileName = null;
		m_file = null;
		m_image = null;
		m_fileType = SWT.IMAGE_UNDEFINED;
		m_imageType = 0;
	}

	/**
	 * Loads an image file
	 * @param fileName
	 * @param fileType
	 * @throws Exception
	 */
	public void load(String fileName, int fileType) throws Exception {
		m_fileType = fileType;
		m_file = ImageFiles.createImageFile(m_fileType);
		
		if (m_file != null) {
			m_fileName = fileName;
			setImage(m_file.read(fileName));
		}
	}
	
	/**
	 * Saves an image file
	 * @param filename
	 * @param fileType
	 * @throws Exception
	 */
	public void save(String filename, int fileType) throws Exception {
		if (filename == null) {
			if (m_file != null && m_fileName != null && m_fileType >= 0) {
				// save with existing file name and file type
				filename = m_fileName;
				fileType = m_fileType;
			} else {
				assert filename != null : "filename is null";
				assert fileType >= 0 : "wrong fileType";

				m_file = ImageFiles.createImageFile(fileType);
			}
		} else {
			// save with new file name or new file type
			assert fileType >= 0 : "wrong fileType";
			if (m_file == null || fileType != m_fileType) {
				m_file = ImageFiles.createImageFile(fileType);
			}
		}
		if (m_file != null) {
			m_fileName = filename;
			m_file.save(filename, fileType, m_image, m_imageType);
		}
	}
	
	/**
	 * Returns image
	 * @return
	 */
	public ImageData getImage() {
		return m_image;
	}
	
	/**
	 * Returns the image type of this image
	 * @return
	 */
	public int getImageType() {
		return m_imageType;
	}
	
	/**
	 * Returns true if this image is in binary format
	 * @return
	 */
	public boolean isBinaryFormat() {
		assert m_file != null : "no image file available";
		return m_file.isBinaryFormat();		
	}
	
	/**
	 * Returns the name of this image file
	 * @return
	 */
	public String getFileName() {
		return m_fileName;
	}
	
	/**
	 * Returns the file type of this image file
	 * @return
	 */
	public int getFileType() {
		return m_fileType;
	}
	
	/**
	 * Returns true if a file exists
	 * @return
	 */
	public boolean hasFile() {
		return m_file != null;
	}
	
	/**
	 * Returns true if an image exists
	 * @return
	 */
	public boolean hasImage() {
		return m_image != null;
	}
	
	/**
	 * Converts the given image data from binary to ASCII and writes it to the given text area 
	 * @param image image data
	 * @param text text area
	 */
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text) {
		assert m_file != null : "no image file available";
		
		text.removeAll();
		m_file.displayTextOfBinaryImage(imageData, text);			
	}

	/**
	 * Set or reset file name
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		m_fileName = fileName;
		m_file = null;
	}

	/**
	 * Set or reset image
	 * @param imageData
	 */
	public void setImage(ImageData imageData) {
		m_image = imageData;
		m_imageType = ImageProcessing.determineImageType(imageData);
	}

}
