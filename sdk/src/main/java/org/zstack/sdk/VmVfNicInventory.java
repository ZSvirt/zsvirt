package org.zstack.sdk;



public class VmVfNicInventory extends org.zstack.sdk.VmNicInventory {

    public java.lang.String pciDeviceUuid;
    public void setPciDeviceUuid(java.lang.String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }
    public java.lang.String getPciDeviceUuid() {
        return this.pciDeviceUuid;
    }

    public java.lang.String haState;
    public void setHaState(java.lang.String haState) {
        this.haState = haState;
    }
    public java.lang.String getHaState() {
        return this.haState;
    }

}
