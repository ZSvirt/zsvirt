package org.zstack.sdk.zwatch.monitorgroup.entity;

import org.zstack.sdk.zwatch.alarm.AlarmStatus;

public class MonitorGroupInstanceInventory  {

    public java.lang.String groupUuid;
    public void setGroupUuid(java.lang.String groupUuid) {
        this.groupUuid = groupUuid;
    }
    public java.lang.String getGroupUuid() {
        return this.groupUuid;
    }

    public java.lang.String instanceResourceType;
    public void setInstanceResourceType(java.lang.String instanceResourceType) {
        this.instanceResourceType = instanceResourceType;
    }
    public java.lang.String getInstanceResourceType() {
        return this.instanceResourceType;
    }

    public java.lang.String instanceUuid;
    public void setInstanceUuid(java.lang.String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }
    public java.lang.String getInstanceUuid() {
        return this.instanceUuid;
    }

    public AlarmStatus status;
    public void setStatus(AlarmStatus status) {
        this.status = status;
    }
    public AlarmStatus getStatus() {
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

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

}
