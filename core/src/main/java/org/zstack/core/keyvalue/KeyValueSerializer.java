package org.zstack.core.keyvalue;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.FieldUtils.CollectionGenericType;
import org.zstack.utils.FieldUtils.MapGenericType;
import org.zstack.utils.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;

/**
 */
public class KeyValueSerializer {
    private List<KeyValueStruct> ret = new ArrayList<KeyValueStruct>();
    private Stack<String> trace = new Stack<String>();
    private Stack<Object> paths = new Stack<Object>();

    private boolean canDeserialize(Class type) {
        return KeyValueUtils.isPrimitiveTypeForKeyValue(type);
    }

    private Object getValue(Field f, Object obj) throws IllegalAccessException {
        f.setAccessible(true);
        return f.get(obj);
    }

    private void take(Field f, Object obj) throws IllegalAccessException {
        f.setAccessible(true);
        Object val = f.get(obj);
        ret.add(new KeyValueStruct(makePath(), val.toString(), val.getClass()));
    }

    private void take(Object obj) {
        ret.add(new KeyValueStruct(makePath(), obj.toString(), obj.getClass()));
    }

    private boolean isNullValue(Field f, Object obj) throws IllegalAccessException {
        return getValue(f, obj) == null;
    }

    private void buildObject(Object obj) throws IllegalAccessException {
        if (paths.contains(obj)) {
            paths.push(obj);
            throw new CloudRuntimeException(String.format("recursive object graph: %s", StringUtils.join(paths, " --> ")));
        }

        paths.push(obj);
        List<Field> fs = FieldUtils.getAllFields(obj.getClass());
        for (Field f : fs) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }

            Object val = getValue(f, obj);
            if (val == null) {
                continue;
            }

            if (canDeserialize(f.getType())) {
                trace.push(f.getName());
                take(f, obj);
                trace.pop();
                continue;
            }

            if (Map.class.isAssignableFrom(f.getType())) {
                buildMap(f, obj);
                continue;
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                buildList(f, obj);
                continue;
            }

            trace.push(f.getName());
            buildObject(val);
            trace.pop();
        }
        paths.pop();
    }

    private void buildList(Field f, Object obj) throws IllegalAccessException {
        CollectionGenericType type = (CollectionGenericType) FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
        DebugUtils.Assert(List.class.isAssignableFrom(f.getType()), String.format("Collection must be List, but %s is %s", makePath(), type.getValueType().getName()));
        DebugUtils.Assert(type.isInferred(), String.format("Collection must use Generic, %s is not", makePath()));
        if (isNullValue(f, obj)) {
            return;
        }

        Object value = getValue(f, obj);
        List col = (List) value;
        if (col.isEmpty()) {
            return;
        }

        if (paths.contains(value)) {
            paths.push(value);
            throw new CloudRuntimeException(String.format("recursive object graph: %s", StringUtils.join(paths, " --> ")));
        }

        paths.push(value);
        for (Object item : col) {
            String itemName = String.format("%s[%s]", f.getName(), col.indexOf(item));
            trace.push(itemName);
            if (canDeserialize(type.getValueType())) {
                take(item);
            } else {
                buildObject(item);
            }
            trace.pop();
        }

        paths.pop();
    }

    public List<KeyValueStruct> build(Object entity) {
        try {
            buildObject(entity);

            return ret;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private String makePath() {
        return StringUtils.join(trace, ".");
    }

    private void buildMap(Field f, Object obj) throws IllegalAccessException {
        MapGenericType type = (MapGenericType) FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
        DebugUtils.Assert(type.isInferred(), String.format("Map must use Generic where key is type of String and value is not type of Map or Collection"));
        DebugUtils.Assert(type.getKeyType() == String.class, String.format("Map must use String as key, but %s use %s", makePath(), type.getKeyType().getName()));
        DebugUtils.Assert(type.getNestedGenericValue() == null, String.format("Map cannot have nested map or collection, %s", makePath()));

        if (isNullValue(f, obj)) {
            return;
        }


        Object value = getValue(f, obj);
        Map map = (Map) value;
        if (map.isEmpty()) {
            return;
        }

        if (paths.contains(value)) {
            paths.push(value);
            throw new CloudRuntimeException(String.format("recursive object graph: %s", StringUtils.join(paths, " --> ")));
        }

        paths.push(obj);
        Iterator<Entry> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            String key = e.getKey().toString();
            Object item = e.getValue();
            String itemName = String.format("%s[\"%s\"]", f.getName(), key);
            trace.push(itemName);
            if (canDeserialize(type.getValueType())) {
                take(item);
            } else {
                buildObject(item);
            }
            trace.pop();

        }
        paths.pop();
    }
}
