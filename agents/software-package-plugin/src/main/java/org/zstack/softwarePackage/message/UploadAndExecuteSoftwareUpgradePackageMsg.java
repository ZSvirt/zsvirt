package org.zstack.softwarePackage.message;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.softwarePackage.header.APIUploadAndExecuteSoftwareUpgradePackageMsg;

public class UploadAndExecuteSoftwareUpgradePackageMsg extends NeedReplyMessage {
    private String softwarePackageUuid;
    private String backupStorageUuid;
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    private String installPath;
    private String originalInstallPath;
    private String originalUnzipInstallPath;
    private String originalMd5sum;
    private long originalSize;
    private String originalBackupStorageUuid;
    private String originalBackupStorageHostUuid;
    private boolean originalPackageSnapshotCaptured;
    private SessionInventory session;
    private String upgradeType;

    public boolean needTrack() {
        return url != null && url.startsWith("upload://");
    }

    public String getSoftwarePackageUuid() {
        return softwarePackageUuid;
    }

    public void setSoftwarePackageUuid(String softwarePackageUuid) {
        this.softwarePackageUuid = softwarePackageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
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

    public boolean isOriginalPackageSnapshotCaptured() {
        return originalPackageSnapshotCaptured;
    }

    public void setOriginalPackageSnapshotCaptured(boolean originalPackageSnapshotCaptured) {
        this.originalPackageSnapshotCaptured = originalPackageSnapshotCaptured;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public String getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(String upgradeType) {
        this.upgradeType = upgradeType;
    }

    public static UploadAndExecuteSoftwareUpgradePackageMsg fromApiMessage(APIUploadAndExecuteSoftwareUpgradePackageMsg apiMessage) {
        if (apiMessage == null) {
            throw new IllegalArgumentException("apiMessage cannot be null");
        }

        UploadAndExecuteSoftwareUpgradePackageMsg msg = new UploadAndExecuteSoftwareUpgradePackageMsg();
        msg.setUrl(apiMessage.getUrl());
        msg.setSession(apiMessage.getSession());
        msg.setUpgradeType(apiMessage.getUpgradeType());
        msg.setInstallPath(apiMessage.getInstallPath());
        msg.setSoftwarePackageUuid(apiMessage.getUuid());
        msg.setBackupStorageUuid(apiMessage.getBackupStorageUuid());
        return msg;
    }
}
