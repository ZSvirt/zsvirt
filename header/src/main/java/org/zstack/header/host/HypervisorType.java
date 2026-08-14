package org.zstack.header.host;

import java.util.*;

public class HypervisorType {
    private static Map<String, HypervisorType> types = Collections.synchronizedMap(new HashMap<String, HypervisorType>());
    private final String typeName;
    private boolean exposed = true;

    public HypervisorType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public HypervisorType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public static boolean hasType(String type) {
        return types.containsKey(type);
    }

    public static HypervisorType valueOf(String typeName) {
        HypervisorType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Hypervisor type: " + typeName + " was not registered by any HypervisorFactory");
        }
        return type;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof HypervisorType)) {
            return false;
        }

        HypervisorType type = (HypervisorType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (HypervisorType type : types.values()) {
            if (type.exposed) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
