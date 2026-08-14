package org.zstack.utils;

import org.zstack.utils.logging.CLogger;
import org.zstack.utils.serializable.SerializableHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 */
public class ObjectUtils {
    public static CLogger logger = Utils.getLogger(ObjectUtils.class);
    public static <T> T copy(T dst, Object src) {
        Class dstClass = dst.getClass();
        Class srcClass = null;
        Class clazz = src.getClass();
        do {
            if (clazz.isAssignableFrom(dstClass)) {
                srcClass = clazz;
                break;
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        if (srcClass == null) {
            throw new RuntimeException(String.format("%s is not assignable from %s", src.getClass().getName(), dstClass.getName()));
        }

        try {
            Map<String, Field> dstFields = new HashMap<String, Field>();
            for (Field f : FieldUtils.getAllFields(dstClass)) {
                dstFields.put(f.getName(), f);
            }

            for (Field f : FieldUtils.getAllFields(srcClass)) {
                f.setAccessible(true);
                Field dstf = dstFields.get(f.getName());
                dstf.setAccessible(true);
                dstf.set(dst, f.get(src));
            }

            return dst;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newAndCopy(Object src, Class<T> dstClass) {
        if (src == null) {
            return null;
        }

        try {
            T dst = dstClass.newInstance();
            org.springframework.beans.BeanUtils.copyProperties(src, dst);
            return dst;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T serializableCopy(T object) throws IOException, ClassNotFoundException {
        if(object == null){
            return null;
        }
        byte[] temp = SerializableHelper.writeObject(object);
        return SerializableHelper.readObject(temp);
    }

    /**
     * <pre>
     * getOrNull("123", String::length) = 3
     * getOrNull("", String::length) = 0
     * getOrNull(null, String::length) = null
     *
     * getOrNull(vm, VmInstanceVO::getUuid) = "2b0f9a804b654e4b8ca56301878a7d51"
     * getOrNull(null, VmInstanceVO::getUuid) = null
     * <pre/>
     */
    public static <O,T> T getOrNull(O object, Function<? super O, ? extends T> mapper) {
        return Optional.of(object).map(mapper).orElse(null);
    }
}
