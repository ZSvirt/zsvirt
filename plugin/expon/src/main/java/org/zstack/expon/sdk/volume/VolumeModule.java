package org.zstack.expon.sdk.volume;


/**
 * @example
 * {
 * "data_size": 0,
 * "id": "ae747374-6c5f-46e2-b9c4-6a033a0bdcdd",
 * "is_delete": false,
 * "name": "csac_00001",
 * "pool_id": "990273f1-3665-4c56-97a5-9c56b2a35954",
 * "pool_name": "zxc",
 * "qos_status": false,
 * "volume_name": "volume-ae747374-6c5f-46e2-b9c4-6a033a0bdcdd",
 * "volume_size": 1073741824,
 * "wwn": "600140501e000000b9c46a033a0bdcdd"
 * }
 */
public class VolumeModule {
    private String id;
    private String name;
    private String poolId;
    private String poolName;
    private String volumeName;
    private String wwn;
    private long volumeSize;
    private long dataSize;
    private boolean isDelete;
    private boolean qosStatus;
    private ExponVolumeQos qos;

    private String runStatus;

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

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    public boolean isQosStatus() {
        return qosStatus;
    }

    public void setQosStatus(boolean qosStatus) {
        this.qosStatus = qosStatus;
    }

    public ExponVolumeQos getQos() {
        return qos;
    }

    public void setQos(ExponVolumeQos qos) {
        this.qos = qos;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }
}
