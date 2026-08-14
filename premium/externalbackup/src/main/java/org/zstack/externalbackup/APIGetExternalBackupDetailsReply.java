package org.zstack.externalbackup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = "all")
public abstract class APIGetExternalBackupDetailsReply extends APIReply {
    protected List<VmExternalBackupInfo> vmBackupInfos = new ArrayList<>();
    protected List<VolumeExternalBackupInfo> volumeBackupInfos = new ArrayList<>();
    protected List<BackupStorageExternalBackupInfo> backupStorageBackupInfos = new ArrayList<>();
    protected String version;

    public List<VmExternalBackupInfo> getVmBackupInfos() {
        return vmBackupInfos;
    }

    public void setVmBackupInfos(List<VmExternalBackupInfo> vmBackupInfos) {
        this.vmBackupInfos = vmBackupInfos;
    }

    public List<VolumeExternalBackupInfo> getVolumeBackupInfos() {
        return volumeBackupInfos;
    }

    public void setVolumeBackupInfos(List<VolumeExternalBackupInfo> volumeBackupInfos) {
        this.volumeBackupInfos = volumeBackupInfos;
    }

    public List<BackupStorageExternalBackupInfo> getBackupStorageBackupInfos() {
        return backupStorageBackupInfos;
    }

    public void setBackupStorageBackupInfos(List<BackupStorageExternalBackupInfo> backupStorageBackupInfos) {
        this.backupStorageBackupInfos = backupStorageBackupInfos;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
