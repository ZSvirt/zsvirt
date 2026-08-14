package org.zstack.header.storage.backup;

import org.zstack.header.message.Message;

public class ScanBackupStorageMsg extends Message implements BackupStorageMessage {
    private String backupStorageUuid;

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
