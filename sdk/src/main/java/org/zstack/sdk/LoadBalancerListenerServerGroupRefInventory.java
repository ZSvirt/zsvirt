package org.zstack.sdk;



public class LoadBalancerListenerServerGroupRefInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String listenerUuid;
    public void setListenerUuid(java.lang.String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }
    public java.lang.String getListenerUuid() {
        return this.listenerUuid;
    }

    public java.lang.String serverGroupUuid;
    public void setServerGroupUuid(java.lang.String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
    public java.lang.String getServerGroupUuid() {
        return this.serverGroupUuid;
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
