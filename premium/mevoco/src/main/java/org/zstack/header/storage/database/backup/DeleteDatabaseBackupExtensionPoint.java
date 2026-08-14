package org.zstack.header.storage.database.backup;

import java.util.List;

public interface DeleteDatabaseBackupExtensionPoint {
    void afterDeleteDatabaseBackup(String backupUuid, List<String> bsUuids);
}
