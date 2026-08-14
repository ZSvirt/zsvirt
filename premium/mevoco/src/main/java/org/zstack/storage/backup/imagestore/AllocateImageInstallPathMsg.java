package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

public class AllocateImageInstallPathMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String parentInstallPath;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getParentInstallPath() {
        return parentInstallPath;
    }

    public void setParentInstallPath(String parentInstallPath) {
        this.parentInstallPath = parentInstallPath;
    }
}
