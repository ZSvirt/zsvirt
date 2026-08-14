package org.zstack.sdk;



public class RaidControllerInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String productName;
    public void setProductName(java.lang.String productName) {
        this.productName = productName;
    }
    public java.lang.String getProductName() {
        return this.productName;
    }

    public java.lang.String sasAddress;
    public void setSasAddress(java.lang.String sasAddress) {
        this.sasAddress = sasAddress;
    }
    public java.lang.String getSasAddress() {
        return this.sasAddress;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
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

    public java.lang.Integer adapterNumber;
    public void setAdapterNumber(java.lang.Integer adapterNumber) {
        this.adapterNumber = adapterNumber;
    }
    public java.lang.Integer getAdapterNumber() {
        return this.adapterNumber;
    }

    public java.util.List raidPhysicalDrives;
    public void setRaidPhysicalDrives(java.util.List raidPhysicalDrives) {
        this.raidPhysicalDrives = raidPhysicalDrives;
    }
    public java.util.List getRaidPhysicalDrives() {
        return this.raidPhysicalDrives;
    }

}
