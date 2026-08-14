package org.zstack.utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class TypeUtils {
    private static List<Class> primitivesAndWrapperTypes = new ArrayList<Class>();
    private static List<Class> primitivesTypes = new ArrayList<Class>();
    private static Set<Class> numberClasses = new HashSet<>();

    static {
        primitivesTypes.add(Boolean.TYPE);
        primitivesTypes.add(Character.TYPE);
        primitivesTypes.add(Byte.TYPE);
        primitivesTypes.add(Short.TYPE);
        primitivesTypes.add(Integer.TYPE);
        primitivesTypes.add(Long.TYPE);
        primitivesTypes.add(Float.TYPE);
        primitivesTypes.add(Double.TYPE);
        primitivesTypes.add(Void.TYPE);

        primitivesAndWrapperTypes.add(Boolean.class);
        primitivesAndWrapperTypes.add(Character.class);
        primitivesAndWrapperTypes.add(Byte.class);
        primitivesAndWrapperTypes.add(Short.class);
        primitivesAndWrapperTypes.add(Integer.class);
        primitivesAndWrapperTypes.add(Long.class);
        primitivesAndWrapperTypes.add(Float.class);
        primitivesAndWrapperTypes.add(Double.class);
        primitivesAndWrapperTypes.add(Void.class);
        primitivesAndWrapperTypes.add(String.class);
        primitivesAndWrapperTypes.addAll(primitivesTypes);

        numberClasses.add(Short.TYPE);
        numberClasses.add(Short.class);
        numberClasses.add(Integer.TYPE);
        numberClasses.add(Integer.class);
        numberClasses.add(Long.TYPE);
        numberClasses.add(Long.class);
        numberClasses.add(Float.TYPE);
        numberClasses.add(Float.class);
        numberClasses.add(Double.TYPE);
        numberClasses.add(Double.class);
    }

    public static boolean isPrimitiveType(Class clazz) {
        return primitivesTypes.contains(clazz);
    }

    public static boolean isPrimitiveOrWrapper(Class clazz) {
        return primitivesAndWrapperTypes.contains(clazz);
    }

    public static boolean isTypeOf(Object val, Class...ts) {
        return isTypeOf(val.getClass(), ts);
    }

    public static boolean isTypeOf(Class t, Class...ts) {
        for (Class clz : ts) {
            if (clz.isAssignableFrom(t)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isZstackBeanPrimitive(Class clazz) {
        return isPrimitiveOrWrapper(clazz) || isTypeOf(clazz, Timestamp.class);
    }

    private static <T> T toValue(String val, Class<T> clazz) {
        if (numberClasses.contains(clazz)) {
            Double d = Double.valueOf(val);
            if (Integer.class.isAssignableFrom(clazz) || Integer.TYPE.isAssignableFrom(clazz)) {
                return (T) new Integer(d.intValue());
            } else if (Long.class.isAssignableFrom(clazz) || Long.TYPE.isAssignableFrom(clazz)) {
                return (T) new Long(d.longValue());
            } else  if (Float.class.isAssignableFrom(clazz) || Float.TYPE.isAssignableFrom(clazz)) {
                return (T) new Float(d.floatValue());
            } else if (Short.class.isAssignableFrom(clazz) || Short.TYPE.isAssignableFrom(clazz)) {
                return (T) new Short(d.shortValue());
            } else if (Double.class.isAssignableFrom(clazz) || Double.TYPE.isAssignableFrom(clazz)) {
                return (T) d;
            } else {
                throw new RuntimeException(String.format("not supported number class: %s", clazz));
            }
        } else if (String.class.isAssignableFrom(clazz)) {
            return (T) val;
        } else if (Boolean.class.isAssignableFrom(clazz) || Boolean.TYPE.isAssignableFrom(clazz)) {
            return (T) Boolean.valueOf(val);
        } else {
            return (T) val;
        }
    }

    public static <T> T stringToValue(String str, Class<T> clazz) {
        if (str == null && isTypeOf(clazz, Integer.class, Long.class, Float.class, Double.class, String.class)) {
            return null;
        }

        return toValue(str, clazz);
    }

    public static <T> T stringToValue(String str, Class<T> clazz, T defaultValue) {
        try {
            return stringToValue(str, clazz);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static List<Class> getAllClassOfClass(Class c) {
        List<Class> clzs = new ArrayList<Class>();
        while (c != Object.class) {
            clzs.add(c);
            c = c.getSuperclass();
        }
        return clzs;
    }

    public static List<Class> getAllClassOfObject(Object obj) {
        List<Class> ret = new ArrayList<Class>();
        Class clazz = obj.getClass();
        do {
            ret.add(clazz);
            clazz = clazz.getSuperclass();
        } while (clazz!=Object.class);
        ret.add(Object.class);
        return ret;
    }

    public static boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }
}
