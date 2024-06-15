package files;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import main.Picsi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;

import javax.swing.JTextArea;

/**
 * Portable Pixmap Format
 * 
 * @author Christoph Stamm
 *
 */
public class PNM implements IImageFile {
	private int m_width, m_height;
	private int m_maxValue;		// max intensity per pixel
	private int m_imageType;
	private boolean m_ascii; 	// image matrix in ASCII or binary format

	/**
	 * Reads PNM file and returns the resulting image.
	 * The file format might be ASCII or binary.
	 */
	public ImageData read(String fileName) throws Exception {
		RandomAccessFile raf = new RandomAccessFile(fileName, "r");
		
		// read header
		readHeader(raf);
		
		// create image and read in image data
		if (m_ascii) {
			BufferedReader in = new BufferedReader(new FileReader(raf.getFD()));
			
			switch(m_imageType) {
			case Picsi.IMAGE_TYPE_BINARY: return readPBM(in);
			case Picsi.IMAGE_TYPE_GRAY: return readPGM(in);
			case Picsi.IMAGE_TYPE_RGB: return readPPM(in);
			default:
				in.close();
				throw new Exception("Read PNM: Wrong image type");
			}
		} else {
			switch(m_imageType) {
			case Picsi.IMAGE_TYPE_BINARY: return readBinPBM(raf);
			case Picsi.IMAGE_TYPE_GRAY: return readBinPGM(raf);
			case Picsi.IMAGE_TYPE_RGB: return readBinPPM(raf);
			default:
				raf.close();
				throw new Exception("Read PNM: Wrong image type");
			}
		}
	}

	/**
	 * Saves PNM file in binary format.
	 */
	public void save(String fileName, int fileType, ImageData imageData, int imageType) throws Exception {
		RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
		
		m_imageType = imageType;
		m_width = imageData.width;
		m_height = imageData.height;
		if (m_maxValue == 0) m_maxValue = 255;
		
		m_ascii = false;
		
		// write header
		switch(m_imageType) {
		case Picsi.IMAGE_TYPE_BINARY:
			raf.writeBytes("P4");
			break;
		case Picsi.IMAGE_TYPE_GRAY:
			raf.writeBytes("P5");
			break;
		case Picsi.IMAGE_TYPE_RGB:
			raf.writeBytes("P6");
			break;
		}
		raf.writeBytes("\n" + m_width + " " + m_height);
		if (m_imageType != Picsi.IMAGE_TYPE_BINARY) {
			raf.writeBytes("\n" + m_maxValue);
		}
		raf.writeBytes("\n");		
		
		// save image in binary format
		switch(m_imageType) {
		case Picsi.IMAGE_TYPE_BINARY: writeBinPBM(raf, imageData); break;
		case Picsi.IMAGE_TYPE_GRAY: writeBinPGM(raf, imageData); break;
		case Picsi.IMAGE_TYPE_RGB: writeBinPPM(raf, imageData); break;
		default:
			raf.close();
			throw new Exception("Write PNM: Wrong image type");
		}
	}
	
	/**
	 * Checks PNM header structure
	 * @param fileName
	 * @return 0: if the format is valid and ASCII; 1: if the header is valid and ASCII and contains creator-tag; -1 wrong header; -2 unknown ImageCreator
	 */
	public int checkHeader(String fileName) {		
		try {
			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			String s;
			int retValue = 0;
			
			// read header
			readHeader(raf);
			
			// check for creator tag: @package.CreatorClass
			// the CreatorClass has to implement the interface IImageCreator
			long pos = raf.getFilePointer(); 
			do {
				s = raf.readLine(); 
				s = s.trim();
			} while(s.isEmpty());
			if (s.charAt(0) == '@') {
				String creatorName = s.substring(1);
				Class<?> cls = Class.forName(creatorName);
				Constructor<?> ct = cls.getConstructor();
				Object o = ct.newInstance();
				if (o instanceof IImageCreator) {
					IImageCreator creator = (IImageCreator)o;
					// truncate file after header
					raf.seek(pos);
					raf.getChannel().truncate(pos);
					// create new image matrix
					creator.create(new PrintWriter(new FileWriter(raf.getFD()), true), m_imageType, m_width, m_height, m_maxValue);
					retValue = 1;
				} else {
					retValue = -2;
				}
			}
			
			raf.close();
			
			return (m_ascii) ? retValue : -1;
		} catch(ClassNotFoundException ex) {
			return -2;
		} catch(Exception ex) {
			return -1;
		}
	}
	
