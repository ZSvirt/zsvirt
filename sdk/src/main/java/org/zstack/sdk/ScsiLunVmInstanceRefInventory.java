package org.zstack.sdk;



public class ScsiLunVmInstanceRefInventory  {

    public java.lang.String scsiLunUuid;
    public void setScsiLunUuid(java.lang.String scsiLunUuid) {
        this.scsiLunUuid = scsiLunUuid;
    }
    public java.lang.String getScsiLunUuid() {
        return this.scsiLunUuid;
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

    public java.lang.Integer deviceId;
    public void setDeviceId(java.lang.Integer deviceId) {
        this.deviceId = deviceId;
    }
    public java.lang.Integer getDeviceId() {
        return this.deviceId;
    }

    public boolean attachMultipath;
    public void setAttachMultipath(boolean attachMultipath) {
        this.attachMultipath = attachMultipath;
    }
    public boolean getAttachMultipath() {
        return this.attachMultipath;
    }

}
