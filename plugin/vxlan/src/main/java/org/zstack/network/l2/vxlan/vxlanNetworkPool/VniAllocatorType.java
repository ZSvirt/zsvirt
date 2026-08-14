package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by weiwang on 10/03/2017.
 */
public class VniAllocatorType {
    private static Map<String, VniAllocatorType> types = Collections.synchronizedMap(new HashMap<String, VniAllocatorType>());

    private final String typeName;

    public VniAllocatorType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static VniAllocatorType valueOf(String typeName) {
        VniAllocatorType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VniAllocatorType type: " + typeName + " was not registered by any IpAllocator");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof VniAllocatorType)) {
            return false;
        }

        VniAllocatorType type = (VniAllocatorType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
