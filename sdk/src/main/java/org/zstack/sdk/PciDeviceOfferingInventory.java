package org.zstack.sdk;

import org.zstack.sdk.PciDeviceOfferingType;

public class PciDeviceOfferingInventory  {

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

    public PciDeviceOfferingType type;
    public void setType(PciDeviceOfferingType type) {
        this.type = type;
    }
    public PciDeviceOfferingType getType() {
        return this.type;
    }

    public java.lang.String vendorId;
    public void setVendorId(java.lang.String vendorId) {
        this.vendorId = vendorId;
    }
    public java.lang.String getVendorId() {
        return this.vendorId;
    }

    public java.lang.String deviceId;
    public void setDeviceId(java.lang.String deviceId) {
        this.deviceId = deviceId;
    }
    public java.lang.String getDeviceId() {
        return this.deviceId;
    }

    public java.lang.String subvendorId;
    public void setSubvendorId(java.lang.String subvendorId) {
        this.subvendorId = subvendorId;
    }
    public java.lang.String getSubvendorId() {
        return this.subvendorId;
    }

    public java.lang.String subdeviceId;
    public void setSubdeviceId(java.lang.String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }
    public java.lang.String getSubdeviceId() {
        return this.subdeviceId;
    }

    public java.lang.String ramSize;
    public void setRamSize(java.lang.String ramSize) {
        this.ramSize = ramSize;
    }
    public java.lang.String getRamSize() {
        return this.ramSize;
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

    public java.util.List attachedInstanceOfferings;
    public void setAttachedInstanceOfferings(java.util.List attachedInstanceOfferings) {
        this.attachedInstanceOfferings = attachedInstanceOfferings;
    }
    public java.util.List getAttachedInstanceOfferings() {
        return this.attachedInstanceOfferings;
    }

    public java.util.List matchedPciDevices;
    public void setMatchedPciDevices(java.util.List matchedPciDevices) {
        this.matchedPciDevices = matchedPciDevices;
    }
    public java.util.List getMatchedPciDevices() {
        return this.matchedPciDevices;
    }

}
