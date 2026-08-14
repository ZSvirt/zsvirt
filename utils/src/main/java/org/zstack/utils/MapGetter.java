package org.zstack.utils;

import java.util.*;

/**
 * Created by xing5 on 2017/6/15.
 */
public class MapGetter {
    private Map map;
    private String path;

    public MapGetter(Map map) {
        this.map = map;
    }

    private Object get(Map current, Iterator<String> it, String key, Class type) {
        if (!it.hasNext()) {
            Object ret = current.get(key);
            if (ret != null && !type.isAssignableFrom(ret.getClass())) {
                throw new RuntimeException(String.format("object[%s] is not of type[%s] but %s, path: %s", key, type, ret.getClass(), path));
            }

            return ret;
        }

        String p = it.next();
        Object c =  current.get(p);
        if (c != null && !(c instanceof Map)) {
            throw new RuntimeException(String.format("object[%s] is not of type Map but %s, path: %s", key, c.getClass(), path));
        } else if (c == null) {
            return null;
        } else {
            return get((Map) c, it, key, type);
        }
    }

    public <T> T get(String path, Class<T> type) {
        this.path = path;
        String[] paths = path.split("\\.");
        List<String> ps = new ArrayList<>();
        ps.addAll(Arrays.asList(paths).subList(0, paths.length - 1));
        String key = paths[paths.length-1];
        return (T) get(map, ps.iterator(), key, type);
    }
}
