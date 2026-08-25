package org.zstack.header.storage.volume.backup;

import java.util.List;

public class VolumeBackupDeletionStruct {
    private String volumeBackupUuid;
    private List<String> backupStorageUuids;
    private boolean handleDependency;

    public String getVolumeBackupUuid() {
        return volumeBackupUuid;
    }

    public void setVolumeBackupUuid(String volumeBackupUuid) {
        this.volumeBackupUuid = volumeBackupUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public boolean isHandleDependency() {
        return handleDependency;
    }

    public void setHandleDependency(boolean handleDependency) {
        this.handleDependency = handleDependency;
    }
}
