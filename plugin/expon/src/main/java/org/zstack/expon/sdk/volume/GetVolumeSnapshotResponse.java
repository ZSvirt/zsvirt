package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

public class GetVolumeSnapshotResponse extends ExponResponse {
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
