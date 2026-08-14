package org.zstack.sdk;

import org.zstack.sdk.PciDeviceMetaData;

public class PciDeviceOfferingInstanceOfferingRefInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String instanceOfferingUuid;
    public void setInstanceOfferingUuid(java.lang.String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }
    public java.lang.String getInstanceOfferingUuid() {
        return this.instanceOfferingUuid;
    }

    public java.lang.String pciDeviceOfferingUuid;
    public void setPciDeviceOfferingUuid(java.lang.String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }
    public java.lang.String getPciDeviceOfferingUuid() {
        return this.pciDeviceOfferingUuid;
    }

    public PciDeviceMetaData metadata;
    public void setMetadata(PciDeviceMetaData metadata) {
        this.metadata = metadata;
    }
    public PciDeviceMetaData getMetadata() {
        return this.metadata;
    }

    public java.lang.Integer pciDeviceCount;
    public void setPciDeviceCount(java.lang.Integer pciDeviceCount) {
        this.pciDeviceCount = pciDeviceCount;
    }
    public java.lang.Integer getPciDeviceCount() {
        return this.pciDeviceCount;
    }

}
