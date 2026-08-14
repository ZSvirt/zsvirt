package org.zstack.utils.form;

import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;
import org.apache.poi.poifs.storage.HeaderBlockConstants;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LongField;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

public class ExcelReader implements FormReader {
    private Sheet sheet;
    private String[] header;

    private int readingRowIndex;
    public ExcelReader(String base64Content) {
        byte[] decoded = Base64.getDecoder().decode(base64Content);
        InputStream input = new ByteArrayInputStream(decoded);

        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                workbook.createSheet();
            }

            sheet = workbook.getSheetAt(0);
            header = sheet.getPhysicalNumberOfRows() == 0 ? null : readRow(0);
        } catch (IOException  e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean checkType(String base64Content) throws IOException {
        byte[] decoded = Base64.getDecoder().decode(base64Content);
        InputStream inp = new ByteArrayInputStream(decoded);
         return hasPOIFSHeader(IOUtils.peekFirst8Bytes(inp)) || DocumentFactoryHelper.hasOOXMLHeader(inp);
    }

    public static boolean hasPOIFSHeader(byte[] header8Bytes) {
        LongField signature = new LongField(HeaderBlockConstants._signature_offset, header8Bytes);
        return (signature.get() == HeaderBlockConstants._signature);
    }


    @Override
    public String getType() {
        return FormType.Excel.toString();
    }

    @Override
    public String[] getHeader() {
        return header;
    }

    @Override
    public String[] nextRecord() {
        if (readingRowIndex == sheet.getLastRowNum()) {
            return null;
        }
        return readRow(++readingRowIndex);
    }

    private String[] readRow(int index) {
        Row row = sheet.getRow(index);
        if (row == null || row.getPhysicalNumberOfCells() == 0) {
            return new String[0];
        }

        String[] record = new String[row.getLastCellNum()];
        for (int i = 0; i < record.length; i++) {
            record[i] =  Optional.ofNullable(row.getCell(i)).map(Object::toString).orElse(null);
        }
        return record;
    }
}
