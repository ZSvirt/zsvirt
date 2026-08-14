package org.zstack.sdk;



public class PubIpVipBandwidthSpending extends org.zstack.sdk.SpendingDetails {

    public java.lang.String vipIp;
    public void setVipIp(java.lang.String vipIp) {
        this.vipIp = vipIp;
    }
    public java.lang.String getVipIp() {
        return this.vipIp;
    }

    public java.util.List bandwidthInInventory;
    public void setBandwidthInInventory(java.util.List bandwidthInInventory) {
        this.bandwidthInInventory = bandwidthInInventory;
    }
    public java.util.List getBandwidthInInventory() {
        return this.bandwidthInInventory;
    }

    public java.util.List bandwidthOutInventory;
    public void setBandwidthOutInventory(java.util.List bandwidthOutInventory) {
        this.bandwidthOutInventory = bandwidthOutInventory;
    }
    public java.util.List getBandwidthOutInventory() {
        return this.bandwidthOutInventory;
    }

}
