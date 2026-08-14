package org.zstack.utils;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class GroovyUtils {
    private static Map<String, Class> groovyClasses = new ConcurrentHashMap<>();

    public static <T> T newInstance(String scriptPath, ClassLoader parent) {
        try {
            Class clz = getClass(scriptPath, parent);
            return (T)clz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newInstance(String scriptPath) {
        return newInstance(scriptPath, GroovyUtils.class.getClassLoader());
    }

    public static <T> T newInstance(String scriptPath, Object...params) {
        Class clz = getClass(scriptPath);
        try {

            if (params.length != 0) {
                List<Class> clzs = new ArrayList<>();
                for (Object param : params) {
                    clzs.add(param.getClass());
                }

                return (T) clz.getConstructor(clzs.toArray(new Class[clzs.size()])).newInstance(params);
            } else {
                return newInstance(scriptPath);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getClass(String scriptPath) {
        return getClass(scriptPath, GroovyUtils.class.getClassLoader());
    }

    public static Class getClass(String scriptPath, ClassLoader parent) {
        Class clz = groovyClasses.get(scriptPath);
        if (clz != null) {
            return clz;
        }

        try (GroovyClassLoader loader = new GroovyClassLoader(parent)) {
            InputStream in = parent.getResourceAsStream(scriptPath);
            String script = StringDSL.inputStreamToString(in);
            clz = loader.parseClass(script);
        } catch (IOException ignore) {}
        groovyClasses.put(scriptPath, clz);
        return clz;
    }
}
