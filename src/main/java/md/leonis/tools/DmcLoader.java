package md.leonis.tools;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class DmcLoader {

    public static List<Dmc> loadDmc() {
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = csvMapper.schemaFor(Dmc.class).withColumnSeparator(',');

        ObjectReader oReader = csvMapper.readerFor(Dmc.class).with(schema);

        List<Dmc> dmcColors = new ArrayList<>();

        try (Reader reader = new FileReader("C:\\Users\\user\\Downloads\\my\\diy\\palettes\\csv\\dmc-floss.csv")) {
            MappingIterator<Dmc> mi = oReader.readValues(reader);
            while (mi.hasNext()) {
                dmcColors.add(mi.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dmcColors;
    }
}
