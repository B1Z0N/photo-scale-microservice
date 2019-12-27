package scales.utility;

import io.vertx.core.buffer.Buffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


// class for image resizing from different sources
public class ImageResize {

    public static Buffer bufferResizeToWidth(Buffer buff, int width, String extension) throws IOException {
        return Buffer.buffer(
                imageToBytes(
                        resizeToWidth(
                                bytesToImage(buff.getBytes()),
                                width
                        ), extension
                ));
    }

    public static BufferedImage resizeToWidth(BufferedImage img, int width) {
        int height = (int) ((1.0 * img.getHeight()) / img.getWidth() * width);
        return resize(img, height, width);
    }

    private static BufferedImage resize(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    public static BufferedImage bytesToImage(byte[] bts) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bts)) {
            return ImageIO.read(bais);
        }
    }

    public static byte[] imageToBytes(BufferedImage img, String ext) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, ext, baos);
            baos.flush();
            return baos.toByteArray();
        }
    }
}
