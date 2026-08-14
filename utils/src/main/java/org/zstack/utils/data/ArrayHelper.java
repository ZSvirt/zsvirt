package org.zstack.utils.data;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

import static org.zstack.utils.ObjectUtils.serializableCopy;

public class ArrayHelper {
    public static <T, K> T[] arrayFromField(Collection<K> c, String fieldName, Class<T> returnClassType) {
        try {
            List<T> lst = new ArrayList<T>();
            for (Enumeration e = Collections.enumeration(c); e.hasMoreElements();) {
                K obj = (K) e.nextElement();
                Class ck = obj.getClass();
                Field f = ck.getDeclaredField(fieldName);
                f.setAccessible(true);
                lst.add((T) f.get(obj));
            }
            return lst.toArray((T[]) Array.newInstance(returnClassType, lst.size()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to extract field[%s] from collection[%s] to array of type[%s]", fieldName, c.toString(),
                    returnClassType.getName()));
        }
    }

    public static <T, K> T[] arrayFromField(K[] c, String fieldName, Class<T> returnClassType) {
        try {
            List<T> lst = new ArrayList<T>();
            for (K k : c) {
                Class ck = k.getClass();
                Field f = ck.getDeclaredField(fieldName);
                f.setAccessible(true);
                lst.add((T) f.get(k));
            }
            return lst.toArray((T[]) Array.newInstance(returnClassType, lst.size()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to extract field[%s] from array[%s] to array of type[%s]", fieldName, Arrays.toString(c),
                    returnClassType.getName()));
        }
    }

    public static <T> List<T> serializableCopyList(List<T> sourceList) throws IOException, ClassNotFoundException {
        if(sourceList == null){
            return null;
        }
        List<T> copyList = new ArrayList<>();
        for(T o : sourceList){
            copyList.add(serializableCopy(o));
        }
        return copyList;
    }
}
