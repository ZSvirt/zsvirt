package org.zstack.sdk;



public class PubIpVmNicBandwidthInBillingInventory extends org.zstack.sdk.BillingInventory {

    public java.lang.String vmNicIp;
    public void setVmNicIp(java.lang.String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }
    public java.lang.String getVmNicIp() {
        return this.vmNicIp;
    }

    public long bandwidthSize;
    public void setBandwidthSize(long bandwidthSize) {
        this.bandwidthSize = bandwidthSize;
    }
    public long getBandwidthSize() {
        return this.bandwidthSize;
    }

}
