package org.zstack.sdk;



public class LoadBalancerServerGroupInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String loadBalancerUuid;
    public void setLoadBalancerUuid(java.lang.String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
    public java.lang.String getLoadBalancerUuid() {
        return this.loadBalancerUuid;
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

    public java.util.List listenerServerGroupRefs;
    public void setListenerServerGroupRefs(java.util.List listenerServerGroupRefs) {
        this.listenerServerGroupRefs = listenerServerGroupRefs;
    }
    public java.util.List getListenerServerGroupRefs() {
        return this.listenerServerGroupRefs;
    }

    public java.util.List serverIps;
    public void setServerIps(java.util.List serverIps) {
        this.serverIps = serverIps;
    }
    public java.util.List getServerIps() {
        return this.serverIps;
    }

    public java.util.List vmNicRefs;
    public void setVmNicRefs(java.util.List vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }
    public java.util.List getVmNicRefs() {
        return this.vmNicRefs;
    }

}
