package org.zstack.softwarePackage.compute;

import org.zstack.header.core.Completion;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.UploadSoftwarePackageToBackupStorageLongJobData;

import java.util.Map;

public interface UploadSoftwarePackageToBackupStorageExtensionPoint {
    String getSoftwarePackageType();

    void upgradeSoftwarePackage(SoftwarePackageVO softwarePackage, UploadSoftwarePackageToBackupStorageLongJobData msgData, Completion completion);

    Map<String, Long> getImagesSize(Map<String, Long> fileSizes);

    String getUpgradePackagePath(Map<String, Long> fileSizes);

    void afterUploadSoftwarePackageToBackupStorage(SoftwarePackageVO softwarePackage, UploadSoftwarePackageToBackupStorageLongJobData msgData, Completion completion);

    void cleanUpgradeSoftwarePackage(SoftwarePackageVO softwarePackage, String backupStorageUuid,
                                     String backupStorageHostUuid, String installPath,
                                     String unzipInstallPath, Completion completion);
}
