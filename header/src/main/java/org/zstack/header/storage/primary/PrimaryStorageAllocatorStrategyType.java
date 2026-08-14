package org.zstack.header.storage.primary;

import java.util.*;

public class PrimaryStorageAllocatorStrategyType {
    private static Map<String, PrimaryStorageAllocatorStrategyType> types = Collections.synchronizedMap(new HashMap<String, PrimaryStorageAllocatorStrategyType>());
    private final String typeName;
    private final boolean exposed;

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public PrimaryStorageAllocatorStrategyType(String typeName) {
        this(typeName, true);
    }

    public PrimaryStorageAllocatorStrategyType(String typeName, boolean exposed) {
        this.typeName = typeName;
        this.exposed = exposed;
        types.put(typeName, this);
    }

    public static PrimaryStorageAllocatorStrategyType valueOf(String typeName) {
        PrimaryStorageAllocatorStrategyType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("PrimaryStorageAlloactorStrategyType type: " + typeName + " was not registered by any PrimaryStorageAlloactorStrategyFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof PrimaryStorageAllocatorStrategyType)) {
            return false;
        }

        PrimaryStorageAllocatorStrategyType type = (PrimaryStorageAllocatorStrategyType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static List<String> getAllExposedTypeNames() {
        List<String> ret = new ArrayList<String>();
        for (Map.Entry<String, PrimaryStorageAllocatorStrategyType> e : types.entrySet()) {
            if (e.getValue().isExposed()) {
                ret.add(e.getKey());
            }
        }

        return ret;
    }

    public boolean isExposed() {
        return exposed;
    }
}
