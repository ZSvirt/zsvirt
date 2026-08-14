package org.zstack.header.network.l2;

import java.util.*;

public class L2ProviderType {
    private static Map<String, L2ProviderType> types = Collections.synchronizedMap(new HashMap<String, L2ProviderType>());
    private final String typeName;
    private boolean exposed = true;

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public L2ProviderType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public L2ProviderType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public static L2ProviderType valueOf(String typeName) {
        L2ProviderType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("L2ProviderType type: " + typeName + " was not registered");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof L2ProviderType)) {
            return false;
        }

        L2ProviderType type = (L2ProviderType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (L2ProviderType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
