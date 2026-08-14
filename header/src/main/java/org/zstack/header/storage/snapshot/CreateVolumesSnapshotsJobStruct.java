package org.zstack.header.storage.snapshot;

/**
 * Create by weiwang at 2018/6/11
 */
public class CreateVolumesSnapshotsJobStruct {
    private String resourceUuid;
    private String volumeUuid;
    private String name;
    private String description;
    private String primaryStorageUuid;
    private VolumeSnapshotStruct volumeSnapshotStruct;

    public VolumeSnapshotStruct getVolumeSnapshotStruct() {
        return volumeSnapshotStruct;
    }

    public void setVolumeSnapshotStruct(VolumeSnapshotStruct volumeSnapshotStruct) {
        this.volumeSnapshotStruct = volumeSnapshotStruct;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
