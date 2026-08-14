package org.zstack.header.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DiskOfferingType {
    private static Map<String, DiskOfferingType> types = Collections.synchronizedMap(new HashMap<String, DiskOfferingType>());
    private final String typeName;

    public DiskOfferingType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static DiskOfferingType valueOf(String typeName) {
        DiskOfferingType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("DiskOfferingType type: " + typeName + " was not registered by any DiskOfferingFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof DiskOfferingType)) {
            return false;
        }

        DiskOfferingType type = (DiskOfferingType) t;
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
