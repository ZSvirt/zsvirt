package org.zstack.header.storage.backup;

import org.zstack.header.message.CancelMessage;

public class CancelDownloadFileOnBackupStorageHostMsg extends CancelMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String backupStorageHostUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getBackupStorageHostUuid() {
        return backupStorageHostUuid;
    }

    public void setBackupStorageHostUuid(String backupStorageHostUuid) {
        this.backupStorageHostUuid = backupStorageHostUuid;
    }
}
