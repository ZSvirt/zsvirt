package org.zstack.sdk;

import org.zstack.sdk.PrimaryStorageHostStatus;

public class SharedBlockGroupPrimaryStorageHostRefInventory  {

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.Integer hostId;
    public void setHostId(java.lang.Integer hostId) {
        this.hostId = hostId;
    }
    public java.lang.Integer getHostId() {
        return this.hostId;
    }

    public PrimaryStorageHostStatus status;
    public void setStatus(PrimaryStorageHostStatus status) {
        this.status = status;
    }
    public PrimaryStorageHostStatus getStatus() {
        return this.status;
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

}
