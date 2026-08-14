package org.zstack.sdk;



public class LoadBalancerServerGroupServerIpInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String serverGroupUuid;
    public void setServerGroupUuid(java.lang.String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
    public java.lang.String getServerGroupUuid() {
        return this.serverGroupUuid;
    }

    public java.lang.String ipAddress;
    public void setIpAddress(java.lang.String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public java.lang.String getIpAddress() {
        return this.ipAddress;
    }

    public java.lang.Long weight;
    public void setWeight(java.lang.Long weight) {
        this.weight = weight;
    }
    public java.lang.Long getWeight() {
        return this.weight;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
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
