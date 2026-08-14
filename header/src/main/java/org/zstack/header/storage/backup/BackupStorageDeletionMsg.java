package org.zstack.header.storage.backup;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class BackupStorageDeletionMsg extends DeletionMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
