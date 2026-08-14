package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply extends MessageReply {
    private String backupStorageInstallPath;

    public String getBackupStorageInstallPath() {
        return backupStorageInstallPath;
    }

    public void setBackupStorageInstallPath(String backupStorageInstallPath) {
        this.backupStorageInstallPath = backupStorageInstallPath;
    }
}
