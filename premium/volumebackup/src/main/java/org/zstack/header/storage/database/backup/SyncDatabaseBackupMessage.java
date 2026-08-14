package org.zstack.header.storage.database.backup;

public interface SyncDatabaseBackupMessage extends DatabaseBackupMessage {
    String getSrcBackupStorageUuid();
    String getDstBackupStorageUuid();
}
