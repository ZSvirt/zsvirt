package org.zstack.search;

import com.google.gson.Gson;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ESTuple {
    private static Gson gson;
    private Map<String, String> vals = new HashMap<String, String>();
    private final String[] fieldNames;

    static {
        gson = new GsonUtil().create();
    }

    ESTuple(String[] fieldNames) {
        super();
        this.fieldNames = fieldNames;
    }

    void put(String name, String val) {
        vals.put(name, val);
    }

    public <T> T get(String name, Class<T> clazz) {
        String val = vals.get(name);
        return val == null ? null : gson.fromJson(val, clazz);
    }

    public <T, K extends Collection> K get(String name, Class<K> collections, Class<T> clazz) {
        try {
            if (collections.isInterface()) {
                throw new IllegalArgumentException(String.format("collections must be a concrete class, not interface[%s]", collections.getName()));
            }
            String val = vals.get(name);
            if (val == null) {
                Collection c = collections.newInstance();
                return (K) c;
            }
            return (K) JSONObjectUtil.toCollection(val, collections, clazz);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    public <T, K extends Collection> K get(int i, Class<K> collections, Class<T> clazz) {
        if (i >= fieldNames.length) {
            throw new IllegalArgumentException(String.format("index[%s] must be lesser than number[%s] of fields selected in SearchQuery.select()", i,
                    fieldNames.length));
        }
        
        return get(fieldNames[i], collections, clazz);
    }

    public String get(String name) {
        return vals.get(name);
    }

    public String get(int i) {
        if (i >= fieldNames.length) {
            throw new IllegalArgumentException(String.format("index[%s] must be lesser than number[%s] of fields selected in SearchQuery.select()", i,
                    fieldNames.length));
        }

        return get(fieldNames[i]);
    }

    public <T> T get(int i, Class<T> clazz) {
        if (i >= fieldNames.length) {
            throw new IllegalArgumentException(String.format("index[%s] must be lesser than number[%s] of fields selected in SearchQuery.select()", i,
                    fieldNames.length));
        }

        String val = get(fieldNames[i]);
        return val == null ? null : (T) gson.fromJson(val, clazz);
    }
    
    public Map<String, String> getKeyValuePairs() {
        return vals;
    }
}
