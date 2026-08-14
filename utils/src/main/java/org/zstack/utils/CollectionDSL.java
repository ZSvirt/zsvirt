package org.zstack.utils;


import java.util.*;

/**
 */
public class CollectionDSL {
    public static <K,V> Map<K,V> map(Map.Entry<K,V>... entries) {
        Map<K,V> map = new HashMap<>();
        for(Map.Entry<K,V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public static <K,V> Map.Entry<K,V> e(final K k, final V v) {
        return new Map.Entry<K, V>() {
            public K getKey() {
                return k;
            }
            public V getValue() {
                return v;
            }
            public V setValue(V value) {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    public static <T> List<T> list(T...els) {
        ArrayList<T> lst = new ArrayList<>(els.length);
        Collections.addAll(lst, els);
        return lst;
    }

    public static <T> List<T> lists(List<T> list, T...els) {
        ArrayList<T> lst = new ArrayList<>(els.length + list.size());
        lst.addAll(list);
        Collections.addAll(lst, els);
        return lst;
    }

    public static <T> List<T> concat(List<T> list, List<T> list2) {
        ArrayList<T> lst = new ArrayList<>(list2.size() + list.size());
        lst.addAll(list);
        lst.addAll(list2);
        return lst;
    }
}
