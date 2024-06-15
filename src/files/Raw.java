package files;

import java.io.RandomAccessFile;

import javax.swing.JTextArea;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * AOS Raw image file implementation (read only)
 * 
 * @author Christoph Stamm
 *
 */
public class Raw implements IImageFile {
	@Override
	public ImageData read(String fileName) throws Exception {
		RandomAccessFile raf = new RandomAccessFile(fileName, "r");
		
		try {
			// read width and height in little-endian format and convert to big-endian Java format
			raf.seek(468);
			int width = little2bigEndian(raf.readInt());
			int stride = ((width + 3)/4)*4;
			int height = little2bigEndian(raf.readInt());
						
			// read raw data (total header size is 1024 bytes)
			byte[] raw = new byte[stride*height];
			raf.seek(1024);
			raf.read(raw);
			
			// create grayscale image with palette
			RGB[] grayscale = new RGB[256];
			for(int i = 0; i < grayscale.length; i++) grayscale[i] = new RGB(i, i, i);
			return new ImageData(width, height, 8, new PaletteData(grayscale), 4, raw);	// stride is a multiple of 4 bytes
		
		} finally {
			raf.close();			
		}
	}

	@Override
	public void save(String fileName, int fileType, ImageData imageData, int imageType) throws Exception {
		// not implemented
	}

	@Override
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text) {
		text.append("P2");
		text.append("\n" + imageData.width + " " + imageData.height);
		text.append("\n255\n");
		PNM.writePGM(imageData, text, 255);
	}

	@Override
	public boolean isBinaryFormat() {
		return true;
	}

	private int little2bigEndian(int i) {
		return (i << 24) | (i & 0xFF00) << 8 | (i & 0xFF0000) >>> 8 | (i >>> 24); 
	}
	
}
