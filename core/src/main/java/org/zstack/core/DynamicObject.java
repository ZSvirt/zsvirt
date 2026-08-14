package org.zstack.core;

import org.zstack.header.exception.CloudRuntimeException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface DynamicObject {
    default <T> T getProperty(String propertyName) {
        DynamicObjectMetadata metadata = Platform.dynamicObjectMetadata.get(getClass());
        if (metadata == null) {
            throw new CloudRuntimeException(String.format("no metadata found for the DynamicObject[%s]", getClass()));
        }

        Field field = metadata.fields.get(propertyName);
        if (field == null) {
            throw new CloudRuntimeException(String.format("cannot find property[%s] on %s", propertyName, getClass()));
        }

        try {
            return (T) field.get(this);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }

    default boolean hasProperty(String name) {
        DynamicObjectMetadata metadata = Platform.dynamicObjectMetadata.get(getClass());
        if (metadata == null) {
            throw new CloudRuntimeException(String.format("no metadata found for the DynamicObject[%s]", getClass()));
        }

        return metadata.fields.containsKey(name);
    }

    default  <T> T invokeMethod(String name, Object...args) {
        DynamicObjectMetadata metadata = Platform.dynamicObjectMetadata.get(getClass());
        if (metadata == null) {
            throw new CloudRuntimeException(String.format("no metadata found for the DynamicObject[%s]", getClass()));
        }

        Method m = metadata.methods.get(name);
        if (m == null) {
            throw new CloudRuntimeException(String.format("cannot find method[%s] on %s", name, getClass()));
        }

        try {
            return (T) m.invoke(this, args);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    default void setProperty(String propertyName, Object newValue) {
        DynamicObjectMetadata metadata = Platform.dynamicObjectMetadata.get(getClass());
        if (metadata == null) {
            throw new CloudRuntimeException(String.format("no metadata found for the DynamicObject[%s]", getClass()));
        }

        Field field = metadata.fields.get(propertyName);
        if (field == null) {
            throw new CloudRuntimeException(String.format("cannot find property[%s] on %s", propertyName, getClass()));
        }

        try {
            field.set(this, newValue);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
