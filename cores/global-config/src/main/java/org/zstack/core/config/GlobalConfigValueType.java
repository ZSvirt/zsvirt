package org.zstack.core.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Allowed value types for global config XML {@code <type>} and Java {@code GlobalConfigDef.type}.
 * XML may omit type; scanner then defaults to {@link #STRING}.
 */
public enum GlobalConfigValueType {
    STRING(String.class, "String", "string", "java.lang.String"),
    INTEGER(Integer.class, "Integer", "int", "java.lang.Integer"),
    LONG(Long.class, "Long", "long", "java.lang.Long"),
    BOOLEAN(Boolean.class, "Boolean", "boolean", "java.lang.Boolean"),
    FLOAT(Float.class, "Float", "float", "java.lang.Float"),
    DOUBLE(Double.class, "Double", "double", "java.lang.Double");

    private static final Map<String, GlobalConfigValueType> BY_TOKEN;

    static {
        Map<String, GlobalConfigValueType> tokens = new HashMap<>();
        for (GlobalConfigValueType t : values()) {
            tokens.put(t.javaClass.getName(), t);
            tokens.put(t.javaClass.getSimpleName(), t);
            for (String alias : t.aliases) {
                tokens.put(alias, t);
            }
        }
        BY_TOKEN = Collections.unmodifiableMap(tokens);
    }

    private final Class<?> javaClass;
    private final String[] aliases;

    GlobalConfigValueType(Class<?> javaClass, String... aliases) {
        this.javaClass = javaClass;
        this.aliases = aliases;
    }

    public String getJavaClassName() {
        return javaClass.getName();
    }

    public static GlobalConfigValueType parse(String token) {
        GlobalConfigValueType t = BY_TOKEN.get(token);
        if (t == null) {
            throw new IllegalArgumentException(String.format(
                    "unsupported global config type[%s], allowed: String, Integer, Long, Boolean, Float, Double",
                    token));
        }
        return t;
    }
}