	/**
	 * Displays image content in PNM ASCII format
	 */
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text) {
		// write header
		switch(m_imageType) {
		case Picsi.IMAGE_TYPE_BINARY:
			text.append("P1");
			break;
		case Picsi.IMAGE_TYPE_GRAY:
			text.append("P2");
			break;
		case Picsi.IMAGE_TYPE_RGB:
			text.append("P3");
			break;
		}
		text.append("\n" + m_width + " " + m_height);
		if (m_imageType != ImageFiles.IMAGE_PBM) {
			text.append("\n" + m_maxValue);
		}
		text.append("\n");
		
		// write image
		switch(m_imageType) {
		case Picsi.IMAGE_TYPE_BINARY:
			writePBM(imageData, text);
			break;
		case Picsi.IMAGE_TYPE_GRAY:
			writePGM(imageData, text, m_maxValue);
			break;
		case Picsi.IMAGE_TYPE_RGB:
			writePPM(imageData, text, m_maxValue);
			break;
		}
	}

	public boolean isBinaryFormat() {
		return !m_ascii;
	}

	/**
	 * Writes PBM image in ASCII format to text
	 * White = 0, Black = 1
	 * @param image
	 * @param text
	 */
	public static void writePBM(ImageData imageData, JTextArea text) {
		StringBuilder sb = new StringBuilder(imageData.width*2);

		for (int v = 0; v < imageData.height; v++) {
 			for(int u = 0; u < imageData.width; u++) {
				sb.append(imageData.getPixel(u, v)).append(' ');
 				
 			}
			text.append(sb.toString());
			text.append("\n");
			sb.delete(0, sb.length());
		}	
	}

	/**
	 * Writes PGM image in ASCII format to text and scales intensities to maxValue
	 * @param image
	 * @param text
	 * @param maxValue
	 */
	public static void writePGM(ImageData imageData, JTextArea text, int maxValue) {
		assert imageData.depth == 8 : "wrong channel depth";
		final int padding = imageData.bytesPerLine - imageData.width;
		byte[] data = imageData.data;
		int pos = 0;
		StringBuilder sb = new StringBuilder(imageData.width*3);
		
		for (int y = 0; y < imageData.height; y++) {
			for (int x = 0; x < imageData.width; x++) {
				int v = data[pos++]*maxValue/255;
				if (v < 0) v += 256;
				sb.append(v).append(' ');
			}
			pos += padding;
			text.append(sb.toString());
			text.append("\n");
			sb.delete(0, sb.length());
		}	
	}

	/**
	 * Writes PPM image in ASCII format to text and scales intensities to maxValue
	 * @param image
	 * @param text
	 * @param maxValue
	 */
	public static void writePPM(ImageData imageData, JTextArea text, int maxValue) {
		assert imageData.palette.isDirect : "indexed color is not supported";
		StringBuilder sb = new StringBuilder(imageData.width*9);
		
		for (int y = 0; y < imageData.height; y++) {
			for (int x = 0; x < imageData.width; x++) {
				RGB rgb = imageData.palette.getRGB(imageData.getPixel(x, y));
				int v = rgb.red*maxValue/255;
				if (v < 0) v += 256;
				sb.append(v).append(' ');
				v = rgb.green*maxValue/255;
				if (v < 0) v += 256;
				sb.append(v).append(' ');
				v = rgb.blue*maxValue/255;
				if (v < 0) v += 256;
				sb.append(v).append(' ');
			}
			text.append(sb.toString());
			text.append("\n");
			sb.delete(0, sb.length());
		}	
	}

	private void readHeader(RandomAccessFile in) throws Exception {
		String s;
		boolean useMaxValue = true;
		
		// read magic number
		do {
			s = in.readLine(); 
			if (s == null) {
				in.close();
				throw new Exception("Wrong header"); 
			}
			s = s.trim();
		} while(s.isEmpty() || s.charAt(0) == '#');
		
		String[] ss = s.split("\\s+");	// regular expression: \s = whitespace, x+ = at least one
		if (ss[0].equals("P1")) {
			m_ascii = true;
			m_imageType = Picsi.IMAGE_TYPE_BINARY;
			useMaxValue = false;
		} else if (ss[0].equals("P2")) {
			m_ascii = true;
			m_imageType = Picsi.IMAGE_TYPE_GRAY;
		} else if (ss[0].equals("P3")) {
			m_ascii = true;
			m_imageType = Picsi.IMAGE_TYPE_RGB;
		} else if (ss[0].equals("P4")) {
			m_ascii = false;
			m_imageType = Picsi.IMAGE_TYPE_BINARY;
			useMaxValue = false;
		} else if (ss[0].equals("P5")) {
			m_ascii = false;
			m_imageType = Picsi.IMAGE_TYPE_GRAY;
		} else if (ss[0].equals("P6")) {
			m_ascii = false;
			m_imageType = Picsi.IMAGE_TYPE_RGB;
		} else {
			in.close();
			throw new Exception("Wrong PNM type");
		}
		
		// read width and height
		do {
			s = in.readLine(); 
			s = s.trim();
		} while(s.isEmpty() || s.charAt(0) == '#');

		ss = s.split("\\s+");	// regular expression: \s = whitespace, x+ = at least one
		
		if (ss.length == 2) {
			m_width = Integer.parseInt(ss[0]);
			m_height = Integer.parseInt(ss[1]);
			
		} else {
			in.close();
			throw new Exception("Wrong header: image size expected");
		}
		//System.out.println(m_width);
		//System.out.println(m_height);
		
		if (useMaxValue) {
			// read max value
			do {
				s = in.readLine(); 
				s = s.trim();
			} while(s.isEmpty() || s.charAt(0) == '#');

			m_maxValue = Integer.parseInt(s);
			//System.out.println(m_maxValue);				
		}
	}
	
	/**
	 * Read binary image using the given buffered reader
	 * White = 0, Black = 1
	 * @param in
	 * @param display
	 * @return
	 * @throws IOException
	 */
	private ImageData readPBM(BufferedReader in) throws IOException {
		try {
			int stride = (m_width + 7)/8;
			byte[] data = new byte[stride*m_height];
			
			// read data
			int pos = 0, val = 0, cnt = 0, x = 0;
			String s;
			
			// read next line
			do {
				s = in.readLine(); 
			} while(s != null && s.isEmpty());
			
			while(s != null && pos < data.length) {
				String[] ss = s.trim().split("\\s+");
				for(int i=0; i < ss.length; i++) {
					val = (val << 1) + Integer.parseInt(ss[i]);
					x++;
					cnt++;
					if (cnt == 8 || x == m_width) {
						data[pos++] = (byte)(val << (8 - cnt));
						val = 0;
						cnt = 0;
						if (x == m_width) x = 0;
					}
				}
				// read next line
				do {
					s = in.readLine(); 
				} while(s != null && s.isEmpty());
			}
			
			// create image
			return new ImageData(m_width, m_height, 1, new PaletteData(new RGB[]{ new RGB(255, 255, 255), new RGB(0, 0, 0) }), 1, data);

		} finally {
			in.close();			
		}
	}
	
	private ImageData readBinPBM(RandomAccessFile in) throws IOException {
		try {
			// read data
			final int stride = (m_width + 7)/8;
			byte[] data = new byte[stride*m_height];
			int pos = 0;
			byte[] line = new byte[stride];
	
			for (int y = 0; y < m_height; y++) {
				in.read(line);
				for (int x = 0; x < stride; x++) {
					data[pos++] = (byte)(~line[x]);
				}
			}			
	
			// create image
			return new ImageData(m_width, m_height, 1, new PaletteData(new RGB[]{ new RGB(255, 255, 255), new RGB(0, 0, 0) }), 1, data);
		
		} finally {
			in.close();
		}

	}
	
	private void writeBinPBM(RandomAccessFile out, ImageData imageData) throws IOException {
		try {
			// write data
			final int stride = (m_width + 7)/8;
			int pos = 0;
			byte[] line = new byte[stride];
	
			for (int y = 0; y < m_height; y++) {
				for (int x = 0; x < stride; x++) {
					line[x] = (byte)(~imageData.data[pos++]);
				}
				out.write(line);
			}	
			
		} finally {
			out.close();
		}
	}
	
	private ImageData readPGM(BufferedReader in) throws IOException {
		try {
			byte[] data = new byte[m_width*m_height];
			
			// read data
			int pos = 0;
			String s;
			
			// read next line
			do {
				s = in.readLine(); 
			} while(s != null && s.isEmpty());
			
			while(s != null && pos < data.length) {
				String[] ss = s.trim().split("\\s+");
				for(int i=0; i < ss.length; i++) {
					int val = Integer.parseInt(ss[i])*255/m_maxValue;
					data[pos++] = (byte)(val);
				}
				// read next line
				do {
					s = in.readLine(); 
				} while(s != null && s.isEmpty());
			}
		
			// create image
			RGB[] grayscale = new RGB[256];
			for(int i = 0; i < grayscale.length; i++) grayscale[i] = new RGB(i, i, i);
			return new ImageData(m_width, m_height, 8, new PaletteData(grayscale), 1, data);
			
		} finally {
			in.close();
		}
	}

	private ImageData readBinPGM(RandomAccessFile in) throws IOException {
		try {
			// read data
			int pos = 0;
	
			byte[] line = new byte[m_width];
			byte[] data = new byte[m_width*m_height];
	
			for (int y = 0; y < m_height; y++) {
				in.read(line);
				for (int x = 0; x < m_width; x++) {
					data[pos++] = (byte)(line[x]*255/m_maxValue);
				}
			}	
			
			// create image
			RGB[] grayscale = new RGB[256];
			for(int i = 0; i < grayscale.length; i++) grayscale[i] = new RGB(i, i, i);
			return new ImageData(m_width, m_height, 8, new PaletteData(grayscale), 1, data);

		} finally {
			in.close();
		}
	}
	
	private void writeBinPGM(RandomAccessFile out, ImageData imageData) throws IOException {
		try {
			// write data
			final int padding = imageData.bytesPerLine - m_width;
			int pos = 0;
			byte[] line = new byte[m_width];
	
			for (int y = 0; y < m_height; y++) {
				for (int x = 0; x < m_width; x++) {
					line[x] = (byte)(imageData.data[pos++]*m_maxValue/255);
				}
				pos += padding;
				out.write(line);
			}	
			
		} finally {
			out.close();
		}
	}
	
	private ImageData readPPM(BufferedReader in) throws IOException {
		try {
			final int bypp = 3;
			byte[] data = new byte[m_width*m_height*bypp];
			
			// read data
			int pos = 0;
			String s;
			
			// read next line
			do {
				s = in.readLine(); 
			} while(s != null && s.isEmpty());
			
			while(s != null && pos < data.length) {
				String[] ss = s.trim().split("\\s+");
				for(int i=0; i < ss.length; i++) {
					data[pos++] = (byte)(Integer.parseInt(ss[i])*255/m_maxValue);
				}
				// read next line
				do {
					s = in.readLine(); 
				} while(s != null && s.isEmpty());
			}
					
			// create image
			return new ImageData(m_width, m_height, bypp*8, new PaletteData(0xFF0000, 0xFF00, 0xFF), 1, data); // LSB is R
			
		} finally {
			in.close();
		}
	}

	private ImageData readBinPPM(RandomAccessFile in) throws IOException {
		try {
			final int bypp = 3;
			byte[] data = new byte[m_width*m_height*bypp];
	
			// read data
			int pos = 0;
			final int nBytes = bypp*m_width;
			byte[] line = new byte[nBytes];
	
			for (int y = 0; y < m_height; y++) {
				in.read(line);
				for (int x = 0; x < nBytes; x++) {
					data[pos++] = (byte)(line[x]*255/m_maxValue);
				}
			}			
			
			// create image
			return new ImageData(m_width, m_height, bypp*8, new PaletteData(0xFF0000, 0xFF00, 0xFF), 1, data); // LSB is R
		
		} finally {
			in.close();			
		}
	}
	
	private void writeBinPPM(RandomAccessFile out, ImageData imageData) throws IOException {
		try {
			final int bypp = 3;
			
			// write data
			final int nBytes = bypp*m_width;
			byte[] line = new byte[nBytes];
	
			for (int y = 0; y < m_height; y++) {
				int pos = 0;
				for (int x = 0; x < m_width; x++) {
					RGB rgb = imageData.palette.getRGB(imageData.getPixel(x, y));
					line[pos++] = (byte)(rgb.red*m_maxValue/255);
					line[pos++] = (byte)(rgb.green*m_maxValue/255);
					line[pos++] = (byte)(rgb.blue*m_maxValue/255);
				}
				out.write(line);
			}	
			
		} finally {
			out.close();
		}
	}
	
	/**
	 * Returns PNM image type depending on header information
	 * @param header
	 * @return
	 */
	public static int imageType(String header) {
		if (header.length() >= 2 && header.charAt(0) == 'P') {
			switch(header.charAt(1)) {
			case '1':
			case '4':
				return Picsi.IMAGE_TYPE_BINARY;
			case '2':
			case '5':
				return Picsi.IMAGE_TYPE_GRAY;
			case '3':
			case '6':
				return Picsi.IMAGE_TYPE_RGB;
			}
		}
		return 0; // unknown image type
	}
	
	/**
	 * Returns PNM file type depending on image type
	 * @param imageType
	 * @return
	 */
	public static int fileType(int imageType) {
		switch(imageType) {
		case Picsi.IMAGE_TYPE_BINARY: return ImageFiles.IMAGE_PBM;
		case Picsi.IMAGE_TYPE_GRAY: return ImageFiles.IMAGE_PGM;
		case Picsi.IMAGE_TYPE_RGB: return ImageFiles.IMAGE_PPM;
		}
		return SWT.IMAGE_UNDEFINED;
	}
}
