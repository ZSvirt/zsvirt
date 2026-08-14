package org.zstack.sdk;



public class VmInstancePciDeviceSpecRefInventory  {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String pciSpecUuid;
    public void setPciSpecUuid(java.lang.String pciSpecUuid) {
        this.pciSpecUuid = pciSpecUuid;
    }
    public java.lang.String getPciSpecUuid() {
        return this.pciSpecUuid;
    }

    public java.lang.Integer pciDeviceNumber;
    public void setPciDeviceNumber(java.lang.Integer pciDeviceNumber) {
        this.pciDeviceNumber = pciDeviceNumber;
    }
    public java.lang.Integer getPciDeviceNumber() {
        return this.pciDeviceNumber;
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
