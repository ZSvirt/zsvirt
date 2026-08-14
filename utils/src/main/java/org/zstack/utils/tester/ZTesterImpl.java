package org.zstack.utils.tester;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mingjian.deng on 2019/1/3.
 */
public class ZTesterImpl implements ZTester {
    private static Map<String, Object> testMap = new ConcurrentHashMap<>();

    @Override
    public Object get(String key) {
        return testMap.get(key);
    }

    @Override
    public <T> T get(String key, T defaultValue, Class<T> clazz) {
        Object v = testMap.get(key);
        if (v == null) {
            return defaultValue;
        } else if (v.getClass().isAssignableFrom(String.class) && v.toString().equals(ZTester.NULL_Flag)) {
            return null;
        } else {
            if (v.getClass().isAssignableFrom(clazz)) {
                return (T)v;
            } else {
                throw new RuntimeException(String.format("need type: %s, but got: %s. [key: %s, value: %s]",
                        clazz.getName(), v.getClass().getName(), key, JSONObjectUtil.toJsonString(v)));
            }
        }
    }

    @Override
    public void set(String key, Object value) {
        testMap.put(key, value);
    }

    @Override
    public void setNull(String key) {
        testMap.put(key, NULL_Flag);
    }

    @Override
    public void clearAll() {
        testMap.clear();
    }

    @Override
    public void clear(String key) {
        testMap.remove(key);
    }

    public static ZTester getTester() {
        //initialize();
        return new ZTesterImpl();
    }
}
