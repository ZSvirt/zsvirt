package org.zstack.sdk;



public class L2VirtualSwitchNetworkInventory extends org.zstack.sdk.L2NetworkInventory {

    public java.lang.Boolean isDistributed;
    public void setIsDistributed(java.lang.Boolean isDistributed) {
        this.isDistributed = isDistributed;
    }
    public java.lang.Boolean getIsDistributed() {
        return this.isDistributed;
    }

    public java.lang.Integer vSwitchIndex;
    public void setVSwitchIndex(java.lang.Integer vSwitchIndex) {
        this.vSwitchIndex = vSwitchIndex;
    }
    public java.lang.Integer getVSwitchIndex() {
        return this.vSwitchIndex;
    }

    public java.util.List portGroups;
    public void setPortGroups(java.util.List portGroups) {
        this.portGroups = portGroups;
    }
    public java.util.List getPortGroups() {
        return this.portGroups;
    }

}
