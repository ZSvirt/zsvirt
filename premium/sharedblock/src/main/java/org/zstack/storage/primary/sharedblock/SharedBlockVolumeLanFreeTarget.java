package org.zstack.storage.primary.sharedblock;

public class SharedBlockVolumeLanFreeTarget {
    private String volumeUuid;
    private Integer requestIndex;
    private String primaryStorageUuid;
    private String volumeInstallPath;

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

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }
}
