package org.zstack.sdk;

import org.zstack.sdk.MttyDeviceType;
import org.zstack.sdk.MttyDeviceState;
import org.zstack.sdk.MttyDeviceVirtStatus;

public class MttyDeviceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
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

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public MttyDeviceType type;
    public void setType(MttyDeviceType type) {
        this.type = type;
    }
    public MttyDeviceType getType() {
        return this.type;
    }

    public MttyDeviceState state;
    public void setState(MttyDeviceState state) {
        this.state = state;
    }
    public MttyDeviceState getState() {
        return this.state;
    }

    public MttyDeviceVirtStatus virtStatus;
    public void setVirtStatus(MttyDeviceVirtStatus virtStatus) {
        this.virtStatus = virtStatus;
    }
    public MttyDeviceVirtStatus getVirtStatus() {
        return this.virtStatus;
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
