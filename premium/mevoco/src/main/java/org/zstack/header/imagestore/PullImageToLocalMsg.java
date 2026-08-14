package org.zstack.header.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

public class PullImageToLocalMsg extends NeedReplyMessage implements BackupStorageMessage{
    private String installPath;
    private String backupStorageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
