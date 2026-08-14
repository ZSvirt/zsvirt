package org.zstack.storage.primary.smp;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/4/29.
 */
public class UploadBitsToBackupStorageReply extends MessageReply {
    private String backupStorageInstallPath;

    public String getBackupStorageInstallPath() {
        return backupStorageInstallPath;
    }

    public void setBackupStorageInstallPath(String backupStorageInstallPath) {
        this.backupStorageInstallPath = backupStorageInstallPath;
    }
}
