package org.zstack.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 */
public class MapDSL {
    public static  <T> T findValue(Map target, String key) {
        Iterator<Entry> it = target.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = it.next();
            Object value = entry.getValue();
            if (!entry.getKey().equals(key)) {
                if (Map.class.isAssignableFrom(value.getClass())) {
                    Object ret = findValue((Map)value, key);
                    if (ret != null) {
                        return (T) ret;
                    }
                }
            } else {
                return (T) value;
            }
        }

        return null;
    }
}
