package md.leonis;

import javafx.util.Pair;
import md.leonis.utils.BinaryUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Crc32Calculator {

    public static final Path PATH = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer");
    public static final Path CRC32_PATH = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer\\crc32.txt");
    public static final Path GNURIP_PATH = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer\\gnurip.txt");

    public static void main(String[] args) throws IOException {
        //saveCrc32();
        compare();
    }

    private static void compare() throws IOException {
        List<Pair<String, String>> list = Files.readAllLines(CRC32_PATH).stream()
                .map(l -> new Pair<>(l.split("\t")[0].trim(), l.split("\t")[1].trim())).collect(Collectors.toList());

        List<Pair<String, String>> gnuList = Files.readAllLines(GNURIP_PATH).stream()
                .map(l -> new Pair<>(l.split("\t")[0].trim(), l.split("\t")[1].trim())).collect(Collectors.toList());

        gnuList.forEach(pair -> {
            if (list.stream().noneMatch(p -> p.getKey().equalsIgnoreCase(pair.getKey()))) {
                System.out.println(pair);
            } else {
                //System.out.println("= " + pair);
            }
        });
    }

    private static void saveCrc32() throws IOException {
        List<Pair<String, String>> list = Files.walk(PATH).filter(path -> Files.isRegularFile(path))
                .peek(System.out::println)
                .map(p -> new Pair<>(longToHex(BinaryUtils.crc32(p)), p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        Files.write(CRC32_PATH, list.stream().map(p -> p.getKey() + "\t" + p.getValue()).collect(Collectors.toList()));
    }

    private static String longToHex(long value) {
        StringBuilder hexString = new StringBuilder(Long.toHexString(value));
        while (hexString.length() != 8) {
            hexString.insert(0, "0");
        }
        return hexString.toString();
    }
}
