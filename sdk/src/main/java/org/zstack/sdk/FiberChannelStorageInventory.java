package org.zstack.sdk;



public class FiberChannelStorageInventory  {

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

    public java.lang.String wwnn;
    public void setWwnn(java.lang.String wwnn) {
        this.wwnn = wwnn;
    }
    public java.lang.String getWwnn() {
        return this.wwnn;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.util.List fiberChannelLuns;
    public void setFiberChannelLuns(java.util.List fiberChannelLuns) {
        this.fiberChannelLuns = fiberChannelLuns;
    }
    public java.util.List getFiberChannelLuns() {
        return this.fiberChannelLuns;
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
