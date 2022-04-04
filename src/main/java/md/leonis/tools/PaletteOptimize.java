package md.leonis.tools;

import net.sf.image4j.codec.bmp.BMPImage;
import net.sf.image4j.codec.bmp.BMPReader;
import net.sf.image4j.codec.bmp.BMPWriter;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Load 8-bit BMP only!!!!!!!!!!!!
public class PaletteOptimize {

    private static final int threshold = 120; // разделитель между тёмными и светлыми цветами
    private static final int whiteGroup = 8; // диапазон группировки светлых цветов; если тёмный фон, то 8, если белый, то 16

    private static final Path path = Paths.get("C:\\Users\\user\\Documents\\1500 игр Sega\\1500 игр Sega\\bmp256");
    private static final Path outPath = path.resolve("optimized");

    public static void main(String[] args) throws IOException {

        Files.createDirectories(outPath);

        List<String> files = Files.list(path).filter(f -> !Files.isDirectory(f)).map(f -> f.getFileName().toString()).collect(Collectors.toList());
        for (String file : files) {
            System.out.println(file);
            BMPImage bmpImage = BMPReader.readExt(path.resolve(file));
            //BufferedImage sourceBufferedImage = ImageIO.read(path.resolve(file).toFile());
            //BufferedImage indexedImage = optimizePalette(bmpImage.getImage());
            IndexColorModel icm = getOptimizedPalette(bmpImage.getImage());
            BMPWriter.write8bit(bmpImage, icm, outPath.resolve(file));
            //ImageIO.write(indexedImage, "BMP", outPath.resolve(file).toFile());
        }
    }

    private static IndexColorModel getOptimizedPalette(BufferedImage sourceBufferedImage) {

        IndexColorModel icm = (IndexColorModel) sourceBufferedImage.getColorModel();
        int[] palette = new int[icm.getMapSize()];
        icm.getRGBs(palette);

        int size = icm.getMapSize();

        byte[] reds = new byte[size];
        byte[] greens = new byte[size];
        byte[] blues = new byte[size];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);

        int red = IntStream.range(0, size).map(idx -> to256(reds[idx])).sum() / size;
        int green = IntStream.range(0, size).map(idx -> to256(greens[idx])).sum() / size;
        int blue = IntStream.range(0, size).map(idx -> to256(blues[idx])).sum() / size;
        int mid = (red + green + blue) / 3;

        for (int i = 0; i < size; i++) {
            int color = (to256(reds[i]) + to256(greens[i]) + to256(blues[i])) / 3;

            if (color > threshold) {
                int dh = whiteGroup / 2;
                color = color / whiteGroup * whiteGroup + dh;
                reds[i] = (byte) (Math.min(255, color / whiteGroup * whiteGroup + dh + (red - mid)));
                greens[i] = (byte) (Math.min(255, color / whiteGroup * whiteGroup + dh + (green - mid)));
                blues[i] = (byte) (Math.min(255, color / whiteGroup * whiteGroup + dh + (blue - mid)));

            } else {
                color = color / 4 * 4;
                reds[i] = (byte) color;
                greens[i] = (byte) color;
                blues[i] = (byte) color;
            }
        }

        return new IndexColorModel(8, size, reds, greens, blues);
    }

    private static BufferedImage optimizePalette(BufferedImage sourceBufferedImage) {

        IndexColorModel icm = (IndexColorModel) sourceBufferedImage.getColorModel();
        int[] palette = new int[icm.getMapSize()];
        icm.getRGBs(palette);

        int size = icm.getMapSize();

        byte[] reds = new byte[size];
        byte[] greens = new byte[size];
        byte[] blues = new byte[size];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);

        int red = IntStream.range(0, size).map(idx -> to256(reds[idx])).sum() / size;
        int green = IntStream.range(0, size).map(idx -> to256(greens[idx])).sum() / size;
        int blue = IntStream.range(0, size).map(idx -> to256(blues[idx])).sum() / size;
        int mid = (red + green + blue) / 3;

        for (int i = 0; i < size; i++) {
            int color = (to256(reds[i]) + to256(greens[i]) + to256(blues[i])) / 3;

            if (color > threshold) {
                int d = 8;
                int dh = d / 2;
                color = color / d * d + dh;
                reds[i] = (byte) (color / d * d + dh + (red - mid));
                greens[i] = (byte) (color / d * d + dh + (green - mid));
                blues[i] = (byte) (color / d * d + dh + (blue - mid));

            } else {
                color = color / 4 * 4;
                reds[i] = (byte) color;
                greens[i] = (byte) color;
                blues[i] = (byte) color;
            }
        }

        BufferedImage indexedImage = new BufferedImage(sourceBufferedImage.getWidth(), sourceBufferedImage.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
        WritableRaster raster = indexedImage.getRaster();
        IndexColorModel icm2 = new IndexColorModel(8, size, reds, greens, blues);
        indexedImage = new BufferedImage(icm2, raster, sourceBufferedImage.isAlphaPremultiplied(), null);
        indexedImage.getGraphics().drawImage(sourceBufferedImage, 0, 0, null);
        return indexedImage;
    }

    static int to256(byte color) {
        return (256 + color) % 256;
    }
}
