package org.zstack.utils.gson;

import com.google.gson.*;
import org.json.JSONArray;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class JSONObjectUtil {
    private static final Gson gson;
    private static final Gson prettyGson;
    
    static {
        gson = new GsonBuilder().registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (jsonElement, type, jsonDeserializationContext) -> {
            Long l = jsonElement.getAsLong();
            if (l > Integer.MAX_VALUE) {
                throw new NumberFormatException(String.format("%d is Integer overflow, Integer.MAX_VALUE is %d", l, Integer.MAX_VALUE));
            } else {
                return l.intValue();
            }
        })
                .registerTypeAdapter(Class.class,
                        (JsonSerializer<Class<?>>) (aClass, type, context) -> (aClass == null) ? JsonNull.INSTANCE : new JsonPrimitive(aClass.getName()))
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .disableHtmlEscaping()
                .create();
        prettyGson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public static <T, K extends Collection> K toCollection(String content, Class<K> collections, Class<T> clazz) {
        try {
            if (collections.isInterface()) {
                throw new IllegalArgumentException(String.format("collections must be a concrete class, not interface[%s]", collections.getName()));
            }
            Collection c = collections.newInstance();
            JSONArray jarr = new JSONArray(content);
            for (int i=0; i<jarr.length(); i++) {
                String objstr = jarr.get(i).toString();
                if (String.class != clazz) {
                    c.add(gson.fromJson(objstr, clazz));
                } else {
                    c.add(objstr);
                }
            }
            return (K) c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T toObject(String content, Class<T> clazz){
        return gson.fromJson(content, clazz);
    }

    // Only supports converting content to List, for example: new TypeToken<List<T>>() {}.getType()
    public static <T> List<T> toList(String content, Type type){
        return gson.fromJson(content, type);
    }

    public static String toJsonString(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T rehashObject(Object obj, Class<T> clazz) {
        String str = toJsonString(obj);
        return toObject(str, clazz);
    }

    public static String toTypedJsonString(Object obj) {
        return toJsonString(map(e(obj.getClass().getName(), obj)));
    }

    public static <T> T fromTypedJsonString(String jstr) {
        LinkedHashMap map = toObject(jstr, LinkedHashMap.class);
        String className = (String) map.keySet().iterator().next();
        try {
            Class clz = Class.forName(className);
            return rehashObject(map.values().iterator().next(), (Class<T>) clz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String dumpPretty(Object obj) {
        return prettyGson.toJson(obj);
    }
}
