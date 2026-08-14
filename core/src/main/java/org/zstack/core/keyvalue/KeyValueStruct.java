package org.zstack.core.keyvalue;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.TypeUtils;

import java.sql.Timestamp;
import java.util.Date;

/**
 */
public class KeyValueStruct {
    private String key;
    private String value;
    private Class type;

    public KeyValueStruct(String key, String value, Class type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public Object toValue() {
        if (TypeUtils.isPrimitiveOrWrapper(type)) {
            return TypeUtils.stringToValue(value, type);
        }

        if (Date.class.isAssignableFrom(type)) {
            return new Date(value);
        }

        if (Timestamp.class.isAssignableFrom(type)) {
            return new Timestamp(new Date(value).getTime());
        }

        throw new CloudRuntimeException(String.format("unknown type[%s]", type.getName()));
    }

    @Override
    public String toString() {
        return String.format("%s: %s [%s]", key, value, type.getName());
    }
}
