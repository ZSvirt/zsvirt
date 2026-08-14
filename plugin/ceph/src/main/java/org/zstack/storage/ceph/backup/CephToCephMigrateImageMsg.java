package org.zstack.storage.ceph.backup;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.io.Serializable;

/**
 * Created by GuoYi on 10/19/17.
 */
public class CephToCephMigrateImageMsg extends NeedReplyMessage implements BackupStorageMessage, Serializable {
    private String imageUuid;
    private long imageSize;
    private String srcInstallPath;
    private String dstInstallPath;
    private String dstMonHostname;
    private String dstMonSshUsername;
    @NoLogging
    private String dstMonSshPassword;
    private int dstMonSshPort;
    private String backupStorageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public long getImageSize() {
        return imageSize;
    }

    public void setImageSize(long imageSize) {
        this.imageSize = imageSize;
    }

    public String getSrcInstallPath() {
        return srcInstallPath;
    }

    public void setSrcInstallPath(String srcInstallPath) {
        this.srcInstallPath = srcInstallPath;
    }

    public String getDstInstallPath() {
        return dstInstallPath;
    }

    public void setDstInstallPath(String dstInstallPath) {
        this.dstInstallPath = dstInstallPath;
    }

    public String getDstMonHostname() {
        return dstMonHostname;
    }

    public void setDstMonHostname(String dstMonHostname) {
        this.dstMonHostname = dstMonHostname;
    }

    public String getDstMonSshUsername() {
        return dstMonSshUsername;
    }

    public void setDstMonSshUsername(String dstMonSshUsername) {
        this.dstMonSshUsername = dstMonSshUsername;
    }

    public String getDstMonSshPassword() {
        return dstMonSshPassword;
    }

    public void setDstMonSshPassword(String dstMonSshPassword) {
        this.dstMonSshPassword = dstMonSshPassword;
    }

    public int getDstMonSshPort() {
        return dstMonSshPort;
    }

    public void setDstMonSshPort(int dstMonSshPort) {
        this.dstMonSshPort = dstMonSshPort;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
