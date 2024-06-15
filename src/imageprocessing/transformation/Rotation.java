package imageprocessing.transformation;

import gui.OptionPane;
import imageprocessing.IImageProcessor;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

public class Rotation implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        double rotation = OptionPane.showDoubleDialog("Rotation", 0.0);
        return rotate(inData, (rotation / 180) * Math.PI);
    }

    public static ImageData rotate(ImageData image, double rotation) {

        rotation = rotation - Math.PI; // flip the image 180°

        int oldWidth = image.width;
        int oldHeight = image.height;

        int newWidth  = (int) Math.round(Math.abs(oldWidth * Math.cos(rotation)) + Math.abs(oldHeight * Math.sin(rotation))); // why?
        int newHeight = (int) Math.round(Math.abs(oldWidth * Math.sin(rotation)) + Math.abs(oldHeight * Math.cos(rotation))); // why?

        ImageData out = new ImageData(newWidth, newHeight, image.depth, image.palette);

        int centerW = newWidth / 2;
        int centerH = newHeight / 2;

        double sin = Math.sin(rotation);
        double cos = Math.cos(rotation);

        Parallel.For(0, newHeight, v -> {
            for(int u = 0; u < newWidth; u ++) {
                /*
                 * oldWidth / 2 --> Mitte des Bildes. Davon ausgehend wird dann der Pixel platziert
                 */
                int uOld = oldWidth / 2 - (int) Math.floor(0.5 + cos * (u - centerW) + sin * (v - centerH));
                int vOld = oldHeight / 2 - (int) Math.floor(0.5 - sin * (u - centerW) + cos * (v - centerH));

                if(uOld >= 0 && vOld >= 0 && uOld < oldWidth && vOld < oldHeight) {
                    out.setPixel(u, v, image.getPixel(uOld, vOld));
                } else {
                    out.setPixel(u, v, 0);
                }
            }
        });

        return out;
    }
}
