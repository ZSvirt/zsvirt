package org.zstack.utils.form;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.function.ValidateFunction;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class Form<T> {
    private final Class<T> clz;
    private String base64Content;

    private Map<String, ListConverter> columnListConverter = new HashMap<>();
    private Map<String, Consumer<T>> columnConsumer = new HashMap<>();
    private List<Converter> headerConverter = new ArrayList<>();
    private ValidateFunction<? super T> validator = null;
    private String[] columns;

    private final int limit;
    private int size = 0;

    public interface Consumer<T> {
        void accept(T item, String value) throws Exception;
    }

    public interface ListConverter {
        Collection<String> accept(String value);
    }

    public interface Converter {
        String accept(String column);
    }

    public static class OutOfLimitException extends IllegalArgumentException{
        public OutOfLimitException(String format) {
            super(format);
        }
    }

    private Form(Class<T> clz, String base64Content, int limit) {
        this.clz = clz;
        this.base64Content = base64Content;
        this.limit = limit;
    }

    public static <T> Form<T> New(Class<T> clazz, String base64Content, int limit) {
        return new Form<>(clazz, base64Content, limit);
    }

    public Form<T> addColumnConverter(String column, ListConverter listConverter, Consumer<T> consumer) {
        columnListConverter.put(column, listConverter);
        columnConsumer.put(column, consumer);
        return this;
    }

    public Form<T> addColumnConverter(String column, Consumer<T> consumer) {
        columnConsumer.put(column, consumer);
        return this;
    }

    public Form<T> addHeaderConverter(Converter converter) {
        headerConverter.add(converter);
        return this;
    }

    public Form<T> withValidator(ValidateFunction<? super T> validator) {
        this.validator = validator;
        return this;
    }

    public List<T> load() throws IOException {
        List<T> results = new ArrayList<>();

        FormReader reader = getFormReader(getReaderType());
        columns = converterColumn(reader.getHeader());
        if (columns == null || columns.length == 0) {
            return null;
        }
        produceDefaultColumnConsumer();

        int line = 1;
        String[] record;
        Map<Integer, String> lineErrorInfo = new TreeMap<>();
        while ((record = reader.nextRecord()) != null) {
            try {
                line++;
                results.addAll(loadObject(record));
            } catch (OutOfLimitException oe) {
                throw oe;
            } catch (Exception e) {
                lineErrorInfo.put(line, e.getMessage());
            }
        }

        if (!lineErrorInfo.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", lineErrorInfo.entrySet().stream()
                    .map(e -> String.format("line %d: %s", e.getKey(), e.getValue()))
                    .collect(Collectors.toList())));
        }

        return results;
    }

    private FormType getReaderType() throws IOException {
        return ExcelReader.checkType(base64Content) ? FormType.Excel : FormType.CSV;
    }

    private FormReader getFormReader(FormType type){
        if (type == FormType.CSV) {
            return new CsvReader(base64Content);
        } else if (type == FormType.Excel) {
            return new ExcelReader(base64Content);
        }

        throw new RuntimeException(String.format("not support form type: %s", type));
    }

    private class ExtendRecordLoader {
        List<T> objects = new ArrayList<>();
        String[][] extendRecord;

        ExtendRecordLoader(String[] originRecord) {
            this.extendRecord = extendRecord(originRecord);
        }

        private List<T> loadExtendRecord() throws Exception {
            stepLoadExtendRecord(new String[extendRecord.length], 0);
            return objects;
        }

        private void stepLoadExtendRecord(String[] finalRecord, int stepIndex) throws Exception {
            for (String value : extendRecord[stepIndex]) {
                finalRecord[stepIndex] = value;
                if (stepIndex == finalRecord.length - 1) {
                    objects.add(doLoadObject(finalRecord));
                } else {
                    stepLoadExtendRecord(finalRecord, stepIndex + 1);
                }
            }
        }

        private String[][] extendRecord(String[] originRecord){
            String[][] result = new String[originRecord.length][];
            for (int i = 0; i < originRecord.length; i++) {
                ListConverter listConverter = columnListConverter.get(columns[i]);
                if (listConverter != null && StringUtils.isNotEmpty(originRecord[i])) {
                    Collection<String> values = listConverter.accept(originRecord[i]);
                    result[i] = values.toArray(new String[values.size()]);
                } else {
                    result[i] = new String[] {originRecord[i]};
                }
            }
            return result;
        }
    }

    private List<T> loadObject(String[] record) throws Exception {
        if (record.length == 0 || Arrays.stream(record).allMatch(StringUtils::isBlank)) {
            return Collections.emptyList();
        } else if (columnListConverter.isEmpty()) {
            return Collections.singletonList(doLoadObject(record));
        } else {
            return new ExtendRecordLoader(record).loadExtendRecord();
        }
    }

    private T doLoadObject(String[] record) throws Exception {
        if (++size > limit) {
            throw new OutOfLimitException(String.format("objects count is too larger than limit[%d]", limit));
        }

        T object = clz.newInstance();
        for (int i = 0; i < record.length; i++) {
            String key = columns[i];
            String value = record[i];
            Consumer<T> consumer = columnConsumer.get(key);
            if (consumer != null && StringUtils.isNotBlank(value)) {
                consumer.accept(object, value);
            }
        }

        if (validator != null) {
            validator.validate(object);
        }
        return object;
    }

    private String[] converterColumn(String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            for (Converter converter : headerConverter) {
                columns[i] = converter.accept(columns[i]);
            }
        }
        return columns;
    }

    private void produceDefaultColumnConsumer(){
        List<String> cols = Arrays.asList(columns);
        for (Field field : FieldUtils.getAllFields(clz)) {
            if (columnConsumer.containsKey(field.getName()) || !cols.contains(field.getName())) {
                continue;
            }
            Consumer<T> setter = getDefaultSetter(field);
            Optional.ofNullable(setter).ifPresent(it -> columnConsumer.put(field.getName(), it));
        }
    }

    private Consumer<T> getDefaultSetter(Field f) {
        f.setAccessible(true);
        if (Integer.class.isAssignableFrom(f.getType()) || Integer.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value).intValue());
        } else if (Long.class.isAssignableFrom(f.getType()) || Long.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value).longValue());
        } else if (Float.class.isAssignableFrom(f.getType()) || Float.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value).floatValue());
        } else if (Double.class.isAssignableFrom(f.getType()) || Double.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value));
        } else if (String.class.isAssignableFrom(f.getType())) {
            return f::set;
        } else if (Boolean.class.isAssignableFrom(f.getType()) || Boolean.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Boolean.valueOf(value.toLowerCase()));
        } else {
            return null;
        }
    }
}
