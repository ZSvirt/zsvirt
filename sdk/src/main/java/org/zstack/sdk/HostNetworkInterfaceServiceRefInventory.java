package org.zstack.sdk;

import org.zstack.sdk.HostNetworkInterfaceServiceType;

public class HostNetworkInterfaceServiceRefInventory  {

    public java.lang.String interfaceUuid;
    public void setInterfaceUuid(java.lang.String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }
    public java.lang.String getInterfaceUuid() {
        return this.interfaceUuid;
    }

    public java.lang.Integer vlanId;
    public void setVlanId(java.lang.Integer vlanId) {
        this.vlanId = vlanId;
    }
    public java.lang.Integer getVlanId() {
        return this.vlanId;
    }

    public HostNetworkInterfaceServiceType serviceType;
    public void setServiceType(HostNetworkInterfaceServiceType serviceType) {
        this.serviceType = serviceType;
    }
    public HostNetworkInterfaceServiceType getServiceType() {
        return this.serviceType;
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

}
