package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class ChangeBackupStorageStatusMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String status;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
