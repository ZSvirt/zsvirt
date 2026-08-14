package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/12/2.
 */
public class ArchiveBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String uuid;
    private String targetInstallPath;
    private boolean dryRun;

    public String getTargetInstallPath() {
        return targetInstallPath;
    }

    public void setTargetInstallPath(String targetInstallPath) {
        this.targetInstallPath = targetInstallPath;
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

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
