package org.zstack.core.rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.*;

public class RESTApiJsonTemplateGenerator {
    private static final CLogger logger = Utils.getLogger(RESTApiJsonTemplateGenerator.class);
    private static List<Class<?>> primitiveTypes = new ArrayList<Class<?>>(10);

    static {
        primitiveTypes.add(int.class);
        primitiveTypes.add(long.class);
        primitiveTypes.add(boolean.class);
        primitiveTypes.add(double.class);
        primitiveTypes.add(float.class);
        primitiveTypes.add(Integer.class);
        primitiveTypes.add(Long.class);
        primitiveTypes.add(Boolean.class);
        primitiveTypes.add(Float.class);
        primitiveTypes.add(Double.class);
        primitiveTypes.add(String.class);
        primitiveTypes.add(Date.class);
        primitiveTypes.add(Timestamp.class);
    }

    private static boolean isPrimitiveType(Class<?> type) {
        return primitiveTypes.contains(type);
    }

    private static JSONObject nj() {
        return new JSONObject();
    }

    private static String populateTemplateString(Field f) {
        APIParam at = f.getAnnotation(APIParam.class);
        if (at != null && at.required()) {
            return String.format("mandatory, %s", f.getType().getName());
        }
        return f.getType().getName();
    }

    private static JSONArray dumpCollection(ParameterizedType pt, Stack<Class> hasDone) throws JSONException {
        Type st = pt.getActualTypeArguments()[0];
        JSONArray arr = new JSONArray();
        if (st instanceof ParameterizedType) {
            ParameterizedType spt = (ParameterizedType) st;
            if (Collection.class.isAssignableFrom((Class<?>) spt.getRawType())) {
                JSONArray oarr = dumpCollection(spt, hasDone);
                arr.put(oarr);
            } else if (Map.class.isAssignableFrom((Class<?>) spt.getRawType())) {
                JSONObject moj = dumpMap(spt, hasDone);
                arr.put(moj);
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "nested generic type[%s] in type[%s], your structure is too complicated to dump, you have to write json template yourself! Again, why do you need so complicated structure??",
                                spt.getRawType(), pt.getRawType()));
            }
        } else {
            Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
            if (Collection.class.isAssignableFrom(elementType) || Map.class.isAssignableFrom(elementType)) {
                throw new IllegalArgumentException(
                        String.format(
                                "nested type[%s] in generic type[%s], the nested type has no generic type information, your structure is too complicated to dump, you have to write json template yourself! Again, why do you need so complicated structure??",
                                elementType.getName(), pt.getRawType()));
            }

            if (isPrimitiveType(elementType)) {
                arr.put(elementType.getName());
            } else {
                JSONObject oj = dumpObject(elementType, hasDone);
                arr.put(oj);
            }
        }

        return arr;
    }

    private static JSONObject dumpMap(ParameterizedType pt, Stack<Class> hasDone) throws JSONException {
        JSONObject oj = nj();
        Type st = pt.getActualTypeArguments()[1];
        if (st instanceof ParameterizedType) {
            ParameterizedType spt = (ParameterizedType) st;
            if (Collection.class.isAssignableFrom((Class<?>) spt.getRawType())) {
                JSONArray oarr = dumpCollection(spt, hasDone);
                oj.put(String.class.getName(), oarr);
            } else if (Map.class.isAssignableFrom((Class<?>) spt.getRawType())) {
                JSONObject moj = dumpMap(spt, hasDone);
                oj.put(String.class.getName(), moj);
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "nested generic type[%s] in type[%s], your structure is too complicated to dump, you have to write json template yourself! Again, why do you need so complicated structure??",
                                spt.getRawType(), pt.getRawType()));
            }
        } else {
            Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[1];
            if (Collection.class.isAssignableFrom(elementType) || Map.class.isAssignableFrom(elementType)) {
                throw new IllegalArgumentException(
                        String.format(
                                "nested type[%s] in generic type[%s], the nested type has no generic type information, your structure is too complicated to dump, you have to write json template yourself! Again, why do you need so complicated structure??",
                                elementType.getName(), pt.getRawType()));
            }
            if (isPrimitiveType(elementType)) {
                oj.put(String.class.getName(), elementType.getName());
            } else {
                JSONObject mj = dumpObject(elementType, hasDone);
                oj.put(String.class.getName(), mj);
            }
        }
        return oj;
    }

    private static JSONObject dumpObject(Class<?> clazz, Stack<Class> hasDone) throws JSONException {
        JSONObject jo = nj();
        if (hasDone.contains(clazz)) {
            jo.put("refer to", clazz.getName());
            return jo;
        }

        hasDone.push(clazz);

        logger.debug(String.format("dumping object: %s", clazz.getName()));
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (f.isAnnotationPresent(APINoSee.class) || f.isAnnotationPresent(NoJsonSchema.class)) {
                    continue;
                }
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }

                Class ct = f.getType();
                Type t = f.getGenericType();
                if (ct.equals(clazz)) {
                    jo.put(f.getName(), "refer to self");
                } else if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    if (Collection.class.isAssignableFrom(ct)) {
                        JSONArray arr = dumpCollection(pt, hasDone);
                        jo.put(f.getName(), arr);
                    } else if (Map.class.isAssignableFrom(ct)) {
                        Class<?> keyType = (Class<?>) pt.getActualTypeArguments()[0];
                        if (!String.class.isAssignableFrom(keyType)) {
                            throw new IllegalArgumentException(String.format(
                                    "Field[%s] of class[%s] is type of Map, but type[%s] of its key element is not String, unable to dump", f.getName(),
                                    clazz.getName(), keyType));
                        }
                        JSONObject mj = dumpMap(pt, hasDone);
                        jo.put(f.getName(), mj);
                    } else {
                        throw new IllegalArgumentException(String.format(
                                "Field[%s] of class[%s] is type of java generic type[%s], but it's not Collection or Map, unable to dump", f.getName(),
                                clazz.getName(), pt.getRawType()));
                    }
                } else {
                    if (isPrimitiveType(ct)) {
                        jo.put(f.getName(), populateTemplateString(f));
                    } else if (Collection.class.isAssignableFrom(ct)) {
                        logger.warn(String.format(
                                "Field[%s] of class[%s] is type of Collection, unable to dump it because it doesn't have java generic type information",
                                f.getName(), clazz.getName()));
                        jo.put(f.getName(), dumpObject(ArrayList.class, hasDone));
                    } else if (Map.class.isAssignableFrom(ct)) {
                        logger.warn(String.format(
                                "Field[%s] of class[%s] is type of Map, unable to dump it because it doesn't have java generic type information", f.getName(),
                                clazz.getName()));
                        jo.put(f.getName(), dumpObject(HashMap.class, hasDone));
                    } else {
                        logger.debug(String.format("dumping %s, %s", f.getName(), ct.getName()));
                        JSONObject oj = dumpObject(ct, hasDone);
                        jo.put(f.getName(), oj);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && clazz != Object.class);

        hasDone.pop();
        return jo;
    }

    public static String dump(Class<?> clazz) {
        try {
            Stack<Class> hasDone = new Stack<Class>();
            JSONObject oj = dumpObject(clazz, hasDone);
            JSONObject root = nj();
            root.put(clazz.getName(), oj);
            return root.toString(2);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Unable to dump class[%s]", clazz.getName()), e);
        }
    }
}
