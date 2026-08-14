package org.zstack.sdk;

import org.zstack.sdk.PortGroupVlanMode;

public class PortGroupInventory extends org.zstack.sdk.L3NetworkInventory {

    public java.lang.String vSwitchUuid;
    public void setVSwitchUuid(java.lang.String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }
    public java.lang.String getVSwitchUuid() {
        return this.vSwitchUuid;
    }

    public PortGroupVlanMode vlanMode;
    public void setVlanMode(PortGroupVlanMode vlanMode) {
        this.vlanMode = vlanMode;
    }
    public PortGroupVlanMode getVlanMode() {
        return this.vlanMode;
    }

    public java.lang.Integer vlanId;
    public void setVlanId(java.lang.Integer vlanId) {
        this.vlanId = vlanId;
    }
    public java.lang.Integer getVlanId() {
        return this.vlanId;
    }

    public java.lang.String vlanRanges;
    public void setVlanRanges(java.lang.String vlanRanges) {
        this.vlanRanges = vlanRanges;
    }
    public java.lang.String getVlanRanges() {
        return this.vlanRanges;
    }

}
