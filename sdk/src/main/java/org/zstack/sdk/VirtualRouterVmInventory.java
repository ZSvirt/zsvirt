package org.zstack.sdk;



public class VirtualRouterVmInventory extends org.zstack.sdk.ApplianceVmInventory {

    public java.lang.String publicNetworkUuid;
    public void setPublicNetworkUuid(java.lang.String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }
    public java.lang.String getPublicNetworkUuid() {
        return this.publicNetworkUuid;
    }

    public java.util.List virtualRouterVips;
    public void setVirtualRouterVips(java.util.List virtualRouterVips) {
        this.virtualRouterVips = virtualRouterVips;
    }
    public java.util.List getVirtualRouterVips() {
        return this.virtualRouterVips;
    }

}
