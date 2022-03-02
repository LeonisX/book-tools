package md.leonis.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.RenderingHints.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class BorderFix {

    private static final Profile profile = new Profile()
            // final page dimensions
            .withWidth(2916)
            .withHeight(4630)
            // Indents on all sides, for which black spots are not taken into account (may be massive). 25 for 600 DPI
            .withDx(30)
            .withDy(30)
            // colors
            .withBlackPoint(80)
            .withWhitePoint(250)
            // increase padding
            .withPadding(5);

/*    private static final Profile profile = new Profile()
            // final page dimensions
            .withWidth(16 * multiplier)
            .withHeight(24 * multiplier)
            // Indents on all sides, for which black spots are not taken into account (may be massive). 25 for 600 DPI
            .withDx(1 * multiplier)
            .withDy(1 * multiplier)
            // colors
            .withBlackPoint(120)
            .withWhitePoint(250)
            // increase padding
            .withPadding(0);*/


    private static Path path = Paths.get("C:\\Documents and Settings\\user\\Documents\\result\\result\\");
    private static Path outPath = path.resolve("out");

    private static final boolean debug = true;

    public static void main(String[] args) throws IOException {

        /*int multiplier = 5;
        String fileName = "corrupted-border-" + multiplier + ".bmp";
        processImage(readImage(fileName), fileName);*/

        if (debug) {
            path = Paths.get("C:\\Documents and Settings\\user\\Documents\\result\\result\\");
            outPath = path.resolve("out");
            Files.createDirectories(outPath);
            String fileName = "page_005.bmp";
            processImage(readImage(fileName), fileName);
            /*fileName = "page_191.bmp";
            processImage(readImage(fileName), fileName);
            fileName = "page_192.bmp";
            processImage(readImage(fileName), fileName);*/

        } else {

            Files.createDirectories(outPath);

            List<String> files = Files.list(path).filter(f -> !Files.isDirectory(f)).map(f -> f.getFileName().toString()).collect(Collectors.toList());
            for (String file : files) {
                System.out.print(file);
                processImage(readImage(file), file);
            }
        }
    }

    private static void processImage(BufferedImage image, String file) throws IOException {

        image = fixRotation(image);
        if (debug) {
            saveImage(image, file, "rotated");
        }
        image = fixBlack(image);
        if (debug) {
            saveImage(image, file, "clean");
        }

        Padding padding = getPadding(image);
        padding.appendSize(profile.getWidth() - image.getWidth(), profile.getHeight() - image.getHeight());

        BufferedImage resizedImage = resizeImage(image, padding);
        if (debug) {
            saveImage(resizedImage, file, "resized");
        }

        padding.appendBy(profile.getPadding());
        BufferedImage finalImage = finalizeImage(resizedImage, padding);
        saveImage(finalImage, file, "final");

        System.out.println(String.format(": %s x %s", finalImage.getWidth(), finalImage.getHeight()));
    }

    private static BufferedImage resizeImage(BufferedImage image, Padding padding) {
        BufferedImage copy = new BufferedImage(profile.getWidth(), profile.getHeight(), TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setPaint(new Color(255, 160, 255));
        graphics.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        // page
        graphics.drawImage(image, padding.getLeft(), padding.getTop(), null);
        graphics.dispose();
        return copy;
    }

    private static BufferedImage finalizeImage(BufferedImage image, Padding padding) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);

        // left
        if (padding.getLeft() > 0) {
            int left = padding.getLeft();
            for (int x = 0; x < padding.getLeft(); x++) {
                if (isTextRow(image, x + padding.getLeft())) {
                    left = x - 2;
                    break;
                }
            }
            graphics.drawImage(image, 0, padding.getTop(), padding.getLeft() + left, image.getHeight() - padding.getBottom(),
                    padding.getLeft(), padding.getTop(), padding.getLeft() + left, image.getHeight() - padding.getBottom(), null);
        }
        // right
        if (padding.getRight() > 0) {
            int right = padding.getRight();
            for (int x = 0; x < padding.getRight(); x++) {
                if (isTextRow(image, image.getWidth() - padding.getRight() - x)) {
                    right = x - 2;
                    break;
                }
            }
            graphics.drawImage(image, image.getWidth() - padding.getRight() - right, padding.getTop(), image.getWidth(), image.getHeight() - padding.getBottom(),
                    image.getWidth() - padding.getRight() - right, padding.getTop(), image.getWidth() - padding.getRight(), image.getHeight() - padding.getBottom(), null);
        }
        // top
        if (padding.getTop() > 0) {
            int top = padding.getTop();
            for (int y = 0; y < padding.getTop(); y++) {
                if (isTextLine(image, y + padding.getTop())) {
                    top = y - 1;
                    break;
                }
            }
            graphics.drawImage(image, 0, 0, image.getWidth(), padding.getTop() + top,
                    0, padding.getTop(), image.getWidth(), padding.getTop() + top, null);
        }
        // bottom
        if (padding.getBottom() > 0) {
            int bottom = padding.getBottom();
            for (int y = 0; y < padding.getBottom(); y++) {
                if (isTextLine(image, image.getHeight() - padding.getBottom() - y)) {
                    bottom = y - 1;
                    break;
                }
            }

            BufferedImage copy2 = new BufferedImage(image.getWidth(), padding.getBottom() + bottom, TYPE_INT_RGB);
            Graphics2D graphics2 = copy2.createGraphics();
            graphics2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            graphics2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
            graphics2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
            //graphics2.setPaint(new Color(12, 128, 255));
            //graphics2.fillRect(0, 0, copy.getWidth(), copy.getHeight());
            graphics2.drawImage(image, 0, 0, copy2.getWidth(), copy2.getHeight(),
                    0, image.getHeight() - padding.getBottom() - bottom, image.getWidth(), image.getHeight() - padding.getBottom(), null);
            graphics.drawImage(copy2, 0, image.getHeight() - padding.getBottom() - bottom, null);
        }
        graphics.dispose();
        return image;
    }

    private static BufferedImage readImage(String file) throws IOException {
        return ImageIO.read(path.resolve(file).toFile());
    }

    private static void saveImage(BufferedImage image, String file, String prefix) throws IOException {
        ImageIO.write(image, "BMP", outPath.resolve(file.replace(".", "-" + prefix + ".")).toFile());
    }

    private static BufferedImage fixRotation(BufferedImage image) {

        int x = getWhitePointX(image);
        int y = getWhitePointY(image);

        if (x == 0 || y == 0) {
            // w/o rotation - do nothing
            return image;
        } else {

            if (x < y) { // rotate right
                y = (int) (image.getWidth() * 1.0 / image.getHeight() * x);
            } else {     //rotate left
                x = (int) (image.getHeight() * 1.0 / image.getWidth() * y);
            }

            x++;
            y++;

            return image.getSubimage(x, y, image.getWidth() - x * 2, image.getHeight() - y * 2);
        }
    }

    private static Padding getPadding(BufferedImage image) {

        Padding padding = new Padding();

        for (int yi = profile.getDy(); yi < image.getHeight() - profile.getDy(); yi++) {
            int x = getBlackPointXLeft(image, yi);
            padding.setLeft(Math.max(padding.getLeft(), x));
        }
        for (int yi = profile.getDy(); yi < image.getHeight() - profile.getDy(); yi++) {
            int x = image.getWidth() - getBlackPointXRight(image, yi) - 1;
            padding.setRight(Math.max(padding.getRight(), x));
        }

        for (int xi = profile.getDx(); xi < image.getWidth() - profile.getDx(); xi++) {
            int y = getBlackPointYTop(image, xi);
            padding.setTop(Math.max(padding.getTop(), y));
        }
        for (int xi = profile.getDx(); xi < image.getWidth() - profile.getDx(); xi++) {
            int y = image.getHeight() - getBlackPointYBottom(image, xi) - 1;
            padding.setBottom(Math.max(padding.getBottom(), y));
        }

        return padding;
    }

    private static BufferedImage fixBlack(BufferedImage image) {
        Padding padding = getPadding(image);
        padding.appendBy(profile.getPadding());
        //padding.appendSize(profile.getWidth() - image.getWidth(), profile.getHeight() - image.getHeight());
        return image.getSubimage(padding.getLeft(), padding.getTop(), image.getWidth() - padding.getLeft() - padding.getRight(),
                image.getHeight() - padding.getTop() - padding.getBottom());
    }

    private static boolean isTextLine(BufferedImage image, int y) {

        for (int x = 0; x < image.getWidth(); x++) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) < profile.getBlackPoint()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTextRow(BufferedImage image, int x) {

        for (int y = 0; y < image.getHeight(); y++) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) < profile.getBlackPoint()) {
                return true;
            }
        }
        return false;
    }

    private static int getBlackPointXLeft(BufferedImage image, int y) {

        for (int x = 0; x < image.getWidth(); x++) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) > profile.getBlackPoint()) {
                return x;
            }
        }
        return image.getWidth();
    }

    private static int getBlackPointXRight(BufferedImage image, int y) {

        for (int x = image.getWidth() - 1; x >= 0; x--) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) > profile.getBlackPoint()) {
                return x;
            }
        }
        return image.getWidth();
    }

    private static int getBlackPointYTop(BufferedImage image, int x) {

        for (int y = 0; y < image.getHeight(); y++) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) > profile.getBlackPoint()) {
                return y;
            }
        }
        return image.getHeight();
    }

    private static int getBlackPointYBottom(BufferedImage image, int x) {

        for (int y = image.getHeight() - 1; y >= 0; y--) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) > profile.getBlackPoint()) {
                return y;
            }
        }
        return image.getHeight();
    }

    private static int getWhitePointX(BufferedImage image) {

        for (int y = 0; y < image.getWidth(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int[] pixel = getPixelRGB(image, x, y);
                if (((pixel[0] + pixel[1] + pixel[2]) / 3) < profile.getWhitePoint()) {
                    return x;
                }
            }
        }
        return image.getWidth() - 1;
    }

    private static int getWhitePointY(BufferedImage image) {

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int[] pixel = getPixelRGB(image, x, y);
                if (((pixel[0] + pixel[1] + pixel[2]) / 3) < profile.getWhitePoint()) {
                    return y;
                }
            }
        }
        return image.getHeight() - 1;
    }

    private static int[] getPixelRGB(BufferedImage image, int x, int y) {
        int pixel = image.getRGB(x, y);
        //int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new int[]{red, green, blue};
    }

    private static class Profile {

        //2.54 (sm in inch)
        private int width;
        private int height;

        // colors
        private int blackPoint = 120;
        private int whitePoint = 250;

        // Indents on all sides, for which black spots are not taken into account (may be massive). 25 for 600 DPI
        private int dx;
        private int dy;
        // Increase padding (borders to be forcefully overwritten)
        private int padding;

        public int getDx() {
            return dx;
        }

        public Profile withDx(int dx) {
            this.dx = dx;
            return this;
        }

        public int getDy() {
            return dy;
        }

        public Profile withDy(int dy) {
            this.dy = dy;
            return this;
        }

        public int getPadding() {
            return padding;
        }

        public Profile withPadding(int padding) {
            this.padding = padding;
            return this;
        }

        public int getWidth() {
            return width;
        }

        public Profile withWidth(int width) {
            this.width = width;
            return this;
        }

        public int getHeight() {
            return height;
        }

        public Profile withHeight(int height) {
            this.height = height;
            return this;
        }

        public int getBlackPoint() {
            return blackPoint;
        }

        public Profile withBlackPoint(int blackPoint) {
            this.blackPoint = blackPoint;
            return this;
        }

        public int getWhitePoint() {
            return whitePoint;
        }

        public Profile withWhitePoint(int whitePoint) {
            this.whitePoint = whitePoint;
            return this;
        }
    }

    private static class Padding {

        private int left;
        private int top;
        private int right;
        private int bottom;

        public void appendBy(int k) {
            left = left + k;
            top = top + k;
            right = right + k;
            bottom = bottom + k;
        }

        public void appendSize(int w, int h) {
            int wx = w / 2;
            int wy = h / 2;
            left = left + wx;
            right = right + w - wx;
            top = top + wy;
            bottom = bottom + h - wy;
            if (right < 0) {
                right = 0;
            }
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }

        public int getBottom() {
            return bottom;
        }

        public void setBottom(int bottom) {
            this.bottom = bottom;
        }
    }
}
