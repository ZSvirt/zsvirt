package org.zstack.sdk;

import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/12/9.
 */
public class ApiResult {
    public ErrorCode error;
    private String resultString;
    private static final Logger logger = Logger.getLogger(ApiResult.class.getName());

    public ErrorCode getError() {
        return error;
    }

    void setError(ErrorCode error) {
        this.error = error;
    }

    void setResultString(String resultString) {
        this.resultString = resultString;
    }

    private static Object getProperty(Object bean, Iterator<String> it) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String path = it.next();
        if (bean instanceof Map) {
            Pattern re = Pattern.compile("(.*)\\[(\\d+)]");
            Matcher m = re.matcher(path);
            if (m.find()) {
                path = String.format("(%s)[%s]", m.group(1), m.group(2));
            }
        }

        try {
            Object val = PropertyUtils.getProperty(bean, path);

            if (it.hasNext()) {
                return getProperty(val, it);
            } else {
                return val;
            }
        } catch (NoSuchMethodException e) {
            logger.log(Level.WARNING, "Warning: NoSuchMethodException occurred. Details: ", e.getMessage());
            return null;
        }
    }

    public static Object getProperty(Object bean, String path) {
        List<String> paths = asList(path.split("\\."));
        try {
            return getProperty(bean, paths.iterator());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setProperty(Object bean, Iterator<String> it, String fieldName, Object val) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (it.hasNext()) {
            bean = getProperty(bean, it);
        }

        if (bean instanceof Map) {
            Pattern re = Pattern.compile("(.*)\\[(\\d+)]");
            Matcher m = re.matcher(fieldName);
            if (m.find()) {
                fieldName = String.format("(%s)[%s]", m.group(1), m.group(2));
            }
        }

        PropertyUtils.setProperty(bean, fieldName, val);
    }

    public static void setProperty(Object bean, String path, Object val) {
        List<String> paths = asList(path.split("\\."));
        String fieldName = paths.get(paths.size()-1);
        paths = paths.subList(0, paths.size()-1);

        try {
            setProperty(bean, paths.iterator(), fieldName, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public  <T> T getResult(Class<T> clz) {
        if (resultString == null || resultString.isEmpty()) {
            return null;
        }

        Map m = ZSClient.gson.fromJson(resultString, LinkedHashMap.class);
        T ret = ZSClient.gson.fromJson(resultString, clz);
        if (!m.containsKey("schema")) {
            return ret;
        }

        Map<String, String> schema = (Map) m.get("schema");
        try {
            for (String path : schema.keySet()) {
                String src = schema.get(path);
                String dst = org.zstack.sdk.SourceClassMap.srcToDstMapping.get(src);

                if (dst == null) {
                    //TODO: warning
                    continue;
                }

                Object bean = getProperty(ret, path);
                if (bean.getClass().getName().equals(dst)) {
                    // not an inherent object
                    continue;
                }

                Class dstClz = Class.forName(dst);
                Object source = getProperty(m, path);
                Object dstBean = ZSClient.gson.fromJson(ZSClient.gson.toJson(source), dstClz);
                setProperty(ret, path, dstBean);
            }

            return ret;
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }
}
