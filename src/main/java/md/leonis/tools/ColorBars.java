package md.leonis.tools;

import boofcv.alg.color.ColorDifference;
import boofcv.alg.color.ColorLab;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;

//http://boofcv.org/javadoc/boofcv/alg/color/ColorDifference.html
public class ColorBars {

    private static List<Dmc> dmcColors = new ArrayList<>();

    private static final Set<Dmc> totalUsedDmcColors = new HashSet<>();
    private static Set<Dmc> usedDmcColors = new HashSet<>(); //todo - выводить использованные цвета + номера

    public static void main(String[] args) throws IOException {
        dmcColors = DmcLoader.loadDmc();

        drawBars();

        List<Dmc> unusedDmcColors = getTotalUnusedDmcColors();
        System.out.println(unusedDmcColors.size());
        unusedDmcColors.forEach(System.out::println);
    }

    private static void drawBars() {
        // RGB full
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, (Color color) -> color, "bar-rgb.png");
        // RGB bricks
        drawBar((Float hue) -> (float) (((int) (hue * 100.0 / 1.2)) * 1.2 / 100.0),
                (Float saturation) -> (float) (((int) (saturation * 100.0 / 2)) * 2 / 100.0), (Color color) -> color, "bar-rgbBricks.png");
        // DMC
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, dmcMapper(), "bar-dmc.png");
        // DMC alt
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, dmcAltMapper(), "bar-dmcAlt.png");
        // DMC ECMC
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, dmcECMCMapper(), "bar-dmcECMC.png");
        // DMC ECIE76
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, dmcECIE76Mapper(), "bar-dmcECIE76.png"); // То же что и DMC Lab
        // DMC ECIE94
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, dmcECIE94Mapper(), "bar-dmcECIE94.png");
        // DMC CIEDE2000
        drawBar((Float hue) -> hue, (Float saturation) -> saturation, dmcCIEDE2000Mapper(), "bar-dmcCIEDE2000.png");
    }

    private static void drawBar(Function<Float, Float> hueFunction,
                                Function<Float, Float> saturationFunction,
                                Function<Color, Color> dmcMapper, String fileName) {
        usedDmcColors = new HashSet<>();
        long started = System.nanoTime();

        int k = 2;

        int barHeight = 5;
        int barHeightK = barHeight * k;

        int precision = 128;
        int precisionK = precision * k;

        int width = 1024 * k;
        int height = 512 * k + 8 * barHeightK;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Draw color wheel
        //todo рисовать полосы одного цвета, перебирать на них s, b
        //для простоты 1024x512
        //128 цветов, 64, 64 - полоса 10 пикселей основного цвета сверху

        Graphics2D graph = img.createGraphics();

        for (int cy = 0; cy < 4; cy++) {
            for (int cx = 0; cx < 8; cx++) {

                float hue = (cy * 8f + cx) / (8 * 4) /*+ 1f / (precisionK * 8 * 4) * 40*/;
                hue = hueFunction.apply(hue);

                for (int s = 0; s < precisionK; s++) {
                    float saturation = 1f - 1f * (s + 1) / precisionK;
                    saturation = saturationFunction.apply(saturation);

                    for (int b = 0; b < precisionK; b++) {
                        float brightness = 1f - 1f * (b + 1) / precisionK;

                        Color color = Color.getHSBColor(hue, saturation, brightness);
                        color = dmcMapper.apply(color);

                        img.setRGB(cx * precisionK + s, cy * (precisionK + barHeightK) + barHeightK + b, RGBtoHEX(color));
                    }
                }

                for (int i = 0; i < precisionK; i++) {
                    hue = (cy * 8f + cx) / (8 * 4) + 1f / (precisionK * 8 * 4) * i;
                    graph.setColor(Color.getHSBColor(hue, 1f, 1f));
                    graph.fill(new Rectangle(cx * precisionK + i, cy * (precisionK + barHeightK), 1, barHeightK));
                }
            }
        }

        /*for (int cy = 0; cy < 8; cy ++) {
            for (int cx = 0; cx < 16; cx++) {

                float hue = (cy * 16f + cx) / (16 * 8);
                hue = hueFunction.apply(hue);

                for (int s = 0; s < precisionK; s++) {
                    float saturation = 1f - 1f * (s + 1) / precisionK;
                    saturation = saturationFunction.apply(saturation);

                    for (int b = 0; b < precisionK; b++) {
                        float brightness = 1f - 1f * (b + 1) / precisionK;

                        Color color = Color.getHSBColor(hue, saturation, brightness);
                        color = dmcMapper.apply(color);

                        img.setRGB(cx * precisionK + s, cy * (precisionK + barHeightK) + barHeightK + b, RGBtoHEX(color));
                    }
                }

                for (int i = 0; i < precisionK; i ++) {
                    hue = (cy * 16f + cx) / (16 * 8) + 1f / (precisionK * 16 * 8) * i;
                    graph.setColor(Color.getHSBColor(hue, 1f, 1f));
                    graph.fill(new Rectangle(cx * precisionK + i, cy * (precisionK + barHeightK), 1, barHeightK));
                }
            }
        }*/

        graph.dispose();

        /*for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //float hue = (float) (j / 2 + 240) / 360 / 2;
                float hue = (float) (j + 240) / 360 / 2;
                //float saturation = i < wc ? 1f - (wc - i) / wc : 1f; //todo log function
                //float brightness = i > wc ? 1f - (i - wc) / wc : 1f; //todo log function

                float saturation = 1f;

                if (i < wc) {
                    saturation = 1f - (wc - i) / wc;
                } else if (i > wc) {
                    saturation = 1f - (i - wc) / wc;
                }

                float brightness = 1f;

                if (i > wc) {
                    brightness = 1f - (i - wc) / wc;
                } else if (i < wc / 2 && j < hc) {
                    brightness = 1.5f - (wc - i) / wc;
                }

                hue = hueFunction.apply(hue);
                saturation = saturationFunction.apply(saturation);

                Color color = Color.getHSBColor(hue, saturation, brightness);
                color = dmcMapper.apply(color);

                img.setRGB(i, j, RGBtoHEX(color));
            }
        }*/

        /*for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //float hue = (float) (j / 2 + 240) / 360 / 2;
                float hue = (float) (j + 240) / 360 / 2;
                //float saturation = i < wc ? 1f - (wc - i) / wc : 1f; //todo log function
                //float brightness = i > wc ? 1f - (i - wc) / wc : 1f; //todo log function

                float saturation = 1f;

                if (i < wc) {
                    saturation = 1f - (wc - i) / wc;
                } else if (i > wc) {
                    saturation = 1f - (i - wc) / wc;
                }

                float brightness = 1f;

                if (i > wc) {
                    brightness = 1f - (i - wc) / wc;
                } else if (i < wc / 2 && j < hc) {
                    brightness = 1.5f - (wc - i) / wc;
                }

                hue = hueFunction.apply(hue);
                saturation = saturationFunction.apply(saturation);

                Color color = Color.getHSBColor(hue, saturation, brightness);
                color = dmcMapper.apply(color);

                img.setRGB(i, j, RGBtoHEX(color));
            }
        }*/

        //todo total, colors

        try {
            System.out.println(fileName + ": " + (System.nanoTime() - started) / 1_000_000_000 + " s, used: " + usedDmcColors.size() + " colors");
            ImageIO.write(img, "png", new File(fileName));
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private static Function<Color, Color> dmcMapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distance(color, c))).map(ColorBars::awtColor).orElse(null);
    }

    public static double distance(Color a, Dmc b) {
        return Math.sqrt(Math.pow(a.getRed() - b.getRed(), 2) + Math.pow(a.getGreen() - b.getGreen(), 2) + Math.pow(a.getBlue() - b.getBlue(), 2));
    }

    private static Function<Color, Color> dmcAltMapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceAlt(color, c))).map(ColorBars::awtColor).orElse(null);
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
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceECMC(color, c))).map(ColorBars::awtColor).orElse(null);
    }

    private static Function<Color, Color> dmcECIE76Mapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceECIE76(color, c))).map(ColorBars::awtColor).orElse(null);
    }

    private static Function<Color, Color> dmcECIE94Mapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceECIE94(color, c))).map(ColorBars::awtColor).orElse(null);
    }

    private static Function<Color, Color> dmcCIEDE2000Mapper() {
        return color -> getDmcColors().parallelStream().min(Comparator.comparingDouble(c -> distanceCIEDE2000(color, c))).map(ColorBars::awtColor).orElse(null);
    }

    private static Color awtColor(Dmc c) {
        addUsedDmcColor(c);
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
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
