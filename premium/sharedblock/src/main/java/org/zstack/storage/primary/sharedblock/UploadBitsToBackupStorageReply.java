package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.MessageReply;

public class UploadBitsToBackupStorageReply extends MessageReply {
    private String backupStorageInstallPath;

    public String getBackupStorageInstallPath() {
        return backupStorageInstallPath;
    }

    public void setBackupStorageInstallPath(String backupStorageInstallPath) {
        this.backupStorageInstallPath = backupStorageInstallPath;
    }
}
