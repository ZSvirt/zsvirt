package org.zstack.sdk;



public class RootVolumeBillingInventory extends org.zstack.sdk.BillingInventory {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public long volumeSize;
    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }
    public long getVolumeSize() {
        return this.volumeSize;
    }

}
