package org.zstack.header.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InstanceOfferingType {
    private static Map<String, InstanceOfferingType> types = Collections.synchronizedMap(new HashMap<String, InstanceOfferingType>());
    private final String typeName;

    public InstanceOfferingType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static InstanceOfferingType valueOf(String typeName) {
        InstanceOfferingType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("InstanceOfferingType type: " + typeName + " was not registered by any InstanceOfferingFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof InstanceOfferingType)) {
            return false;
        }

        InstanceOfferingType type = (InstanceOfferingType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        return types.keySet();
    }
}
