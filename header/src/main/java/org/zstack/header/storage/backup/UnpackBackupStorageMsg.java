package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class UnpackBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String uuid;
    private String srcInstallPath;

    public String getSrcInstallPath() {
        return srcInstallPath;
    }

    public void setSrcInstallPath(String srcInstallPath) {
        this.srcInstallPath = srcInstallPath;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
}
