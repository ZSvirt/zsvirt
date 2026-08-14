package org.zstack.sdk;



public class PubIpVmNicBandwidthSpending extends org.zstack.sdk.SpendingDetails {

    public java.lang.String vmNicIp;
    public void setVmNicIp(java.lang.String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }
    public java.lang.String getVmNicIp() {
        return this.vmNicIp;
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
