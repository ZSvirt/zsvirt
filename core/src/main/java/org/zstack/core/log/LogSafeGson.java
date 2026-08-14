package org.zstack.core.log;

import com.google.gson.*;
import org.apache.logging.log4j.util.Strings;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.JsonSchemaBuilder;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by MaJin on 2019/9/21.
 */
public class LogSafeGson {
    private static final CLogger logger = Utils.getLogger(LogSafeGson.class);

    /**
     * class and its need be masked fields
     */
    private static final Map<Class, Set<FieldNoLogging>> maskFields = new HashMap<>();
    /**
     * field with annotation @NoLogging(behavior = NoLogging.Behavior.Auto) will be collect to this map
     *
     * when serialize class with autoFields, the specific class will be found (for example, its super class) and
     * then execute serialize.
     *
     * this should be used for base abstractions
     */
    private static final Map<Class, Set<FieldNoLogging>> autoFields = new HashMap<>();

    /**
     * used for class implements Serializable or Message for potential sensitive check
     *
     * this is different from autoFields, because with NoLogging means there are some
     * sensitive fields so more extendable fields check could be used.
     *
     * all class not annotated with NoLogging will be store in potentialSensitiveFields.
     */
    private static final Map<Class, Set<FieldNoLogging>> potentialSensitiveFields = new HashMap<>();

    private static final List<Class<?>> searchClasses = Arrays.asList(Serializable.class, Message.class);
    private static final Gson logSafeGson;
    private static Function<String, String> tagInfoHider = s -> s;
    private static class FieldNoLogging {
        Field field;
        NoLogging annotation;
        Field classNameField;

        private static final Pattern uriPattern = Pattern.compile(":[^:]*@");

        FieldNoLogging(Field field) {
            this.field = field;
        }

        FieldNoLogging(Field field, NoLogging annotation, Class<?> senClz) {
            this.field = field;
            this.annotation = annotation;
            if (!Strings.isEmpty(annotation.classNameField())) {
                this.classNameField = FieldUtils.getField(annotation.classNameField(), senClz);
                if (this.classNameField != null) {
                    this.classNameField.setAccessible(true);
                }
            }
        }

        String getName() {
            return field.getName();
        }

        Object getValue(Object obj) {
            try {
                Object value = field.get(obj);
                if (classNameField == null) {
                    return value;
                }

                Class<?> clz = Class.forName((String) classNameField.get(obj));
                return mayHasSensitiveInfo(clz) ? JSONObjectUtil.toObject((String) value, clz) : value;
            } catch (IllegalAccessException | ClassNotFoundException e) {
                return null;
            }
        }

        String getMaskedValue(String raw) {
            if (annotation.type().simple()) {
                return "*****";
            } else if (annotation.type().tag()) {
                return tagInfoHider.call(raw);
            } else if (annotation.type().longText()) {
                if (raw == null)
                    return null;
                return (raw.length() < 20) ? raw : raw.substring(0, 10) + "..." + raw.substring(raw.length() - 10);
            } else {
                return Utils.getLogMaskWords().getOrDefault(raw, uriPattern.matcher(raw).replaceFirst(":*****@"));
            }
        }
    }

    static {
        GsonUtil util = new GsonUtil();
        searchClasses.forEach(it -> util.setInstanceCreator(it, getSerializer()));
        logSafeGson = util.setSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(GsonTransient.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).enableComplexMapKeySerialization().create();

        for (Class<?> baseClz : searchClasses) {
            for (Class<?> clz : BeanUtils.reflections.getSubTypesOf(baseClz)) {
                if (clz.isInterface()) {
                    continue;
                }

                cacheNoLoggingInfo(clz);
            }
        }
    }

