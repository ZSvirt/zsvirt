package org.zstack.sdk;



public class VmSchedHistoryInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String schedType;
    public void setSchedType(java.lang.String schedType) {
        this.schedType = schedType;
    }
    public java.lang.String getSchedType() {
        return this.schedType;
    }

    public java.lang.String schedReason;
    public void setSchedReason(java.lang.String schedReason) {
        this.schedReason = schedReason;
    }
    public java.lang.String getSchedReason() {
        return this.schedReason;
    }

    public java.lang.String failReason;
    public void setFailReason(java.lang.String failReason) {
        this.failReason = failReason;
    }
    public java.lang.String getFailReason() {
        return this.failReason;
    }

    public java.lang.Boolean success;
    public void setSuccess(java.lang.Boolean success) {
        this.success = success;
    }
    public java.lang.Boolean getSuccess() {
        return this.success;
    }

    public java.lang.String lastHostUuid;
    public void setLastHostUuid(java.lang.String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }
    public java.lang.String getLastHostUuid() {
        return this.lastHostUuid;
    }

    public java.lang.String destHostUuid;
    public void setDestHostUuid(java.lang.String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }
    public java.lang.String getDestHostUuid() {
        return this.destHostUuid;
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

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
    }

}
