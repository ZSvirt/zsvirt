package org.zstack.sdk;



public class L3NetworkHostRouteInventory  {

    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String l3NetworkUuid;
    public void setL3NetworkUuid(java.lang.String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public java.lang.String getL3NetworkUuid() {
        return this.l3NetworkUuid;
    }

    public java.lang.String prefix;
    public void setPrefix(java.lang.String prefix) {
        this.prefix = prefix;
    }
    public java.lang.String getPrefix() {
        return this.prefix;
    }

    public java.lang.String nexthop;
    public void setNexthop(java.lang.String nexthop) {
        this.nexthop = nexthop;
    }
    public java.lang.String getNexthop() {
        return this.nexthop;
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
