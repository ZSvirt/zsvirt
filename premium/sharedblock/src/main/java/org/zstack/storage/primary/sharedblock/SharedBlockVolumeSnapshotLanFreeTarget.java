package org.zstack.storage.primary.sharedblock;

public class SharedBlockVolumeSnapshotLanFreeTarget {
    private String volumeSnapshotUuid;
    private String volumeUuid;
    private Integer requestIndex;
    private String primaryStorageUuid;
    private String volumeSnapshotInstallPath;

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public Integer getRequestIndex() {
        return requestIndex;
    }

    public void setRequestIndex(Integer requestIndex) {
        this.requestIndex = requestIndex;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVolumeSnapshotInstallPath() {
        return volumeSnapshotInstallPath;
    }

    public void setVolumeSnapshotInstallPath(String volumeSnapshotInstallPath) {
        this.volumeSnapshotInstallPath = volumeSnapshotInstallPath;
    }

}
