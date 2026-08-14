package org.zstack.sdk;



public class GetExternalBackupDetailsResult {
    public java.util.List vmBackupInfos;
    public void setVmBackupInfos(java.util.List vmBackupInfos) {
        this.vmBackupInfos = vmBackupInfos;
    }
    public java.util.List getVmBackupInfos() {
        return this.vmBackupInfos;
    }

    public java.util.List volumeBackupInfos;
    public void setVolumeBackupInfos(java.util.List volumeBackupInfos) {
        this.volumeBackupInfos = volumeBackupInfos;
    }
    public java.util.List getVolumeBackupInfos() {
        return this.volumeBackupInfos;
    }

    public java.util.List backupStorageBackupInfos;
    public void setBackupStorageBackupInfos(java.util.List backupStorageBackupInfos) {
        this.backupStorageBackupInfos = backupStorageBackupInfos;
    }
    public java.util.List getBackupStorageBackupInfos() {
        return this.backupStorageBackupInfos;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

}
