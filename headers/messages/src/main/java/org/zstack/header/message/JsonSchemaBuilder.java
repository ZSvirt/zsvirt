package org.zstack.header.message;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Created by xing5 on 2016/12/12.
 */
public class JsonSchemaBuilder {
    private static final CLogger logger = Utils.getLogger(JsonSchemaBuilder.class);

    Object object;

    private LinkedHashMap<String, String> schema = new LinkedHashMap<>();

    public JsonSchemaBuilder(Object object) {
        this.object = object;
    }

    private boolean isSkip(Field f) {
        return f.isAnnotationPresent(NoJsonSchema.class) || Modifier.isStatic(f.getModifiers())
                || f.isAnnotationPresent(GsonTransient.class) || Modifier.isTransient(f.getModifiers());
    }

    private void build(Object o, Stack<String> paths) throws IllegalAccessException {
        List<Field> fields = FieldUtils.getAllFields(o.getClass());

        for (Field f : fields) {
            try {
                if (isSkip(f)) {
                    continue;
                }

                f.setAccessible(true);
                Object value = f.get(o);
                if (value == null) {
                    // null value
                    continue;
                }

                final String valueClassCanonicalName = value.getClass().getCanonicalName();
                if (valueClassCanonicalName == null) {
                    continue;
                }

                if (valueClassCanonicalName.startsWith("java.")) {
                    // for JRE classes, only deal with Collection and Map
                    if (value instanceof Collection) {
                        Collection c = (Collection) value;

                        Class gtype = FieldUtils.getGenericType(f);

                        if (gtype != null && !gtype.getName().startsWith("java.")) {
                            int i = 0;
                            for (Object co : c) {
                                paths.push(String.format("%s[%s]", f.getName(), i++));
                                build(co, paths);
                                paths.pop();
                            }
                        }

                    } else if (value instanceof Map) {
                        Class gtype = FieldUtils.getGenericType(f);

                        if (gtype != null && !gtype.getName().startsWith("java.")) {
                            for (Object me : ((Map) value).entrySet()) {
                                Map.Entry e = (Map.Entry) me;
                                paths.push(String.format("%s.%s", f.getName(), e.getKey().toString()));
                                build(e.getValue(), paths);
                                paths.pop();
                            }
                        }
                    }

                    // don't record standard JRE classes

                } else if (valueClassCanonicalName.startsWith("org.zstack")) {
                    paths.push(f.getName());
                    build(value, paths);
                    paths.pop();
                }
            } catch (StackOverflowError e) {
                throw new CloudRuntimeException(String.format("StackOverflowError at object: %s, o: %s, field[name:%s, type: %s], paths: %s",
                        object.getClass(), o.getClass(), f.getName(), f.getType(), paths));
            }
        }

        if (!paths.isEmpty()) {
            schema.put(StringUtils.join(paths, "."), o.getClass().getName());
        }
    }

    // support Map and org.zstack.* objects
    public Map<String, String> build() {
        try {
            if (!object.getClass().getName().startsWith("org.zstack") && !(object instanceof Map)) {
                throw new CloudRuntimeException(String.format("only a org.zstack.* object can be built schema, %s is not", object.getClass()));
            }

            if (object instanceof Map) {
                Map m = (Map) object;
                for (Object o : m.entrySet()) {
                    Map.Entry e = (Map.Entry) o;
                    if (e.getValue() == null) {
                        continue;
                    }

                    if (e.getValue().getClass().getName().startsWith("java.") &&
                            !Collection.class.isAssignableFrom(e.getValue().getClass())) {
                        continue;
                    }

                    if (Collection.class.isAssignableFrom(e.getValue().getClass())) {
                        Collection c = (Collection) e.getValue();
                        int i = 0;
                        for (Object it : c) {
                            Stack<String> path = new Stack<>();
                            path.add(String.format("%s[%s]", e.getKey(), i++));
                            build(it, path);
                        }
                    } else {
                        build(e.getValue(), new Stack<String>() {
                            {
                                add(e.getKey().toString());
                            }
                        });
                    }
                }
            } else {
                build(object, new Stack<>());
            }

            List<String> keys = new ArrayList<>(schema.keySet());
            Collections.reverse(keys);
            LinkedHashMap ret = new LinkedHashMap(schema.size());
            for (String key : keys) {
                ret.put(key, schema.get(key));
            }

            return ret;
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn(String.format("failed to build schema of %s, %s", object.getClass(), JSONObjectUtil.toJsonString(object)));
            throw new CloudRuntimeException(e);
        }
    }
}
