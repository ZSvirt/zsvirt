package org.zstack.header.allocator;

import java.util.*;

public class HostAllocatorStrategyType {
    private static Map<String, HostAllocatorStrategyType> types = Collections.synchronizedMap(new HashMap<String, HostAllocatorStrategyType>());
    private static final HostAllocatorStrategyType defaultStrategy = new HostAllocatorStrategyType(HostAllocatorConstant.DEFAULT_HOST_ALLOCATOR_STRATEGY_TYPE);
    private final String typeName;
    private final boolean exposed;

    public HostAllocatorStrategyType(String typeName) {
        this(typeName, true);
    }

    public HostAllocatorStrategyType(String typeName, boolean exposed) {
        this.typeName = typeName;
        this.exposed = exposed;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static HostAllocatorStrategyType valueOf(String typeName) {
        HostAllocatorStrategyType type = types.get(typeName);
        if (type != null) {
            return type;
        }

        if (typeName == null) {
            return defaultStrategy;
        }

        throw new IllegalArgumentException("HostAllocatorStrategy type: " + typeName + " was not registered by any HostAllocatorStrategyFactory");
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof HostAllocatorStrategyType)) {
            return false;
        }

        HostAllocatorStrategyType type = (HostAllocatorStrategyType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public boolean isExposed() {
        return exposed;
    }


    public static List<String> getAllExposedTypeNames() {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, HostAllocatorStrategyType> e : types.entrySet()) {
            HostAllocatorStrategyType t = e.getValue();
            if (t.isExposed()) {
                ret.add(e.getKey());
            }
        }

        return ret;
    }

    public static Collection<String> getAllTypeNames() {
        return types.keySet();
    }
}
