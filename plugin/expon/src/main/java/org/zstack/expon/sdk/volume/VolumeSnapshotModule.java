package org.zstack.expon.sdk.volume;

/**
 * @example
 * {
 * "id": "ae747374-6c5f-46e2-b9c4-6a033a0bdcdd",
 * "name": "csac_00001",
 * "snap_name": "snapshot-ae747374-6c5f-46e2-b9c4-6a033a0bdcdd ",
 * "snap_size": 107372100,
 * "data_size": 107372100,
 * "volume_id": "990273f1-3665-4c56-97a5-9c56b2a35954",
 * "volume_name": "volume-ae747374-6c5f-46e2-b9c4-6a033a0bdcdd",
 * "volume_disp_name": "volume1",
 * "is_delete": false,
 * "pool_id": "978273f1-3665-4c56-97a5-9c56b2a35954",
 * "pool_name": "zxc"
 * }
 */
public class VolumeSnapshotModule {
    private String id;
    private String name;
    private String snapName;
    private long snapSize;
    private long dataSize;
    private String volumeId;
    private String volumeName;
    private String volumeDispName;
    private boolean isDelete;
    private String poolId;
    private String poolName;

    private String wwn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSnapName() {
        return snapName;
    }

    public void setSnapName(String snapName) {
        this.snapName = snapName;
    }

    public long getSnapSize() {
        return snapSize;
    }

    public void setSnapSize(long snapSize) {
        this.snapSize = snapSize;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getVolumeDispName() {
        return volumeDispName;
    }

    public void setVolumeDispName(String volumeDispName) {
        this.volumeDispName = volumeDispName;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }
}
