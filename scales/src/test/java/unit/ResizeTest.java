package unit;

import org.junit.jupiter.api.Test;
import scales.utility.ImageResize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResizeTest {

    // scaling-relative tests/data

    private static String mPath =
            "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_92x30dp.png";
    private static String mExtension = mPath.substring(
            mPath.lastIndexOf('.') + 1
    );

    private static URL mURL;
    private static BufferedImage mImg;

    private static Integer[] mSmall = {100, 250, 400};
    private static Integer[] mMedium = {750, 947, 1000};
    private static Integer[] mLarge = {1440, 1920, 4832};

    // setting up image URL and image itself
    static {
        try {
            mURL = new URL(mPath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            mImg = ImageIO.read(mURL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void resizeSmall() {
        resizeGeneral(mSmall, mImg);
    }

    @Test
    void resizeMedium() {
        resizeGeneral(mMedium, mImg);
    }

    @Test
    void resizeLarge() {
        resizeGeneral(mLarge, mImg);
    }


    void resizeGeneral(Integer[] sizes, BufferedImage img) {
        for (Integer width : sizes) {
            // assert that width is ok after scaling
            assertEquals(ImageResize.resizeToWidth(img, width).getWidth(), width);
        }
    }


    // data-transformations-relative tests/data

    @Test
    void dataFormatsTransformationsIdempotence() {
        try {
            assertTrue(
                    compareBufferedImages(
                            mImg,
                            // idempotent operation
                            ImageResize.bytesToImage(
                                ImageResize.imageToBytes(mImg, mExtension)
                            )
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Boolean compareBufferedImages(BufferedImage fst, BufferedImage snd) {
        return (fst.getWidth() == snd.getWidth()) &&
                (fst.getHeight() == snd.getHeight()) &&
                (fst.getColorModel() == snd.getColorModel()) &&
                (fst.getType() == snd.getType());
    }
}
