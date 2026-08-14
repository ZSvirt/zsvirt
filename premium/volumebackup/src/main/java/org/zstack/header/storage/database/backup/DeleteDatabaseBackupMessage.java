package org.zstack.header.storage.database.backup;

import java.util.List;

public interface DeleteDatabaseBackupMessage extends DatabaseBackupMessage {
    List<String> getBackupStorageUuids();
    boolean isDbOnly();
}
