package org.zstack.header.storage.backup;

import org.zstack.header.message.APIMessage;

public class APIScanBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
 

    public static APIScanBackupStorageMsg __example__() {
        APIScanBackupStorageMsg msg = new APIScanBackupStorageMsg();
        return msg;
    }
    
}