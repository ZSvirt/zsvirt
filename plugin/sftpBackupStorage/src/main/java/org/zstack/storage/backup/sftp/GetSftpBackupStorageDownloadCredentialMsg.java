package org.zstack.storage.backup.sftp;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

public class GetSftpBackupStorageDownloadCredentialMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    
    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
