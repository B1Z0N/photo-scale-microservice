package profiles.utility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageResize {

    public static File resizeFromFile(File imgFile, String imgExt, Integer newWidth) throws IOException {
        imgExt = imgExt.toLowerCase();
        if (imgExt == "jpeg") imgExt = "jpg";

        BufferedImage img = ImageIO.read(imgFile);
        img = resizeToWidth(img, newWidth);

        File outputFile = File.createTempFile("aws", "s3");
        ImageIO.write(img, imgExt, outputFile);

        return outputFile;
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