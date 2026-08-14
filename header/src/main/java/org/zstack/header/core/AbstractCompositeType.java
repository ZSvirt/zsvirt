package org.zstack.header.core;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.FieldUtils;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public abstract class AbstractCompositeType implements CompositeData, Serializable {
    protected abstract String[] getFieldNames();

    public abstract CompositeType getCompositeType();

    @Override
    public Object get(String key) {
        Field f = FieldUtils.getField(key, this.getClass());
        if (f == null) {
            return null;
        }

        f.setAccessible(true);
        try {
            return f.get(this);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public Object[] getAll(String[] keys) {
        Object[] ret = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            ret[i] = get(keys[i]);
        }
        return ret;
    }

    @Override
    public boolean containsKey(String key) {
        String[] fieldNames = getFieldNames();
        for (String fname : fieldNames) {
            if (fname.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        Collection values = values();
        return values.contains(value);
    }

    @Override
    public Collection<?> values() {
        List<Object> ret = new ArrayList<Object>();
        for (String fname : getFieldNames()) {
            ret.add(get(fname));
        }
        return ret;
    }
}
