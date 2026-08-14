package org.zstack.sdk;

import org.zstack.sdk.HostNetworkInterfaceLldpRefInventory;

public class HostNetworkInterfaceLldpInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String interfaceUuid;
    public void setInterfaceUuid(java.lang.String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }
    public java.lang.String getInterfaceUuid() {
        return this.interfaceUuid;
    }

    public java.lang.String mode;
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }
    public java.lang.String getMode() {
        return this.mode;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public HostNetworkInterfaceLldpRefInventory neighborDevice;
    public void setNeighborDevice(HostNetworkInterfaceLldpRefInventory neighborDevice) {
        this.neighborDevice = neighborDevice;
    }
    public HostNetworkInterfaceLldpRefInventory getNeighborDevice() {
        return this.neighborDevice;
    }

}
