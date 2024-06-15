package imageprocessing.transformation;

import imageprocessing.IImageProcessor;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

public class AffineMapping implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {

        double rotation =  (45.0 / 180.0) * Math.PI;
        double scale = 2;
        int width = inData.width / 2;
        int height = inData.height / 2;

        return transform(inData, Matrix
            .translation(width, height) // damit Ursprung in den Mittelpunkt gelangt
            .multiply(Matrix.rotation(rotation))
            .multiply(Matrix.scaling(scale, scale))
            .multiply(Matrix.translation(-width, -height))); // damit der Ursprung wieder oben links ist
    }

    public static ImageData transform(ImageData inData, Matrix a) {

        Matrix inverse = a.inverse();
        int width = inData.width;
        int height = inData.height;
        ImageData imageData = new ImageData(width, height, inData.depth, inData.palette);

        Parallel.For(0, height, v -> {
            for(int u = 0; u < width; u ++) {
                final double[] t = inverse.multiply(new double[] { u, v, 1 });
                final int uOld = (int) t[0];
                final int vOld = (int) t[1];

                if (uOld >= 0 && vOld >= 0 && uOld < width && vOld < height) {
                    imageData.setPixel(u, v, inData.getPixel(uOld, vOld));
                } else {
                    imageData.setPixel(u, v, 0);
                }
            }
        });

        return imageData;
    }
}
