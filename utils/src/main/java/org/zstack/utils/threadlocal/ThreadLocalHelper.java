package org.zstack.utils.threadlocal;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalHelper {
    private static ThreadLocal<Map<String, Object>> _local = new ThreadLocal<Map<String, Object>>();
    
    public static <T> void set(String key, T value) {
       Map<String, Object> map = _local.get(); 
       if (map == null) {
          map = new HashMap<String, Object>(1); 
          _local.set(map);
       }
       map.put(key, value);
    }
    
    public static <T> T get(String key) {
       Map<String, Object> map = _local.get();
       if (map == null) {
           return null;
       } else {
           return (T) map.get(key);
       }
    }
}
