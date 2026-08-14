package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class PingBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
