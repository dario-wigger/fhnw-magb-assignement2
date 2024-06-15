package imageprocessing.binary;

import org.eclipse.swt.graphics.ImageData;

import utils.Parallel;

/**
 * Binary operations
 * Palette: background, foreground
 * @author Christoph Stamm
 *
 */
public class BinaryOperation {
	public static enum BinOp { AND, OR, XOR };

	/**
	 * Binary image operations
	 * @param inData1
	 * @param inData2
	 * @param op
	 * @return inData1 op inData2
	 */
	public static ImageData binaryOperation(ImageData inData1, ImageData inData2, BinOp op) {
		ImageData outData = (ImageData)inData1.clone();
		Parallel.For(0, outData.height, v -> {
			for (int u=0; u < outData.width; u++) {
				int in1 = inData1.getPixel(u, v);
				int in2 = inData2.getPixel(u, v);
				int out = 0;
				
				switch(op) {
				case AND: out = in1 & in2; break; 
				case OR:  out = in1 | in2; break; 
				case XOR: out = in1 ^ in2; break; 
				}
				outData.setPixel(u, v, out);
			}
		});
		return outData;
	}

}
