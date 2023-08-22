package md.leonis.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

//todo на самом деле нужно много ещё доработок, это скелет.
//например надо центрировать изображение, сделать фон как при загрузке
//в images закинул пример бордюра bordemo6.png
public class DIY {

    private static final double DIAMOND_WIDTH_SM = 0.25;
    private static final int WIDTH_SM = 70;
    private static final int HEIGHT_SM = 60;

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("C:\\Users\\user\\Downloads\\image.png");
        String fileName = path.getFileName().toString();

        BufferedImage sourceImage = ImageIO.read(path.toFile());
        System.out.println(String.format("%sx%s (%s)", sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getWidth() * sourceImage.getHeight()));

        if (sourceImage.getWidth() != 320 || sourceImage.getHeight() != 240) {
            throw new RuntimeException("Wrong ZX Spectrum image size");
        }

        BufferedImage imageWoBorder = (sourceImage.getWidth() == 320) ? sourceImage.getSubimage(32, 24, 256, 192) : sourceImage;

        ImageIO.write(imageWoBorder, "png", path.getParent().resolve(fileName.replace(".", "-clipped.")).toFile());

        Map<Integer, Integer> colors = new HashMap<>();

        for (int y = 0; y < imageWoBorder.getHeight(); y++) {
            for (int x = 0; x < imageWoBorder.getWidth(); x++) {
                int color = imageWoBorder.getRGB(x, y);
                colors.merge(color, 1, Integer::sum);
                //img2.setRGB(x, y, img.getRGB(x, y));
            }
        }

        savePalette(colors, "palette");

        int width = (int) (WIDTH_SM / DIAMOND_WIDTH_SM);
        int height = (int) (HEIGHT_SM / DIAMOND_WIDTH_SM);

        BufferedImage outputImage = new BufferedImage(width, height, TYPE_INT_RGB);

        int colorCounter = 0;

        //todo цвета по индексам надо
        List<Integer> colorsList = new ArrayList<>(colors.keySet());

        for (int x = 0; x < outputImage.getWidth(); x++) {
            for (int y = 0; y < outputImage.getHeight(); y++) {
                outputImage.setRGB(x, y, colorsList.get(colorCounter++));
                if (colorCounter == colorsList.size()) {
                    colorCounter = 0;
                }
            }
        }

        for (int x = 0; x < imageWoBorder.getWidth(); x++) {
            for (int y = 0; y < imageWoBorder.getHeight(); y++) {
                outputImage.setRGB(x, y, imageWoBorder.getRGB(x, y));
            }
        }

        ImageIO.write(outputImage, "png", path.getParent().resolve(fileName.replace(".", "-big.")).toFile());

        colors = new HashMap<>();

        for (int y = 0; y < outputImage.getHeight(); y++) {
            for (int x = 0; x < outputImage.getWidth(); x++) {
                int color = outputImage.getRGB(x, y);
                colors.merge(color, 1, Integer::sum);
                //img2.setRGB(x, y, img.getRGB(x, y));
            }
        }

        savePalette(colors, "palette2");

        List<Integer> plain = new ArrayList<>();
        colors.entrySet().stream().sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).forEach(e -> plain.addAll(Collections.nCopies(e.getValue(), e.getKey())));

        int counter = 0;
        int rectSize = 40;

        for (int x = 0; x < outputImage.getWidth(); x += rectSize) {
            for (int y = 0; y < outputImage.getHeight(); y += rectSize) {
                for (int y2 = 0; y2 < rectSize; y2++) {
                    for (int x2 = 0; x2 < rectSize; x2++) {
                        outputImage.setRGB(x + x2, y + y2, plain.get(counter++));
                    }
                }
            }
        }

        ImageIO.write(outputImage, "png", path.getParent().resolve(fileName.replace(".", "-big-blocks.")).toFile());
    }

    private static void savePalette(Map<Integer, Integer> colors, String title) throws IOException {
        System.out.println("\nPalette: " + title);
        BufferedImage palette = new BufferedImage(800, 600, TYPE_BYTE_INDEXED);

        final int[] yOffset = {0};

        Graphics2D graph = palette.createGraphics();
        graph.setColor(Color.WHITE);
        graph.fill(new Rectangle(0, 0, palette.getWidth(), palette.getHeight()));

        graph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graph.setFont(new Font("Verdana", Font.PLAIN, 20));

        List<Integer> plain = new ArrayList<>();

        colors.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).forEach(e -> {
            plain.addAll(Collections.nCopies(e.getValue(), e.getKey()));

            System.out.printf("$%s: %s%n", longToHex(e.getKey() + 16777216L), e.getValue());

            graph.setColor(new Color(e.getKey()));
            graph.fill(new Rectangle(0, yOffset[0], 130, 50));
            graph.setColor(Color.BLACK);
            graph.draw(new Rectangle(0, yOffset[0], 130, 50));

            graph.setPaint(Color.BLACK);
            graph.drawString("$" + longToHex(e.getKey() + 16777216L), 20, yOffset[0] + 33);
            graph.drawString("" + e.getValue(), 140, yOffset[0] + 33);

            yOffset[0] += 50;
        });

        long total = colors.values().stream().mapToLong(l -> l).sum();
        System.out.println("Total: " + total);

        graph.setPaint(Color.BLACK);
        graph.drawString("  TOTAL:", 20, yOffset[0] + 33);
        graph.drawString("" + total, 140, yOffset[0] + 33);

        graph.dispose();

        ImageIO.write(palette, "png", new File(String.format("C:\\Users\\user\\Downloads\\%s.png", title)));
    }

    private static String longToHex(long l) {
        String zeros = "000000";
        String s = Long.toHexString(l).toUpperCase();
        return zeros.substring(s.length()) + s;
    }
}
