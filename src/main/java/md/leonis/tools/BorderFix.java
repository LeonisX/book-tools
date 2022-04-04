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

// Process images with any colors count, ex. 16M
public class BorderFix {

    private static final Profile profile = new Profile()
            .withPath("C:\\Users\\user\\Documents\\Simba's Описание видеоигр. Том 4\\bmp\\result")
            // final page dimensions
            .withWidth(3820)
            .withHeight(5530)
            // Indents on all sides, for which black spots are not taken into account (may be massive). 25 for 600 DPI
            .withDx(30)
            .withDy(30)
            // colors
            .withBlackPoint(80) // Max 255
            .withWhitePoint(250) // Max 255
            // increase padding
            .withPadding(5) // additional padding
            .withFixBlackLeft(true) // Fix black fields from left side
            .withFixBlackRight(true) // Fix black fields from right side
            .withAllowedBlackPoints(7); // 0 for normal images, add more for very dirty backgrounds

    private static final boolean debug = false;

    public static void main(String[] args) throws IOException {

        /*int multiplier = 5;
        String fileName = "corrupted-border-" + multiplier + ".bmp";
        processImage(readImage(fileName), fileName);*/

        if (debug) {
            profile.withPath("C:\\Users\\user\\Documents\\Страна PlayStation Большая книга многосерийных хитов\\test");
            String fileName = "page_288.bmp";
            Files.createDirectories(profile.getOutputPath());
            processImage(readImage(fileName), fileName);
            /*fileName = "page_191.bmp";
            processImage(readImage(fileName), fileName);
            fileName = "page_192.bmp";
            processImage(readImage(fileName), fileName);*/

        } else {

            Files.createDirectories(profile.getOutputPath());

            List<String> files = Files.list(profile.getPath()).filter(f -> !Files.isDirectory(f)).map(f -> f.getFileName().toString()).collect(Collectors.toList());
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

        /*if (profile.isForceFixBorders()) {
            int wx = (profile.getWidth() - image.getWidth()) / 2;
            int wy = (profile.getHeight() - image.getHeight()) / 2;
            padding = new Padding(wx, wy, wx, wy);
        } else {
            padding = getPadding(image);
            padding.appendSize(profile.getWidth() - image.getWidth(), profile.getHeight() - image.getHeight());
        }*/


        padding.appendBy(profile.getPadding());
        BufferedImage finalImage = finalizeImage(resizedImage, padding);
        if (debug) {
            saveImage(finalImage, file, "final");
        } else {
            saveImage(finalImage, file, "");
        }

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
        return ImageIO.read(profile.getPath().resolve(file).toFile());
    }

    private static void saveImage(BufferedImage image, String file, String prefix) throws IOException {
        prefix = prefix.isEmpty() ? prefix : "-" + prefix;
        ImageIO.write(image, "BMP", profile.getOutputPath().resolve(file.replace(".", prefix + ".")).toFile());
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

        if (profile.isFixBlackLeft()) {
            for (int yi = profile.getDy(); yi < image.getHeight() - profile.getDy(); yi++) {
                int x = getBlackPointXLeft(image, yi);
                padding.setLeft(Math.max(padding.getLeft(), x));
            }
        }
        if (profile.isFixBlackRight()) {
            for (int yi = profile.getDy(); yi < image.getHeight() - profile.getDy(); yi++) {
                int x = image.getWidth() - getBlackPointXRight(image, yi) - 1;
                padding.setRight(Math.max(padding.getRight(), x));
            }
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

        int k = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) < profile.getBlackPoint()) {
                k++;
            }
            if (k > profile.getAllowedBlackPoints()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTextRow(BufferedImage image, int x) {

        int k = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            int[] pixel = getPixelRGB(image, x, y);
            if (((pixel[0] + pixel[1] + pixel[2]) / 3) < profile.getBlackPoint()) {
                k++;
            }
            if (k > profile.getAllowedBlackPoints()) {
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

        private Path path;
        private Path outputPath;

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

        private boolean fixBlackLeft = true;
        private boolean fixBlackRight = true;

        private int allowedBlackPoints = 0;

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

        public boolean isFixBlackLeft() {
            return fixBlackLeft;
        }

        public Profile withFixBlackLeft(boolean fixBlackLeft) {
            this.fixBlackLeft = fixBlackLeft;
            return this;
        }

        public boolean isFixBlackRight() {
            return fixBlackRight;
        }

        public Profile withFixBlackRight(boolean fixBlackRight) {
            this.fixBlackRight = fixBlackRight;
            return this;
        }

        public int getAllowedBlackPoints() {
            return allowedBlackPoints;
        }

        public Profile withAllowedBlackPoints(int allowedBlackPoints) {
            this.allowedBlackPoints = allowedBlackPoints;
            return this;
        }

        public Path getPath() {
            return path;
        }

        public Profile withPath(String path) {
            this.path = Paths.get(path);
            this.outputPath = this.path.resolve("out");
            return this;
        }

        public Path getOutputPath() {
            return outputPath;
        }


    }

    private static class Padding {

        private int left;
        private int top;
        private int right;
        private int bottom;

        public Padding() {
        }

        public void appendBy(int k) {
            left = left + k;
            top = top + k;
            right = right + k;
            bottom = bottom + k;
        }

        public void appendSize(int w, int h) {

            int wx = w / 2;
            int wy = h / 2;

            if (!profile.isFixBlackLeft()) {
                right = right + w;
            } else if (!profile.isFixBlackRight()) {
                left = left + w;
            } else {
                left = left + wx;
                right = right + wx;
            }

            top = top + wy;
            bottom = bottom + wy;
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
