package imageprocessing.colors;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

public class GrayscaleImage implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_RGBA || imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        return grayscale(inData);
    }

    public static ImageData grayscale(ImageData image) {
        ImageData outData = ImageProcessing.createImage(image.width, image.height, Picsi.IMAGE_TYPE_GRAY);

        int wR = 3, wG = 6, wB = 1;

        Parallel.For(0, image.height, v -> {
            for (int u=0; u < image.width; u++) {
                RGB rgb = image.palette.getRGB(image.getPixel(u,v));
                int value = (wR * rgb.red + wG * rgb.green + wB * rgb.blue) / 10;
                outData.setPixel(u, v, value);
            }
        });

        return outData;
    }
}
