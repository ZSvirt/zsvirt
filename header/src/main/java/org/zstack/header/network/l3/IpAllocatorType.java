package org.zstack.header.network.l3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IpAllocatorType {
    private static Map<String, IpAllocatorType> types = Collections.synchronizedMap(new HashMap<String, IpAllocatorType>());

    private final String typeName;

    public IpAllocatorType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static IpAllocatorType valueOf(String typeName) {
        IpAllocatorType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("IpAllocatorType type: " + typeName + " was not registered by any IpAllocator");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof IpAllocatorType)) {
            return false;
        }

        IpAllocatorType type = (IpAllocatorType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
