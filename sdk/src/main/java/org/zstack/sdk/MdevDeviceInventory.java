package org.zstack.sdk;

import org.zstack.sdk.MdevDeviceType;
import org.zstack.sdk.MdevDeviceState;
import org.zstack.sdk.MdevDeviceStatus;
import org.zstack.sdk.MdevDeviceChooser;

public class MdevDeviceInventory  {

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

    public java.lang.String parentUuid;
    public void setParentUuid(java.lang.String parentUuid) {
        this.parentUuid = parentUuid;
    }
    public java.lang.String getParentUuid() {
        return this.parentUuid;
    }

    public java.lang.String mttyUuid;
    public void setMttyUuid(java.lang.String mttyUuid) {
        this.mttyUuid = mttyUuid;
    }
    public java.lang.String getMttyUuid() {
        return this.mttyUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String mdevSpecUuid;
    public void setMdevSpecUuid(java.lang.String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }
    public java.lang.String getMdevSpecUuid() {
        return this.mdevSpecUuid;
    }

    public MdevDeviceType type;
    public void setType(MdevDeviceType type) {
        this.type = type;
    }
    public MdevDeviceType getType() {
        return this.type;
    }

    public MdevDeviceState state;
    public void setState(MdevDeviceState state) {
        this.state = state;
    }
    public MdevDeviceState getState() {
        return this.state;
    }

    public MdevDeviceStatus status;
    public void setStatus(MdevDeviceStatus status) {
        this.status = status;
    }
    public MdevDeviceStatus getStatus() {
        return this.status;
    }

    public MdevDeviceChooser chooser;
    public void setChooser(MdevDeviceChooser chooser) {
        this.chooser = chooser;
    }
    public MdevDeviceChooser getChooser() {
        return this.chooser;
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

    public java.lang.String vendor;
    public void setVendor(java.lang.String vendor) {
        this.vendor = vendor;
    }
    public java.lang.String getVendor() {
        return this.vendor;
    }

}
