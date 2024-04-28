package md.leonis.tools;

import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CrostiParser {

    // Теги: юмор, фентези, сказка, баба-яга, кот
    // Поминки Кащея
    private static final String URI_PAGES = "https://crosti.ru/patterns/alnum/";
    private static final String URI_PAGE = "https://crosti.ru/patterns/part/%s/%s";
    private static final String URI_PALETTE = "https://crosti.ru/patterns/palette/";

    private static final String ID = "2738158";

    public static void main(String[] args) throws IOException, TransformerException {

        System.out.println("Parse main page");
        Document pages = Jsoup.connect(URI_PAGES + ID).get();

        // Parse picture info and pages list
        Elements as = pages.getElementsByClass("speedbar").get(0).getElementsByTag("A");
        String title = as.get(as.size() - 1).text();

        Elements descriptions = pages.getElementsByClass("description").get(0).getElementsByTag("DIV");
        String author = descriptions.get(1).text();                                               // MarTatiana
        String size = descriptions.get(2).text().split(" ")[0];                             // 200x250 крестов
        String colorsCount = descriptions.get(3).text().split(",")[1].trim().split(" ")[0];// todo palette?? DMC (ДМС), 60 цветов

        Elements tds = pages.getElementById("alnum-parts").getElementsByTag("TD");
        List<Integer> pageIds = tds.stream().map(td -> Integer.parseInt(td.text())).toList();

        Map<Pair<Integer, Integer>, String> cells = new LinkedHashMap<>();

        final int[] width = {0};
        final int[] height = {0};

        //Parse pages
        for (Integer pageId : pageIds) {

            System.out.println("Parse page #" + pageId);
            // convert page to generated HTML and convert to document
            Document page = Jsoup.connect(String.format(URI_PAGE, ID, pageId)).get();
            //System.out.println(page);

            Element script = page.getElementsByTag("SCRIPT").get(0);

            List<String> jsCode = List.of(script.childNodes().get(0).outerHtml().split("\n"));
            String ptSquare = jsCode.stream().filter(l -> l.contains("ptSquare")).findFirst().orElse("");
            String ptMap = jsCode.stream().filter(l -> l.contains("ptMap")).findFirst().orElse("");

            //System.out.println(ptSquare);
            //System.out.println(ptMap);

            List<Integer> square = Arrays.stream(ptSquare.split("\\[")[1].split("]")[0].split(", ")).map(Integer::parseInt).toList();
            int a = 0xA;
            List<String> map = Arrays.stream(ptMap.split("\\[")[1].split("]")[0].split(", ")).map(Integer::parseInt)
                    .map(k -> String.format("%02x", (a + (k / 10)) * 16 + k % 10).toUpperCase()).toList();

            //System.out.println(square);
            //System.out.println(map);

            //      var ptSquare = [0, 0, 29, 39];
            //      var ptPalette = ["252, 252, 255", "57, 48, 104", "225, 244, 119", "173, 194, 56", "130, 125, 125", "226, 165, 152", "94, 15, 119", "110, 46, 155", "145, 114, 69", "184, 185, 189", "218, 162, 111", "252, 249, 153", "209, 222, 117", "151, 11, 44", "29, 54, 42", "137, 184, 159", "172, 218, 193", "148, 183, 203", "131, 138, 41", "82, 173, 171", "144, 142, 133", "236, 191, 125", "242, 220, 159", "246, 239, 218", "253, 219, 99", "253, 233, 139", "187, 156, 84", "247, 201, 176", "178, 105, 35", "124, 130, 181", "124, 95, 32", "210, 180, 104", "163, 125, 100", "101, 19, 41", "56, 74, 74", "97, 118, 116", "73, 92, 107", "147, 160, 175", "69, 39, 26", "9, 9, 47", "207, 117, 50", "236, 143, 67", "175, 169, 123", "134, 106, 118", "175, 152, 160", "210, 210, 202", "186, 201, 204", "233, 244, 250", "70, 114, 147", "207, 134, 109", "89, 63, 43", "98, 82, 76", "96, 147, 122", "204, 201, 89", "146, 77, 120", "217, 234, 242", "31, 127, 160", "94, 204, 236", "172, 133, 131", "110, 73, 42"];
            //      var ptMap = [8, 32, 51, 4, 51, 51, 30, 50, 51, 32, 43, 4, 43, 35, 51, 51, 20, 43, 43, 30, 30, 51, 32, 4, 4, 8, 35, 51, 42, 20, 8, 51, 50, 50, 50, 34, 34, 38, 34, 38, 50, 34, 51, 50, 50, 51, 34, 51, 50, 34, 30, 51, 50, 34, 38, 34, 34, 34, 34, 50, 30, 59, 4, 44, 43, 20, 4, 20, 20, 43, 4, 20, 20, 4, 20, 20, 4, 35, 44, 4, 32, 37, 4, 43, 37, 4, 20, 37, 43, 4, 4, 38, 34, 51, 35, 20, 35, 18, 4, 35, 4, 37, 4, 20, 4, 4, 20, 35, 51, 43, 4, 4, 43, 4, 20, 37, 20, 37, 43, 43, 30, 50, 50, 51, 37, 37, 9, 4, 29, 20, 46, 37, 37, 9, 37, 37, 46, 37, 9, 9, 37, 20, 37, 46, 44, 37, 44, 44, 46, 22, 35, 38, 34, 51, 20, 17, 9, 46, 20, 37, 46, 20, 9, 37, 20, 46, 37, 20, 46, 37, 29, 20, 4, 37, 37, 4, 20, 9, 46, 9, 51, 38, 51, 35, 36, 37, 37, 37, 55, 9, 37, 52, 46, 35, 29, 9, 35, 37, 9, 29, 4, 20, 29, 37, 9, 44, 9, 46, 9, 58, 43, 50, 34, 4, 9, 4, 9, 9, 45, 55, 46, 9, 9, 37, 46, 17, 37, 46, 9, 9, 9, 9, 9, 44, 46, 55, 9, 46, 9, 44, 8, 50, 34, 35, 9, 37, 52, 46, 46, 9, 55, 55, 20, 9, 46, 37, 9, 9, 9, 37, 37, 37, 9, 9, 46, 46, 46, 44, 37, 9, 43, 34, 34, 34, 4, 45, 20, 37, 55, 37, 46, 55, 9, 46, 9, 9, 46, 46, 9, 37, 9, 9, 9, 9, 46, 46, 46, 9, 9, 45, 43, 50, 34, 51, 35, 9, 46, 46, 46, 55, 46, 37, 55, 55, 9, 45, 45, 45, 9, 46, 9, 9, 9, 46, 46, 46, 45, 46, 46, 46, 30, 34, 59, 51, 37, 9, 45, 46, 46, 9, 9, 46, 9, 46, 55, 9, 46, 9, 9, 37, 37, 44, 9, 44, 9, 44, 9, 46, 45, 46, 30, 38, 51, 51, 4, 52, 20, 9, 46, 37, 37, 45, 9, 20, 55, 9, 9, 37, 37, 9, 37, 9, 46, 9, 46, 9, 9, 23, 45, 45, 4, 59, 34, 51, 35, 37, 37, 9, 46, 37, 46, 46, 46, 55, 9, 45, 47, 9, 9, 9, 9, 46, 9, 46, 44, 9, 9, 55, 9, 45, 4, 50, 50, 51, 37, 37, 52, 37, 46, 9, 46, 37, 37, 55, 45, 9, 9, 46, 9, 20, 44, 9, 5, 9, 44, 9, 45, 46, 27, 45, 43, 50, 50, 51, 20, 37, 17, 9, 46, 55, 9, 37, 45, 45, 46, 46, 44, 9, 45, 9, 46, 46, 46, 9, 9, 46, 22, 9, 22, 45, 4, 50, 51, 35, 37, 37, 20, 37, 9, 46, 9, 9, 46, 9, 37, 9, 46, 37, 9, 22, 45, 46, 46, 9, 44, 46, 46, 9, 45, 23, 30, 34, 51, 35, 35, 20, 15, 37, 37, 9, 55, 9, 20, 9, 44, 58, 9, 46, 44, 9, 45, 23, 46, 9, 9, 22, 9, 45, 22, 46, 51, 34, 51, 18, 20, 17, 37, 29, 46, 55, 9, 9, 37, 9, 44, 20, 58, 9, 45, 9, 44, 46, 46, 44, 46, 22, 44, 45, 45, 9, 32, 51, 14, 35, 9, 52, 4, 17, 9, 37, 4, 37, 37, 44, 37, 29, 20, 20, 9, 23, 9, 44, 46, 46, 23, 46, 9, 23, 9, 9, 4, 50, 34, 51, 37, 4, 17, 37, 35, 37, 20, 29, 37, 9, 44, 37, 44, 44, 46, 46, 46, 9, 44, 45, 47, 9, 45, 46, 44, 45, 8, 50, 51, 34, 37, 46, 37, 35, 20, 37, 4, 4, 4, 9, 9, 29, 20, 44, 9, 44, 58, 9, 5, 45, 9, 46, 23, 37, 9, 23, 43, 34, 51, 35, 20, 9, 35, 35, 4, 37, 20, 4, 4, 37, 46, 4, 44, 44, 44, 5, 58, 44, 23, 46, 58, 46, 45, 9, 45, 45, 51, 38, 35, 34, 14, 35, 18, 14, 34, 43, 51, 36, 51, 51, 37, 44, 44, 35, 4, 44, 20, 44, 5, 44, 45, 46, 32, 9, 27, 44, 20, 51, 34, 34, 34, 35, 37, 35, 51, 4, 35, 34, 51, 35, 20, 9, 4, 51, 51, 44, 44, 20, 4, 9, 22, 9, 44, 46, 45, 45, 20, 51, 50, 35, 37, 37, 9, 9, 44, 9, 9, 9, 45, 45, 45, 9, 46, 9, 9, 45, 46, 9, 46, 23, 46, 45, 23, 45, 5, 45, 32, 51, 51, 34, 43, 20, 37, 46, 20, 37, 46, 46, 9, 9, 46, 9, 9, 9, 45, 23, 46, 9, 45, 27, 9, 45, 27, 22, 22, 46, 20, 51, 51, 51, 35, 20, 20, 9, 9, 45, 46, 20, 20, 37, 9, 9, 9, 45, 23, 46, 9, 46, 46, 46, 46, 45, 46, 9, 45, 23, 4, 34, 51, 51, 4, 20, 17, 9, 46, 9, 9, 46, 9, 46, 46, 46, 46, 9, 46, 46, 9, 46, 37, 46, 45, 9, 46, 46, 9, 27, 4, 59, 34, 35, 37, 17, 37, 20, 37, 4, 9, 46, 9, 9, 46, 45, 9, 9, 9, 22, 22, 46, 9, 45, 46, 27, 45, 46, 22, 45, 37, 35, 34, 35, 4, 4, 20, 37, 46, 46, 9, 9, 20, 37, 9, 46, 9, 45, 45, 22, 47, 46, 22, 27, 45, 46, 27, 45, 46, 27, 37, 51, 51, 51, 20, 9, 9, 15, 37, 9, 37, 46, 46, 46, 9, 46, 45, 22, 22, 22, 23, 23, 45, 22, 45, 22, 22, 27, 45, 22, 32, 34, 51, 35, 37, 37, 4, 52, 20, 15, 37, 37, 9, 37, 37, 45, 45, 9, 44, 9, 22, 45, 9, 9, 45, 45, 45, 27, 23, 22, 4, 50, 35, 18, 4, 37, 20, 37, 20, 37, 37, 20, 9, 9, 46, 9, 37, 9, 46, 46, 46, 45, 27, 46, 45, 46, 45, 23, 22, 22, 32, 34, 51, 35, 18, 20, 20, 37, 52, 20, 17, 37, 55, 45, 37, 37, 45, 23, 45, 46, 45, 27, 55, 22, 45, 45, 23, 22, 45, 22, 20, 50, 34, 35, 37, 17, 15, 37, 17, 9, 16, 55, 46, 46, 46, 45, 55, 45, 45, 42, 9, 22, 55, 23, 23, 47, 0, 47, 0, 0, 32, 34, 51, 18, 37, 9, 9, 45, 16, 46, 46, 46, 37, 46, 47, 45, 46, 45, 45, 4, 37, 23, 23, 47, 0, 0, 0, 0, 47, 0, 32, 34, 51, 4, 9, 46, 46, 15, 37, 17, 37, 9, 45, 55, 22, 46, 9, 9, 45, 20, 9, 47, 0, 47, 47, 0, 0, 47, 0, 0, 4, 38, 35, 20, 20, 37, 37, 17, 46, 16, 46, 55, 55, 37, 9, 9, 15, 37, 9, 20, 9, 23, 47, 23, 23, 47, 0, 0, 0, 0, 4, 50, 35, 4, 37, 17, 46, 46, 15, 37, 37, 37, 37, 20, 9, 16, 46, 20, 4, 9, 45, 23, 23, 55, 23, 47, 0, 0, 47, 23];

            height[0] = Math.max(height[0], square.get(3) + 1);
            width[0] = Math.max(width[0], square.get(2) + 1);

            int h = square.get(3) + 1 - square.get(1);
            int w = square.get(2) + 1 - square.get(0);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    cells.put(new Pair<>(x + square.get(0), y + square.get(1)), map.get(y * w + x));
                }
            }
        }

        //Parse palette
        System.out.println("Parse palette page");
        Document palette = Jsoup.connect(URI_PALETTE + ID).get();

        Map<String, String> colorsMap = palette.getElementsByClass("color").stream()
                .collect(Collectors.toMap(c -> c.getElementsByTag("STRONG").get(0).text(), c -> c.ownText().split(" ")[0]));

        Map<String, String> colorsPaletteMap = palette.getElementsByClass("color").stream()
                .collect(Collectors.toMap(c -> c.getElementsByTag("STRONG").get(0).text(),
                        c -> c.getElementsByTag("SPAN").get(0).attr("STYLE").replace("\"", "").split(" ")[1]));

        //Load Palette
        List<Dmc> dmcColors = DmcLoader.loadDmc();
        Map<String, Dmc> dmcColorsMap = dmcColors.stream().collect(Collectors.toMap(Dmc::getColor, Function.identity()));

        //Map palette
        //Draw image
        BufferedImage img = new BufferedImage(width[0], height[0], BufferedImage.TYPE_INT_RGB);

        cells.forEach((pair, color) -> {
            /*Dmc dmc = dmcColorsMap.get(colorsMap.get(color));
            img.setRGB(pair.getKey(), pair.getValue(), RGBtoHEX(new Color(dmc.getRed(), dmc.getGreen(), dmc.getBlue())));*/
            img.setRGB(pair.getKey(), pair.getValue(), Integer.decode(colorsPaletteMap.get(color)));
        });

        String fileName = String.format("%s-%s-%s (%s).png", title, size, colorsCount, author);

        try {
            System.out.println(fileName);
            ImageIO.write(img, "png", new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //todo unify
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
