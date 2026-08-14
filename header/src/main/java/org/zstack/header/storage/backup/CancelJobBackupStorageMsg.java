package org.zstack.header.storage.backup;


import org.zstack.header.message.CancelMessage;

/**
 * Created by MaJin on 2019/9/5.
 */
public class CancelJobBackupStorageMsg extends CancelMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}