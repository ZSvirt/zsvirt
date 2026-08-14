package org.zstack.sdk;



public class PubIpVipBandwidthInBillingInventory extends org.zstack.sdk.BillingInventory {

    public java.lang.String vipIp;
    public void setVipIp(java.lang.String vipIp) {
        this.vipIp = vipIp;
    }
    public java.lang.String getVipIp() {
        return this.vipIp;
    }

    public long bandwidthSize;
    public void setBandwidthSize(long bandwidthSize) {
        this.bandwidthSize = bandwidthSize;
    }
    public long getBandwidthSize() {
        return this.bandwidthSize;
    }

}
