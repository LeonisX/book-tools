package md.leonis.tools;

import boofcv.alg.color.ColorDifference;
import boofcv.alg.color.ColorLab;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

//http://boofcv.org/javadoc/boofcv/alg/color/ColorDifference.html
public class ColorCircle {

    private static List<Dmc> dmcColors = new ArrayList<>();

    private static final Set<Dmc> totalUsedDmcColors = new HashSet<>();
    private static Set<Dmc> usedDmcColors = new HashSet<>(); //todo - выводить использованные цвета + номера

    public static void main(String[] args) throws IOException {
        dmcColors = DmcLoader.loadDmc();

        // Bright
        drawCircles((Integer distance, Integer radius) -> 1.0f, "Bright");
        // Dark
        drawCircles((Integer distance, Integer radius) -> (float) distance / radius, "Dark");

        List<Dmc> unusedDmcColors = getTotalUnusedDmcColors();
        System.out.println(unusedDmcColors.size());
        unusedDmcColors.forEach(System.out::println);
    }

    private static void drawCircles(BiFunction<Integer, Integer, Float> brightnessFunction, String name) {
        // RGB full
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                (Color color) -> color, String.format("rgb%s.png", name));
        // RGB bricks
        drawCircle(brightnessFunction, (Float hue) -> (float) (((int) (hue * 100.0 / 1.2)) * 1.2 / 100.0),
                (Float saturation) -> (float) (((int) (saturation * 100.0 / 4)) * 4 / 100.0), (Color color) -> color, String.format("rgb%sBricks.png", name));
        // DMC bright
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcMapper(), String.format("dmc%s.png", name));
        // DMC alt bright
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcAltMapper(), String.format("dmc%sAlt.png", name));
        // DMC Lab
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcLabMapper(), String.format("dmc%sLab.png", name));
        // DMC CIEDE2000 bright
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcECMCMapper(), String.format("dmc%sECMC.png", name));
        // DMC CIEDE2000 bright
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcECIE76Mapper(), String.format("dmc%sECIE76.png", name));
        // DMC CIEDE2000 bright
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcECIE94Mapper(), String.format("dmc%sECIE94.png", name));
        // DMC CIEDE2000 bright
        drawCircle(brightnessFunction, (Float hue) -> hue, (Float saturation) -> saturation,
                dmcCIEDE2000Mapper(), String.format("dmc%sCIEDE2000.png", name));
    }

    private static Function<Color, Color> dmcMapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distance(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    public static double distance(Color a, Dmc b) {
        return Math.sqrt(Math.pow(a.getRed() - b.getRed(), 2) + Math.pow(a.getGreen() - b.getGreen(), 2) + Math.pow(a.getBlue() - b.getBlue(), 2));
    }

    private static Function<Color, Color> dmcAltMapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceAlt(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    public static double distanceAlt(Color c1, Dmc c2) {
        int red1 = c1.getRed();
        int red2 = c2.getRed();
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        return Math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8));
    }

    private static Function<Color, Color> dmcECMCMapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceECMC(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    private static Function<Color, Color> dmcECIE76Mapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceECIE76(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    private static Function<Color, Color> dmcECIE94Mapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceECIE94(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    private static Function<Color, Color> dmcCIEDE2000Mapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceCIEDE2000(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    public static double distanceECMC(Color a, Dmc b) {
        double[] lab1 = new double[3];
        ColorLab.rgbToLab(a.getRed(), a.getGreen(), a.getBlue(), lab1);
        return ColorDifference.deltaECMC(lab1, b.getLab());
    }

    public static double distanceECIE76(Color a, Dmc b) {
        double[] lab1 = new double[3];
        ColorLab.rgbToLab(a.getRed(), a.getGreen(), a.getBlue(), lab1);
        return ColorDifference.deltaECIE76(lab1, b.getLab());
    }

    public static double distanceECIE94(Color a, Dmc b) {
        double[] lab1 = new double[3];
        ColorLab.rgbToLab(a.getRed(), a.getGreen(), a.getBlue(), lab1);
        return ColorDifference.deltaECIE94(lab1, b.getLab());
    }

    public static double distanceCIEDE2000(Color a, Dmc b) {
        double[] lab1 = new double[3];
        ColorLab.rgbToLab(a.getRed(), a.getGreen(), a.getBlue(), lab1);
        return ColorDifference.deltaECIEDE2000(lab1, b.getLab());
    }

    private static Function<Color, Color> dmcLabMapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceLab(color, c))).map(c -> {
            addUsedDmcColor(c);
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }).orElse(null);
    }

    public static double distanceLab(Color a, Dmc b) {
        double[] lab1 = new double[3];
        ColorLab.rgbToLab(a.getRed(), a.getGreen(), a.getBlue(), lab1);
        double[] lab2 = b.getLab();
        //System.out.printf("L = %s, A = %s, B = %s%n", lab2[0], lab2[1], lab2[2]);
        return Math.sqrt(Math.pow(lab1[0] - lab2[0], 2) + Math.pow(lab1[1] - lab2[1], 2) + Math.pow(lab1[2] - lab2[2], 2));
    }

    private static void drawCircle(BiFunction<Integer, Integer, Float> brightnessFunction,
                                   Function<Float, Float> hueFunction,
                                   Function<Float, Float> saturationFunction,
                                   Function<Color, Color> dmcMapper,
                                   String fileName) {
        usedDmcColors = new HashSet<>();
        long started = System.nanoTime();
        int rad = 128;
        BufferedImage img = new BufferedImage(rad, rad, BufferedImage.TYPE_INT_RGB);

        int width = img.getWidth();
        int height = img.getHeight();
        int centerX = img.getWidth() / 2;
        int centerY = img.getHeight() / 2;
        int radius = (img.getWidth() / 2) * (img.getWidth() / 2);

        // Draw color wheel
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int a = i - centerX;
                int b = j - centerY;

                int distance = a * a + b * b;

                if (distance < radius) {
                    float hue = (float) (Math.atan2(j - height / 2.0, i - width / 2.0) / (2 * Math.PI) + 0.5);
                    float saturation = (float) (Math.sqrt((i - width / 2.0) * (i - width / 2.0) + (j - height / 2.0) * (j - height / 2.0)) / (width / 2.0));

                    float brightness = brightnessFunction.apply(distance, radius);

                    hue = hueFunction.apply(hue);
                    saturation = saturationFunction.apply(saturation);

                    Color color = Color.getHSBColor(hue, saturation, brightness);
                    color = dmcMapper.apply(color);

                    img.setRGB(i, j, RGBtoHEX(color));
                } else {
                    img.setRGB(i, j, 0xFFFFFF);
                }
            }
        }

        //todo total, colors

        try {
            System.out.println(fileName + ": " + (System.nanoTime() - started) / 1_000_000_000 + " s, used: " + usedDmcColors.size() + " colors");
            ImageIO.write(img, "png", new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Dmc> getDmcColors() {
        return dmcColors;
    }

    public static Set<Dmc> getUsedDmcColors() {
        return usedDmcColors;
    }

    public static List<Dmc> getTotalUnusedDmcColors() {
        List<Dmc> unusedDmcColors = new ArrayList<>(getDmcColors());
        unusedDmcColors.removeAll(totalUsedDmcColors);

        return unusedDmcColors;
    }

    public static void addUsedDmcColor(Dmc dmc) {
        usedDmcColors.add(dmc);
        totalUsedDmcColors.add(dmc);
    }

    public static int RGBtoHEX(Color color) {
        String hex = Integer.toHexString(color.getRGB() & 0xffffff);
        if (hex.length() < 6) {
            if (hex.length() == 5)
                hex = "0" + hex;
            if (hex.length() == 4)
                hex = "00" + hex;
            if (hex.length() == 3)
                hex = "000" + hex;
        }
        hex = "#" + hex;
        return Integer.decode(hex);
    }
}
