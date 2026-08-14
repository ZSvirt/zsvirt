package org.zstack.header.cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClusterType {
    private static Map<String, ClusterType> types = Collections.synchronizedMap(new HashMap<String, ClusterType>());
    private final String typeName;

    public ClusterType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static ClusterType valueOf(String typeName) {
        ClusterType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("ClusterType type: " + typeName + " was not registered by any ClusterFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof ClusterType)) {
            return false;
        }

        ClusterType type = (ClusterType) t;
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
