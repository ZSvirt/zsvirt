package org.zstack.header.zone;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ZoneType {
    private static Map<String, ZoneType> types = Collections.synchronizedMap(new HashMap<String, ZoneType>());
    private final String typeName;

    public ZoneType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static ZoneType valueOf(String typeName) {
        ZoneType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("ZoneType type: " + typeName + " was not registered by any ZoneFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof ZoneType)) {
            return false;
        }

        ZoneType type = (ZoneType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
