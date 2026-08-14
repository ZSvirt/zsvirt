package org.zstack.sdk;

import org.zstack.sdk.PciDeviceType;
import org.zstack.sdk.PciDeviceState;
import org.zstack.sdk.PciDeviceStatus;
import org.zstack.sdk.PciDeviceVirtStatus;
import org.zstack.sdk.PciDevicePassThroughState;
import org.zstack.sdk.PciDeviceChooser;
import org.zstack.sdk.PciDeviceMetaData;

public class PciDeviceInventory  {

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

    public java.lang.String parentUuid;
    public void setParentUuid(java.lang.String parentUuid) {
        this.parentUuid = parentUuid;
    }
    public java.lang.String getParentUuid() {
        return this.parentUuid;
    }

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

    public PciDeviceType type;
    public void setType(PciDeviceType type) {
        this.type = type;
    }
    public PciDeviceType getType() {
        return this.type;
    }

    public PciDeviceState state;
    public void setState(PciDeviceState state) {
        this.state = state;
    }
    public PciDeviceState getState() {
        return this.state;
    }

    public PciDeviceStatus status;
    public void setStatus(PciDeviceStatus status) {
        this.status = status;
    }
    public PciDeviceStatus getStatus() {
        return this.status;
    }

    public PciDeviceVirtStatus virtStatus;
    public void setVirtStatus(PciDeviceVirtStatus virtStatus) {
        this.virtStatus = virtStatus;
    }
    public PciDeviceVirtStatus getVirtStatus() {
        return this.virtStatus;
    }

    public PciDevicePassThroughState passThroughState;
    public void setPassThroughState(PciDevicePassThroughState passThroughState) {
        this.passThroughState = passThroughState;
    }
    public PciDevicePassThroughState getPassThroughState() {
        return this.passThroughState;
    }

    public PciDeviceChooser chooser;
    public void setChooser(PciDeviceChooser chooser) {
        this.chooser = chooser;
    }
    public PciDeviceChooser getChooser() {
        return this.chooser;
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

    public java.lang.String pciDeviceAddress;
    public void setPciDeviceAddress(java.lang.String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }
    public java.lang.String getPciDeviceAddress() {
        return this.pciDeviceAddress;
    }

    public java.lang.String iommuGroup;
    public void setIommuGroup(java.lang.String iommuGroup) {
        this.iommuGroup = iommuGroup;
    }
    public java.lang.String getIommuGroup() {
        return this.iommuGroup;
    }

    public PciDeviceMetaData metaData;
    public void setMetaData(PciDeviceMetaData metaData) {
        this.metaData = metaData;
    }
    public PciDeviceMetaData getMetaData() {
        return this.metaData;
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

    public java.util.List matchedPciDeviceOfferingRef;
    public void setMatchedPciDeviceOfferingRef(java.util.List matchedPciDeviceOfferingRef) {
        this.matchedPciDeviceOfferingRef = matchedPciDeviceOfferingRef;
    }
    public java.util.List getMatchedPciDeviceOfferingRef() {
        return this.matchedPciDeviceOfferingRef;
    }

    public java.util.List mdevSpecRefs;
    public void setMdevSpecRefs(java.util.List mdevSpecRefs) {
        this.mdevSpecRefs = mdevSpecRefs;
    }
    public java.util.List getMdevSpecRefs() {
        return this.mdevSpecRefs;
    }

}
