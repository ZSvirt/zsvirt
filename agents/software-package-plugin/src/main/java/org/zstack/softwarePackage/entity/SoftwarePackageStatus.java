package org.zstack.softwarePackage.entity;

public enum SoftwarePackageStatus {
    Uploading,
    UploadFailed,
    Uploaded,
    Installing,
    Installed,
    InstallFailed,
    Initializing,
    Initialized,
    InitializeFailed,
    Upgrading,
    UpgradePackageUploadFailed,
    UpgradePackageUploaded,
    UpgradeExecuteFailed,
    Upgraded,
}
