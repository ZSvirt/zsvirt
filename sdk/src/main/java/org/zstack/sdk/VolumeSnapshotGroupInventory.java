package org.zstack.sdk;



public class VolumeSnapshotGroupInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.Integer snapshotCount;
    public void setSnapshotCount(java.lang.Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
    }
    public java.lang.Integer getSnapshotCount() {
        return this.snapshotCount;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.util.List volumeSnapshotRefs;
    public void setVolumeSnapshotRefs(java.util.List volumeSnapshotRefs) {
        this.volumeSnapshotRefs = volumeSnapshotRefs;
    }
    public java.util.List getVolumeSnapshotRefs() {
        return this.volumeSnapshotRefs;
    }

}
