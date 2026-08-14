package org.zstack.utils;

import com.googlecode.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public class FieldUtils {
    public static <T> T getFieldValue(String name, Object obj) {
        Field f = getField(name, obj.getClass());
        if (f == null) {
            return null;
        }

        try {
            f.setAccessible(true);
            Object val = f.get(obj);
            return (T)val;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasField(String name, Class clazz) {
        return getField(name, clazz) != null;
    }

    public static Field getField(String name, Class<?> clazz) {
        do {
            try {
                Field f = clazz.getDeclaredField(name);
                return f;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            }
        } while (clazz != Object.class);
        
        return null;
    }
    
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        do {
            Field[] fs = clazz.getDeclaredFields();
            Collections.addAll(fields, fs);
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return fields;
            
    }

    public static List<Field> getDeclaringClassFields(Class<?> fieldDeclaringClass, Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        do {
            Field[] fs = clazz.getDeclaredFields();
            for (Field f : fs) {
                if (fieldDeclaringClass.isAssignableFrom(f.getType())) {
                    fields.add(f);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return fields;
    }

    public static Field getAnnotatedField(Class annotation, Class clazz) {
        do {
            Field[] fs = clazz.getDeclaredFields();
            for (Field f : fs) {
                if (f.isAnnotationPresent(annotation)) {
                    return f;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        return null;
    }

    public static Field getAnnotatedFieldOfThisClass(Class annotation, Class clazz) {
        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            if (f.isAnnotationPresent(annotation)) {
                return f;
            }
        }
        return null;
    }

    public static List<Class> getAllSuperClasses(Class self, Class stopBy) {
        if (stopBy == null) {
            stopBy = Object.class;
        }

        if (!stopBy.isAssignableFrom(self)) {
            throw new RuntimeException(String.format("class[%s] is not ancient class of class[%s]", stopBy.getName(), self.getName()));
        }

        List<Class> ret = new ArrayList<Class>();
        while (self != stopBy) {
            self = self.getSuperclass();
            ret.add(self);
        }
        return ret;
    }

    public static List<Class> getAllSuperClasses(Class self) {
        return getAllSuperClasses(self, null);
    }


    public static List<Field> getTypeAnnotatedFields(Class annotation, Class clazz) {
        List<Field> ret = new ArrayList<Field>();
        do {
            Field[] fs = clazz.getDeclaredFields();
            for (Field f : fs) {
                if (f.getType().isAnnotationPresent(annotation)) {
                    ret.add(f);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        return ret;
    }

    public static List<Field> getTypeAnnotatedFieldsOnThisClass(Class annotation, Class clazz) {
        List<Field> ret = new ArrayList<Field>();
        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            if (f.getType().isAnnotationPresent(annotation)) {
                ret.add(f);
            }
        }

        return ret;
    }

    public static List<Field> getAnnotatedFields(Class annotation, Class clazz) {
        List<Field> ret = new ArrayList<Field>();
        do {
            Field[] fs = clazz.getDeclaredFields();
            for (Field f : fs) {
                if (f.isAnnotationPresent(annotation)) {
                    ret.add(f);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        return ret;
    }

    public static <T extends Annotation> List<Field> getAnnotatedFieldsWithFilter(Class<T> annotation, Class clazz, Function<T, Boolean> filter) {
        List<Field> ret = new ArrayList<Field>();
        do {
            Field[] fs = clazz.getDeclaredFields();
            for (Field f : fs) {
                if (f.isAnnotationPresent(annotation) && filter.apply(f.getAnnotation(annotation))) {
                    ret.add(f);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        return ret;
    }

    public static List<Field> getAnnotatedFieldsOnThisClass(Class annotation, Class clazz) {
        List<Field> ret = new ArrayList<Field>();
        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            if (f.isAnnotationPresent(annotation)) {
                ret.add(f);
            }
        }

        return ret;
    }

    public static Class getGenericType(Field field) {
        Class type = field.getType();
        if (!Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format("only Collection and Map can get generic type at runtime, field[name:%s] is %s",
                    field.getName(), type.getName()));
        }

        try {
            Type gtype = field.getGenericType();
            if (!(gtype instanceof ParameterizedType)) {
                return null;
            }

            Type[] gtypes =  ((ParameterizedType)gtype).getActualTypeArguments();
            if (gtypes.length == 0) {
                return null;
            }

            Type ret = null;
            if (Collection.class.isAssignableFrom(type)) {
                ret = gtypes[0];
            } else {
                ret = gtypes[1];
            }

            if (ret instanceof Class) {
                return (Class) ret;
            } else {
                return null;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static GenericType inferMap(Type type) {
        MapGenericType ret = new MapGenericType();
        Type keyType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[0]);
        Type valueType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);
        ret.isInferred = keyType != null && valueType != null;
        if (keyType == null || valueType == null) {
            return ret;
        }

        ret.keyType = GenericTypeReflector.erase(keyType);
        ret.valueType = GenericTypeReflector.erase(valueType);
        if (!ret.isInferred) {
            return ret;
        }

        if (Map.class.isAssignableFrom(ret.keyType)) {
            ret.nestedGenericKey = inferMap(keyType);
        } else if (Collection.class.isAssignableFrom(ret.keyType)) {
            ret.nestedGenericKey = inferCollection(keyType);
        }

        if (Map.class.isAssignableFrom(ret.valueType)) {
            ret.nestedGenericValue = inferMap(valueType);
        } else if (Collection.class.isAssignableFrom(ret.valueType)) {
            ret.nestedGenericValue = inferCollection(valueType);
        }

        return ret;
    }

    private static GenericType inferCollection(Type type) {
        CollectionGenericType ret = new CollectionGenericType();
        Type valueType = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
        ret.isInferred = valueType != null;
        if (valueType == null) {
            return ret;
        }

        ret.valueType = GenericTypeReflector.erase(valueType);
        if (Map.class.isAssignableFrom(ret.valueType)) {
            ret.nestedGenericValue = inferMap(valueType);
        } else if (Collection.class.isAssignableFrom(ret.valueType)) {
            ret.nestedGenericValue = inferCollection(valueType);
        }
        return ret;
    }

    public static GenericType inferGenericTypeOnMapOrCollectionField(Field field) {
        Class owner = field.getDeclaringClass();
        Type t = GenericTypeReflector.getExactFieldType(field, owner);
        if (Map.class.isAssignableFrom(field.getType())) {
            return inferMap(t);
        } else if (Collection.class.isAssignableFrom(field.getType())) {
            return inferCollection(t);
        } else {
            throw new IllegalArgumentException(String.format("field is type of %s, only Map or Collection field can be inferred", field.getType().getName()));
        }
    }

    public static interface GenericType {
        boolean isMap();
        boolean isCollection();
        boolean isInferred();
        <T> T cast();
    }

    public static class MapGenericType implements GenericType {
        private boolean isInferred;
        private Class keyType;
        private Class valueType;
        private GenericType nestedGenericValue;
        private GenericType nestedGenericKey;

        public Class getKeyType() {
            return keyType;
        }

        public void setKeyType(Class keyType) {
            this.keyType = keyType;
        }

        public Class getValueType() {
            return valueType;
        }

        public void setValueType(Class valueType) {
            this.valueType = valueType;
        }

        public GenericType getNestedGenericValue() {
            return nestedGenericValue;
        }

        public void setNestedGenericValue(GenericType nestedGenericValue) {
            this.nestedGenericValue = nestedGenericValue;
        }

        public GenericType getNestedGenericKey() {
            return nestedGenericKey;
        }

        public void setNestedGenericKey(GenericType nestedGenericKey) {
            this.nestedGenericKey = nestedGenericKey;
        }

        @Override
        public boolean isMap() {
            return true;
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public boolean isInferred() {
            return isInferred;
        }

        @Override
        public <T> T cast() {
            return (T)this;
        }

        public void setInferred(boolean isInferred) {
            this.isInferred = isInferred;
        }
    }

    public static class CollectionGenericType implements GenericType {
        private Class valueType;
        private boolean isInferred;
        private GenericType nestedGenericValue;

        public Class getValueType() {
            return valueType;
        }

        public void setValueType(Class valueType) {
            this.valueType = valueType;
        }

        @Override
        public boolean isMap() {
            return false;
        }

        @Override
        public boolean isCollection() {
            return true;
        }

        @Override
        public boolean isInferred() {
            return isInferred;
        }

        @Override
        public <T> T cast() {
            return (T)this;
        }

        public void setInferred(boolean isInferred) {
            this.isInferred = isInferred;
        }

        public GenericType getNestedGenericValue() {
            return nestedGenericValue;
        }

        public void setNestedGenericValue(GenericType nestedGenericValue) {
            this.nestedGenericValue = nestedGenericValue;
        }
    }
}
