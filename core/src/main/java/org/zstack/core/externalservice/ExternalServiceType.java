package org.zstack.core.externalservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExternalServiceType {
    private static Map<String, ExternalServiceType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;

    public ExternalServiceType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static Map<String, ExternalServiceType> getAllTypes() {
        return types;
    }

    public static ExternalServiceType valueOf(String typeName) {
        ExternalServiceType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("ExternalServiceType type: " + typeName + " was not registered by any AlarmActionFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof ExternalServiceType)) {
            return false;
        }

        ExternalServiceType type = (ExternalServiceType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
