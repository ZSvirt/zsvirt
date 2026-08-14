package org.zstack.header.storage.backup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public class BackupStorageAllocatorStrategyType {
    private static Map<String, BackupStorageAllocatorStrategyType> types = Collections.synchronizedMap(new HashMap<String, BackupStorageAllocatorStrategyType>());
    private final String typeName;

    public BackupStorageAllocatorStrategyType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static BackupStorageAllocatorStrategyType valueOf(String typeName) {
        BackupStorageAllocatorStrategyType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("BackupStorageAllocatorStrategyType type: " + typeName + " was not registered by any BackupStorageAllocatorStrategyFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof BackupStorageAllocatorStrategyType)) {
            return false;
        }

        BackupStorageAllocatorStrategyType type = (BackupStorageAllocatorStrategyType) t;
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
