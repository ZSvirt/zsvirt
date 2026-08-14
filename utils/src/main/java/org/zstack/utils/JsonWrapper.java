package org.zstack.utils;

import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;

public class JsonWrapper<T> implements Serializable {
    private String content;
    private String className;
    
    public JsonWrapper(T obj) {
        content = JSONObjectUtil.toJsonString(obj);
        className = obj.getClass().getName();
    }
    
    public T get() {
        Class<T> clazz;
        try {
            clazz = (Class<T>) Class.forName(className);
            return JSONObjectUtil.toObject(content, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> JsonWrapper<T> wrap(T obj) {
        return new JsonWrapper<T>(obj);
    }
}
