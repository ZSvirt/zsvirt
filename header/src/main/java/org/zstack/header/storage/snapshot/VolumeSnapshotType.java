package org.zstack.header.storage.snapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class VolumeSnapshotType {
    private static Map<String, VolumeSnapshotType> types = Collections.synchronizedMap(new HashMap<String, VolumeSnapshotType>());
    private final String typeName;

    public VolumeSnapshotType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static VolumeSnapshotType valueOf(String typeName) {
        VolumeSnapshotType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("SnapshotType type: " + typeName + " was not registered by any SnapshotFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof VolumeSnapshotType)) {
            return false;
        }

        VolumeSnapshotType type = (VolumeSnapshotType) t;
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
