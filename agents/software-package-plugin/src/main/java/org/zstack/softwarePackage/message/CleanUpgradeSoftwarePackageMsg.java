package org.zstack.softwarePackage.message;

import org.zstack.header.message.NeedReplyMessage;

public class CleanUpgradeSoftwarePackageMsg extends NeedReplyMessage {
    private String uuid;
    private String upgradeBackupStorageUuid;
    private String upgradeBackupStorageHostUuid;
    private String upgradeInstallPath;
    private String upgradeUnzipInstallPath;
    private String originalInstallPath;
    private String originalUnzipInstallPath;
    private String originalMd5sum;
    private long originalSize;
    private String originalBackupStorageUuid;
    private String originalBackupStorageHostUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUpgradeBackupStorageUuid() {
        return upgradeBackupStorageUuid;
    }

    public void setUpgradeBackupStorageUuid(String upgradeBackupStorageUuid) {
        this.upgradeBackupStorageUuid = upgradeBackupStorageUuid;
    }

    public String getUpgradeBackupStorageHostUuid() {
        return upgradeBackupStorageHostUuid;
    }

    public void setUpgradeBackupStorageHostUuid(String upgradeBackupStorageHostUuid) {
        this.upgradeBackupStorageHostUuid = upgradeBackupStorageHostUuid;
    }

    public String getUpgradeInstallPath() {
        return upgradeInstallPath;
    }

    public void setUpgradeInstallPath(String upgradeInstallPath) {
        this.upgradeInstallPath = upgradeInstallPath;
    }

    public String getUpgradeUnzipInstallPath() {
        return upgradeUnzipInstallPath;
    }

    public void setUpgradeUnzipInstallPath(String upgradeUnzipInstallPath) {
        this.upgradeUnzipInstallPath = upgradeUnzipInstallPath;
    }

    public String getOriginalInstallPath() {
        return originalInstallPath;
    }

    public void setOriginalInstallPath(String originalInstallPath) {
        this.originalInstallPath = originalInstallPath;
    }

    public String getOriginalUnzipInstallPath() {
        return originalUnzipInstallPath;
    }

    public void setOriginalUnzipInstallPath(String originalUnzipInstallPath) {
        this.originalUnzipInstallPath = originalUnzipInstallPath;
    }

    public String getOriginalMd5sum() {
        return originalMd5sum;
    }

    public void setOriginalMd5sum(String originalMd5sum) {
        this.originalMd5sum = originalMd5sum;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public String getOriginalBackupStorageUuid() {
        return originalBackupStorageUuid;
    }

    public void setOriginalBackupStorageUuid(String originalBackupStorageUuid) {
        this.originalBackupStorageUuid = originalBackupStorageUuid;
    }

    public String getOriginalBackupStorageHostUuid() {
        return originalBackupStorageHostUuid;
    }

    public void setOriginalBackupStorageHostUuid(String originalBackupStorageHostUuid) {
        this.originalBackupStorageHostUuid = originalBackupStorageHostUuid;
    }
}
