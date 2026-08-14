package org.zstack.sdk;



public class VpcRouterDnsInventory  {

    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String vpcRouterUuid;
    public void setVpcRouterUuid(java.lang.String vpcRouterUuid) {
        this.vpcRouterUuid = vpcRouterUuid;
    }
    public java.lang.String getVpcRouterUuid() {
        return this.vpcRouterUuid;
    }

    public java.lang.String dns;
    public void setDns(java.lang.String dns) {
        this.dns = dns;
    }
    public java.lang.String getDns() {
        return this.dns;
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
