package org.zstack.network.hostNetworkInterface;

import java.util.*;

public class HostNetworkBondingType {
    private static Map<String, HostNetworkBondingType> types = Collections.synchronizedMap(new HashMap<String, HostNetworkBondingType>());
    private final String typeName;
    private boolean exposed = true;

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public HostNetworkBondingType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public HostNetworkBondingType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public static HostNetworkBondingType valueOf(String typeName) {
        HostNetworkBondingType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("HostNetworkBondingType type: " + typeName + " was not registered");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof HostNetworkBondingType)) {
            return false;
        }

        HostNetworkBondingType type = (HostNetworkBondingType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (HostNetworkBondingType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
