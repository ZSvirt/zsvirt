package org.zstack.sdk;

import org.zstack.sdk.UplinkGroupType;

public class UplinkGroupInventory extends org.zstack.sdk.L2NetworkHostRefInventory {

    public java.lang.String interfaceName;
    public void setInterfaceName(java.lang.String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public java.lang.String getInterfaceName() {
        return this.interfaceName;
    }

    public UplinkGroupType type;
    public void setType(UplinkGroupType type) {
        this.type = type;
    }
    public UplinkGroupType getType() {
        return this.type;
    }

    public java.lang.String bondingUuid;
    public void setBondingUuid(java.lang.String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }
    public java.lang.String getBondingUuid() {
        return this.bondingUuid;
    }

    public java.lang.String interfaceUuid;
    public void setInterfaceUuid(java.lang.String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }
    public java.lang.String getInterfaceUuid() {
        return this.interfaceUuid;
    }

}
