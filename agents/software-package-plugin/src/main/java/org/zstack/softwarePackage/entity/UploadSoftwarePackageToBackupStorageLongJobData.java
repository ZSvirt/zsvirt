package org.zstack.softwarePackage.entity;

import org.zstack.core.Platform;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.longjob.LongJobMessageData;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.softwarePackage.header.APIUploadAndExecuteSoftwareUpgradePackageMsg;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToBackupStorageMsg;
import org.zstack.softwarePackage.message.UploadAndExecuteSoftwareUpgradePackageMsg;
import org.zstack.softwarePackage.message.UploadSoftwarePackageToBackupStorageMsg;

import java.util.List;

public class UploadSoftwarePackageToBackupStorageLongJobData extends LongJobMessageData {
    public String softwarePackageUuid;
    public String name;
    public String backupStorageUuid;
    public String url;
    public String installPath;
    public String unzipInstallPath;
    public String originalInstallPath;
    public String originalUnzipInstallPath;
    public String originalMd5sum;
    public long originalSize;
    public String originalBackupStorageUuid;
    public String originalBackupStorageHostUuid;
    public String type;
    public SessionInventory session;
    public String backupStorageHostUuid;
    public List<String> imagesPath;
    public String upgradePackagePath;
    public String upgradeType;

    public UploadSoftwarePackageToBackupStorageLongJobData(NeedReplyMessage msg) {
        super(msg);
    }

    public boolean needTrack() {
        return url != null && url.startsWith("upload://") && backupStorageUuid != null;
    }

    public static UploadSoftwarePackageToBackupStorageLongJobData buildFileLongJobDataFromApiMsg(APIUploadSoftwarePackageToBackupStorageMsg msg) {
        UploadSoftwarePackageToBackupStorageLongJobData data = new UploadSoftwarePackageToBackupStorageLongJobData(msg);
        data.softwarePackageUuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        data.name = msg.getName() != null ? msg.getName() : msg.getType();
        data.backupStorageUuid = msg.getBackupStorageUuid();
        data.url = msg.getUrl();
        data.installPath = msg.getInstallPath();
        data.type = msg.getType();
        data.session = msg.getSession();
        return data;
    }

    public static UploadSoftwarePackageToBackupStorageLongJobData buildFileLongJobDataFromMsg(UploadSoftwarePackageToBackupStorageMsg msg) {
        UploadSoftwarePackageToBackupStorageLongJobData data = new UploadSoftwarePackageToBackupStorageLongJobData(msg);
        data.softwarePackageUuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        data.name = msg.getName() != null ? msg.getName() : msg.getType();
        data.backupStorageUuid = msg.getBackupStorageUuid();
        data.url = msg.getUrl();
        data.type = msg.getType();
        data.session = msg.getSession();
        data.installPath = msg.getInstallPath();
        return data;
    }

    public static UploadSoftwarePackageToBackupStorageLongJobData buildFileLongJobDataFromApiMsg(APIUploadAndExecuteSoftwareUpgradePackageMsg msg) {
        UploadSoftwarePackageToBackupStorageLongJobData data = new UploadSoftwarePackageToBackupStorageLongJobData(msg);
        data.softwarePackageUuid = msg.getUuid();
        data.backupStorageUuid = msg.getBackupStorageUuid();
        data.url = msg.getUrl();
        data.session = msg.getSession();
        data.installPath = msg.getInstallPath();
        data.upgradeType = msg.getUpgradeType();
        return data;
    }

    public static UploadSoftwarePackageToBackupStorageLongJobData buildFileLongJobDataFromMsg(UploadAndExecuteSoftwareUpgradePackageMsg msg) {
        UploadSoftwarePackageToBackupStorageLongJobData data = new UploadSoftwarePackageToBackupStorageLongJobData(msg);
        data.softwarePackageUuid = msg.getSoftwarePackageUuid();
        data.backupStorageUuid = msg.getBackupStorageUuid();
        data.url = msg.getUrl();
        data.session = msg.getSession();
        data.installPath = msg.getInstallPath();
        data.upgradeType = msg.getUpgradeType();
        data.originalInstallPath = msg.getOriginalInstallPath();
        data.originalUnzipInstallPath = msg.getOriginalUnzipInstallPath();
        data.originalMd5sum = msg.getOriginalMd5sum();
        data.originalSize = msg.getOriginalSize();
        data.originalBackupStorageUuid = msg.getOriginalBackupStorageUuid();
        data.originalBackupStorageHostUuid = msg.getOriginalBackupStorageHostUuid();
        return data;
    }
}
