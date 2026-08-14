package org.zstack.sdk;

import org.zstack.sdk.PciDeviceType;
import org.zstack.sdk.PciDeviceSpecState;

public class PciDeviceSpecInventory  {

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

    public java.lang.String vendorId;
    public void setVendorId(java.lang.String vendorId) {
        this.vendorId = vendorId;
    }
    public java.lang.String getVendorId() {
        return this.vendorId;
    }

    public java.lang.String vendor;
    public void setVendor(java.lang.String vendor) {
        this.vendor = vendor;
    }
    public java.lang.String getVendor() {
        return this.vendor;
    }

    public java.lang.String deviceId;
    public void setDeviceId(java.lang.String deviceId) {
        this.deviceId = deviceId;
    }
    public java.lang.String getDeviceId() {
        return this.deviceId;
    }

    public java.lang.String device;
    public void setDevice(java.lang.String device) {
        this.device = device;
    }
    public java.lang.String getDevice() {
        return this.device;
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

    public java.lang.Integer maxPartNum;
    public void setMaxPartNum(java.lang.Integer maxPartNum) {
        this.maxPartNum = maxPartNum;
    }
    public java.lang.Integer getMaxPartNum() {
        return this.maxPartNum;
    }

    public PciDeviceType type;
    public void setType(PciDeviceType type) {
        this.type = type;
    }
    public PciDeviceType getType() {
        return this.type;
    }

    public PciDeviceSpecState state;
    public void setState(PciDeviceSpecState state) {
        this.state = state;
    }
    public PciDeviceSpecState getState() {
        return this.state;
    }

    public java.lang.Boolean isVirtual;
    public void setIsVirtual(java.lang.Boolean isVirtual) {
        this.isVirtual = isVirtual;
    }
    public java.lang.Boolean getIsVirtual() {
        return this.isVirtual;
    }

    public java.lang.String romVersion;
    public void setRomVersion(java.lang.String romVersion) {
        this.romVersion = romVersion;
    }
    public java.lang.String getRomVersion() {
        return this.romVersion;
    }

    public java.lang.String romMd5sum;
    public void setRomMd5sum(java.lang.String romMd5sum) {
        this.romMd5sum = romMd5sum;
    }
    public java.lang.String getRomMd5sum() {
        return this.romMd5sum;
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
