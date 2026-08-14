package org.zstack.sdk;

import org.zstack.sdk.EthernetVfStatus;

public class EthernetVfPciDeviceInventory extends org.zstack.sdk.PciDeviceInventory {

    public java.lang.String hostDevUuid;
    public void setHostDevUuid(java.lang.String hostDevUuid) {
        this.hostDevUuid = hostDevUuid;
    }
    public java.lang.String getHostDevUuid() {
        return this.hostDevUuid;
    }

    public java.lang.String interfaceName;
    public void setInterfaceName(java.lang.String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public java.lang.String getInterfaceName() {
        return this.interfaceName;
    }

    public java.lang.String vmUuid;
    public void setVmUuid(java.lang.String vmUuid) {
        this.vmUuid = vmUuid;
    }
    public java.lang.String getVmUuid() {
        return this.vmUuid;
    }

    public java.lang.String l3NetworkUuid;
    public void setL3NetworkUuid(java.lang.String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public java.lang.String getL3NetworkUuid() {
        return this.l3NetworkUuid;
    }

    public EthernetVfStatus vfStatus;
    public void setVfStatus(EthernetVfStatus vfStatus) {
        this.vfStatus = vfStatus;
    }
    public EthernetVfStatus getVfStatus() {
        return this.vfStatus;
    }

}
