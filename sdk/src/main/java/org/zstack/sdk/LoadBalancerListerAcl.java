package org.zstack.sdk;



public class LoadBalancerListerAcl  {

    public java.lang.String aclUuid;
    public void setAclUuid(java.lang.String aclUuid) {
        this.aclUuid = aclUuid;
    }
    public java.lang.String getAclUuid() {
        return this.aclUuid;
    }

    public java.lang.String listenerUuid;
    public void setListenerUuid(java.lang.String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }
    public java.lang.String getListenerUuid() {
        return this.listenerUuid;
    }

    public java.util.List serverGroupUuids;
    public void setServerGroupUuids(java.util.List serverGroupUuids) {
        this.serverGroupUuids = serverGroupUuids;
    }
    public java.util.List getServerGroupUuids() {
        return this.serverGroupUuids;
    }

}
