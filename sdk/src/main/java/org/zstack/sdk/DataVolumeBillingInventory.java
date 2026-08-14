package org.zstack.sdk;



public class DataVolumeBillingInventory extends org.zstack.sdk.BillingInventory {

    public long volumeSize;
    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }
    public long getVolumeSize() {
        return this.volumeSize;
    }

}
