package org.zstack.header.storage.backup;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class SoftwareUpgradePackageDeployMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String backupStorageHostUuid;
    private String upgradePackagePath;
    private String upgradePackageTargetPath;
    private int targetHostSshPort;
    private String targetHostSshUsername;
    @NoLogging
    private String targetHostSshPassword;
    private String targetHostIp;
    private String upgradeScriptPath;
    private String softwareType;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getBackupStorageHostUuid() {
        return backupStorageHostUuid;
    }

    public void setBackupStorageHostUuid(String backupStorageHostUuid) {
        this.backupStorageHostUuid = backupStorageHostUuid;
    }

    public String getUpgradePackageTargetPath() {
        return upgradePackageTargetPath;
    }

    public void setUpgradePackageTargetPath(String upgradePackageTargetPath) {
        this.upgradePackageTargetPath = upgradePackageTargetPath;
    }

    public String getUpgradePackagePath() {
        return upgradePackagePath;
    }

    public void setUpgradePackagePath(String upgradePackagePath) {
        this.upgradePackagePath = upgradePackagePath;
    }

    public int getTargetHostSshPort() {
        return targetHostSshPort;
    }

    public void setTargetHostSshPort(int targetHostSshPort) {
        this.targetHostSshPort = targetHostSshPort;
    }

    public String getTargetHostSshUsername() {
        return targetHostSshUsername;
    }

    public void setTargetHostSshUsername(String targetHostSshUsername) {
        this.targetHostSshUsername = targetHostSshUsername;
    }

    public String getTargetHostSshPassword() {
        return targetHostSshPassword;
    }

    public void setTargetHostSshPassword(String targetHostSshPassword) {
        this.targetHostSshPassword = targetHostSshPassword;
    }

    public String getTargetHostIp() {
        return targetHostIp;
    }

    public void setTargetHostIp(String targetHostIp) {
        this.targetHostIp = targetHostIp;
    }

    public String getUpgradeScriptPath() {
        return upgradeScriptPath;
    }

    public void setUpgradeScriptPath(String upgradeScriptPath) {
        this.upgradeScriptPath = upgradeScriptPath;
    }

    public String getSoftwareType() {
        return softwareType;
    }

    public void setSoftwareType(String softwareType) {
        this.softwareType = softwareType;
    }
}