    private static void cacheNoLoggingInfo(Class<?> si) {
        for (Field f : FieldUtils.getAllFields(si)) {
            NoLogging an = f.getAnnotation(NoLogging.class);
            if (an != null) {
                logger.trace(String.format("load @NoLogging annotated class: %s, fields: %s", si.getName(), f.getName()));

                f.setAccessible(true);
                if (an.behavior().auto()) {
                    autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f, an, si));
                } else {
                    maskFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f, an, si));
                }
            }

            if (f.getType().isEnum() || f.getType().isAssignableFrom(si)) {
                continue;
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                Class<?> genericType = FieldUtils.getGenericType(f);
                if (genericType == null || !mayHasSensitiveInfo(genericType)) {
                    continue;
                }
            } else if (!mayHasSensitiveInfo(f.getType())) {
                continue;
            }

            f.setAccessible(true);
            logger.trace(String.format("load potentially sensitive info contained class: %s, fields: %s", si.getName(), f.getName()));
            potentialSensitiveFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f));
        }
    }

    private static <T> JsonSerializer<T> getSerializer() {
        return (o, type, jsonSerializationContext) -> {
            JsonElement jsonElement = logSafeGson.toJsonTree(o);
            if (!jsonElement.isJsonObject()) {
                return jsonElement;
            }
            JsonObject jObj = jsonElement.getAsJsonObject();
            maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                Object obj = f.getValue(o);
                if (obj instanceof Collection) {
                    JsonArray array = new JsonArray();
                    ((Collection<?>) obj).forEach(v -> array.add(f.getMaskedValue(v.toString())));
                    jObj.add(f.getName(), array);
                } else if (obj != null) {
                    jObj.addProperty(f.getName(), f.getMaskedValue(obj.toString()));
                }
            });

            autoFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                Object si = f.getValue(o);
                if (mayHasSensitiveInfo(si)) {
                    jObj.add(f.getName(), toJsonElement(si));
                } else if (si instanceof Collection) {
                    JsonArray array = new JsonArray();
                    ((Collection<?>) si).forEach(v -> array.add(toJsonElement(v)));
                    jObj.add(f.getName(), array);
                }
            });

            potentialSensitiveFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                Object si = f.getValue(o);
                if (mayHasSensitiveInfo(si)) {
                    jObj.add(f.getName(), toJsonElement(si));
                } else if (si instanceof Collection) {
                    JsonArray array = new JsonArray();
                    ((Collection<?>) si).forEach(v -> array.add(toJsonElement(v)));
                    jObj.add(f.getName(), array);
                }
            });
            return jObj;
        };
    }

    public static JsonElement toJsonElement(Object o) {
        return logSafeGson.toJsonTree(o, getGsonType(o.getClass()));
    }

    public static String toJson(Object o) {
        return logSafeGson.toJson(o, getGsonType(o.getClass()));
    }

    public static boolean needMaskLog(Class clz) {
        return maskFields.containsKey(clz);
    }

    /**
     * only use maskFields and autoFields to check class
     * most likely contains sensitive info which is collected from
     * @NoLogging annotated classes.
     *
     * @param clz the class need check if contains any sensitive field
     * @return if the class has any field annotated by @NoLogging return false
     *         else return true
     */
    private static boolean mostLikelyCommonClass(Class clz) {
        return !maskFields.containsKey(clz) && !autoFields.containsKey(clz);
    }

    public static Message desensitize(Message o) {
        if (o == null || mostLikelyCommonClass(o.getClass())) {
            logger.trace(String.format("%s is not class annotated by @NoLogging, skip desensitize",
                    o != null ? o.getClass().getCanonicalName() : null));
            return o;
        }

        buildSchemaIfNeed(o);
        String retStr = toJson(o);
        Map raw = JSONObjectUtil.toObject(retStr, LinkedHashMap.class);
        Message result = JSONObjectUtil.toObject(retStr, o.getClass());

        try {
            result.restoreFromSchema(raw);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        logger.trace(String.format("object class: %s \ntransform sensitive object: %s \n to insensitive object: %s",
                o.getClass().getName(),
                JSONObjectUtil.toJsonString(o),
                JSONObjectUtil.toJsonString(result)));

        return result;
    }

    private static void buildSchemaIfNeed(Message msg) {
        if (msg.getHeaderEntry(CloudBus.HEADER_SCHEMA) != null) {
            return;
        }

        try {
            msg.putHeaderEntry(CloudBus.HEADER_SCHEMA, new JsonSchemaBuilder(msg).build());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public static Map<String, String> getValuesToMask(Object o) {
        Map<String, String> results = new HashMap<>();
        maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object obj = f.getValue(o);
            if (obj instanceof Collection) {
                ((Collection<?>) obj).forEach(v -> results.put(v.toString(), f.getMaskedValue(v.toString())));
            } else if (obj != null) {
                results.put(obj.toString(), f.getMaskedValue(obj.toString()));
            }
        });

        autoFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object si = f.getValue(o);
            if (mayHasSensitiveInfo(si)) {
                results.putAll(getValuesToMask(si));
            } else if (si instanceof Collection) {
                ((Collection<?>) si).forEach(v -> {
                    if (mayHasSensitiveInfo(v)) {
                        results.put(v.toString(), f.getMaskedValue(v.toString()));
                    }
                });
            }
        });

        potentialSensitiveFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object si = f.getValue(o);
            if (mayHasSensitiveInfo(si)) {
                results.putAll(getValuesToMask(si));
            } else if (si instanceof Collection) {
                ((Collection<?>) si).forEach(v -> {
                    if (mayHasSensitiveInfo(v)) {
                        results.put(v.toString(), f.getMaskedValue(v.toString()));
                    }
                });
            }
        });

        results.remove(Strings.EMPTY);
        return results;
    }

    public static Map<String, NoLogging.Type> getSensitiveFields(Class<?> clz) {
        Map<String, NoLogging.Type> paths = new HashMap<>();
        List<Field> fs = FieldUtils.getAllFields(clz);
        for (Field f : fs) {
            NoLogging an = f.getAnnotation(NoLogging.class);
            if (an != null && an.type() == NoLogging.Type.Simple) {
                paths.put(f.getName(), NoLogging.Type.Simple);
            } else if (mayHasSensitiveInfo(f.getType()) && !f.getType().isEnum() && !f.getType().isAssignableFrom(clz)) {
                String path = f.getName();
                getSensitiveFields(f.getType()).forEach((k, v) -> paths.put(path + '.' + k, v));
            }
        }

        return paths;
    }

    public static void registryTagHider(Function<String, String> hider) {
        tagInfoHider = hider;
    }

    private static boolean mayHasSensitiveInfo(Class<?> clz) {
        Package p = clz.getPackage();
        return p != null && p.getName().startsWith("org.zstack") && searchClasses.stream().anyMatch(it -> it.isAssignableFrom(clz));
    }

    private static boolean mayHasSensitiveInfo(Object obj) {
        return obj != null && mayHasSensitiveInfo(obj.getClass());
    }

    private static Class<?> getGsonType(Class<?> clz) {
        return searchClasses.stream().filter(it -> it.isAssignableFrom(clz)).findFirst().orElse(Object.class);
    }
}
