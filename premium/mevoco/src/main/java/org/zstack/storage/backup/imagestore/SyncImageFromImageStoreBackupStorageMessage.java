package org.zstack.storage.backup.imagestore;

public interface SyncImageFromImageStoreBackupStorageMessage {
    String getUuid();
    String getSrcBackupStorageUuid();
    String getDstBackupStorageUuid();
    String getName();
    String getDescription();
}
