package org.zstack.header.storage.addon.primary;

import org.zstack.header.message.NeedReplyMessage;

public class DownloadExternalPrimaryStorageEncryptedImageMsg extends NeedReplyMessage {
    private String primaryStorageUuid;
    private String primaryStorageType;
    private String imageUuid;
    private String backupStorageUuid;
    private String backupStorageInstallPath;
    private String primaryStorageInstallPath;
    private String remoteTargetUrl;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageType() {
        return primaryStorageType;
    }

    public void setPrimaryStorageType(String primaryStorageType) {
        this.primaryStorageType = primaryStorageType;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getBackupStorageInstallPath() {
        return backupStorageInstallPath;
    }

    public void setBackupStorageInstallPath(String backupStorageInstallPath) {
        this.backupStorageInstallPath = backupStorageInstallPath;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public String getRemoteTargetUrl() {
        return remoteTargetUrl;
    }

    public void setRemoteTargetUrl(String remoteTargetUrl) {
        this.remoteTargetUrl = remoteTargetUrl;
    }
}
