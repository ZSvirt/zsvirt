package org.zstack.utils.test;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.zstack.utils.form.Form;
import org.zstack.utils.verify.Param;
import org.zstack.utils.verify.ParamValidator;
import org.zstack.utils.verify.Verifiable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class TestForm {
    public static class TestClass implements Verifiable {
        boolean aBoolean;
        int anInt = 23;
        float aFloat;
        double aDouble;
        long aLong;
        String value;

        public void setValue(String value) {
            this.value = value;
        }

        public TestClass(){

        }
    }

    /**
     * Excel:
     * | aBoolean   | anInt   | aFloat  | aDouble   | aLong | test  |
     * | true       | 2.3     | 3.3     |   4.3     | 5.3   |       |
     * | True       |         |         |           |       |  a,b,c|
     * @throws Exception
     */

    @Test
    public void test() throws Exception {
        testExcel();
        testCsv();
        testOtherCsv();
        testParam();
        testLimit();
    }

    private void testExcel() throws Exception {
        List<TestClass> results = Form.New(TestClass.class, createExcelContent(), 100)
                .addColumnConverter("test", it -> Arrays.asList(it.split(",")), TestClass::setValue)
                .withValidator(ParamValidator::validate)
                .load();
        assert results.size() == 4;
        assert results.get(0).aBoolean && results.get(1).aBoolean;
        assert results.get(0).anInt == 2;
        assert results.get(1).anInt == 23;
        assert results.get(1).value.equals("a");
    }

    private void testCsv() throws Exception {
        List<TestClass> results = Form.New(TestClass.class, createCsvContent(), 100)
                .addColumnConverter("test", it -> Arrays.asList(it.split(",")), TestClass::setValue)
                .withValidator(ParamValidator::validate)
                .load();
        assert results.size() == 4;
        assert results.get(0).aBoolean && results.get(1).aBoolean;
        assert results.get(0).anInt == 2;
        assert results.get(1).anInt == 23;
        assert results.get(1).value.equals("a");
    }

    private void testOtherCsv() throws Exception {
        List<TestClass> results = Form.New(TestClass.class, createOtherCsv(), 100)
                .addColumnConverter("test", it -> Arrays.asList(it.split("\\|")), TestClass::setValue)
                .withValidator(ParamValidator::validate)
                .load();
        assert results.size() == 4;
        assert results.get(0).aBoolean && results.get(1).aBoolean;
        assert results.get(0).anInt == 2;
        assert results.get(1).anInt == 23;
        assert results.get(1).value.equals("a");
    }

    private void testLimit() throws Exception {
        List<TestClass> results = Form.New(TestClass.class, createOtherCsv(), 4)
                .addColumnConverter("test", it -> Arrays.asList(it.split("\\|")), TestClass::setValue)
                .withValidator(ParamValidator::validate)
                .load();

        boolean called = false;
        try {
            Form.New(TestClass.class, createOtherCsv(), 3)
                    .addColumnConverter("test", it -> Arrays.asList(it.split("\\|")), TestClass::setValue)
                    .withValidator(ParamValidator::validate)
                    .load();
        } catch (Exception e) {
            called = true;
        }
        assert called;
    }

    public static class TestClass2 extends TestParentClass{
        @Param(noTrim = true)
        String noTrimValue;

        @Param(numberRange = {1,3})
        int number = 2;
    }

    public static class TestParentClass implements Verifiable {
        @Param
        String trimValue;
    }

    private void testParam() throws Exception {
        String csv = "trimValue,noTrimValue,number\n,aa,1";
        boolean failure = false;
        try {
            Form.New(TestClass2.class, encodeToBase64(csv), 100).withValidator(ParamValidator::validate).load();
        } catch (Exception e) {
            failure = true;
        }
        assert failure;

        csv = "trimValue,noTrimValue,number\naa,aa,4\n , , \n";
        failure = false;
        try {
            Form.New(TestClass2.class, encodeToBase64(csv), 100).withValidator(ParamValidator::validate).load();
        } catch (Exception e) {
            failure = true;
        }
        assert failure;

        csv = "trimValue,noTrimValue,number\naa , aa\n,,\n";
        List<TestClass2> results = Form.New(TestClass2.class, encodeToBase64(csv), 100).withValidator(ParamValidator::validate).load();
        assert results.get(0).trimValue.equals("aa");
        assert results.get(0).noTrimValue.equals(" aa");
    }

    private String createExcelContent(){
        Workbook workBook = new HSSFWorkbook();
        workBook.createSheet();
        Sheet sheet = workBook.getSheetAt(0);
        Row column = sheet.createRow(0);
        Row data1 = sheet.createRow(1);
        sheet.createRow(2);
        Row data2 = sheet.createRow(4);


        Field[] fields = TestClass.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            column.createCell(i, CellType.STRING).setCellValue(fields[i].getName());
        }

        column.getCell(5).setCellValue("test");

        data1.createCell(0, CellType.BOOLEAN).setCellValue(true);
        data2.createCell(0, CellType.STRING).setCellValue("True");
        data1.createCell(1, CellType.NUMERIC).setCellValue(2.3);
        data1.createCell(2, CellType.NUMERIC).setCellValue(3.3);
        data1.createCell(3, CellType.NUMERIC).setCellValue(4.3);
        data1.createCell(4, CellType.NUMERIC).setCellValue(5.3);
        data2.createCell(5, CellType.NUMERIC).setCellValue("a,b,c");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            workBook.write(os);
            byte[] bytes = os.toByteArray();
            os.close();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String createCsvContent() {
        String[][] origin = {
                {"aBoolean", "anInt", "aFloat", "aDouble", "aLong", "test"},
                {"true", "2.3", "3.3", "4.3", "5.3"},
                {"True","","","","","a,b,c"},
        };

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            OutputStreamWriter out =new OutputStreamWriter(os, Charset.forName("UTF-8"));
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.RFC4180);
            printer.printRecords(origin);
            out.close();

            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String createOtherCsv() {
        return encodeToBase64("aBoolean;anInt;aFloat;aDouble;aLong;test\ntrue;2.3;3.3;4.3;5.3\nTrue;;;;;a|b|c");
    }

    private String encodeToBase64(String origin){
        return Base64.getEncoder().encodeToString(origin.getBytes(Charset.forName("UTF-8")));
    }
}
