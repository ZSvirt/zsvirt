package org.zstack.storage.migration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by GuoYi on 9/4/17.
 */
public class StorageMigrationChainType {
    private static Map<String, StorageMigrationChainType> types =
            Collections.synchronizedMap(new HashMap<String, StorageMigrationChainType>());
    private final String typeName;

    public StorageMigrationChainType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static StorageMigrationChainType valueOf(String typeName) {
        StorageMigrationChainType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("StorageMigrationChain type: " + typeName + " was not registered by any StorageMigrateChainFactory");
        }
        return type;
    }

    public static Collection<String> getAllTypeNames() {
        return types.keySet();
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageMigrationChainType that = (StorageMigrationChainType) o;

        return typeName != null ? typeName.equals(that.typeName) : that.typeName == null;
    }

    @Override
    public int hashCode() {
        return typeName != null ? typeName.hashCode() : 0;
    }
}
