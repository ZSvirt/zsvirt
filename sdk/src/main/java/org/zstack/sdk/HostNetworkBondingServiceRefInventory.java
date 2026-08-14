package org.zstack.sdk;

import org.zstack.sdk.HostNetworkInterfaceServiceType;

public class HostNetworkBondingServiceRefInventory  {

    public java.lang.String bondingUuid;
    public void setBondingUuid(java.lang.String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }
    public java.lang.String getBondingUuid() {
        return this.bondingUuid;
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
