package org.zstack.utils.form;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;


public class CsvReader implements FormReader {
    private static List<CSVFormat> formats = new ArrayList<>();
    private String[] header;
    private Iterator<String[]> recordIterator;
    static {
        formats.add(CSVFormat.RFC4180);
        formats.add(CSVFormat.DEFAULT.withDelimiter(';'));
        formats.add(CSVFormat.TDF);
        formats.add(CSVFormat.DEFAULT.withDelimiter(':'));
    }

    public CsvReader(String base64Content) {
        String content = new String(Base64.getDecoder().decode(base64Content), Charset.forName("utf-8"));
        List<String[]> records = getRecords(content.replaceFirst("\\uFEFF", ""));
        recordIterator = records.iterator();
        header = recordIterator.next();
    }

    @Override
    public String getType() {
        return FormType.CSV.toString();
    }

    @Override
    public String[] getHeader() {
        return header;
    }

    @Override
    public String[] nextRecord() {
        if (!recordIterator.hasNext()) {
            return null;
        }
        return recordIterator.next();
    }

    private List<String[]> getRecords(String content) {
        List<String[]> records = new ArrayList<>();
        for (CSVFormat format : formats) {
            try {
                List<String[]> tempRecords = getRecords(content, format);
                if (tempRecords.size() <= 1) {
                    continue;
                }

                int length = tempRecords.get(0).length;
                boolean illegal = tempRecords.stream().anyMatch(it -> it.length > length);
                if (illegal) {
                    continue;
                }

                if (records.isEmpty() || tempRecords.get(0).length > records.get(0).length) {
                    records = tempRecords;
                }
            } catch (IOException ignored) {}
        }

        if (records.size() > 1 && records.get(0).length != 0) {
            return records;
        }

        throw new IllegalArgumentException("it is not a legal csv format");
    }



    private List<String[]> getRecords(String content, CSVFormat format) throws IOException {
        List<String[]> records = new ArrayList<>();
        CSVParser parser = CSVParser.parse(content, format);
        for (CSVRecord record : parser.getRecords()) {
            String[] line = new String[record.size()];
            for (int i = 0; i < line.length; i++) {
                line[i] = record.get(i);
            }
            records.add(line);
        }
        return records;
    }
}
