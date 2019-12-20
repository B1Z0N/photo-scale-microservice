package profiles.utility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class ImageResize {

    public static BufferedImage resizeFromURL(String imgURL, Integer newWidth) throws IOException {
        String imgExt = imgURL.substring(imgURL.lastIndexOf('.') + 1);
        if (imgExt == "jpeg") imgExt = "jpg";

        BufferedImage image = imageFromURL(imgURL);
        return resizeToWidth(image, newWidth);
    }

    private static BufferedImage imageFromURL(String urlPath) throws IOException {
        URL url = new URL(urlPath);
        return ImageIO.read(url);
    }

    private static BufferedImage resizeToWidth(BufferedImage img, int width) {
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
}