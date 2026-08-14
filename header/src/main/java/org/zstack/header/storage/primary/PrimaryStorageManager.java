package org.zstack.header.storage.primary;

public interface PrimaryStorageManager {
    PrimaryStorageFactory getPrimaryStorageFactory(PrimaryStorageType type);
}
