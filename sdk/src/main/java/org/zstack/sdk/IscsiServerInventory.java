package org.zstack.sdk;



public class IscsiServerInventory  {

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

    public java.lang.String ip;
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }
    public java.lang.String getIp() {
        return this.ip;
    }

    public java.lang.Integer port;
    public void setPort(java.lang.Integer port) {
        this.port = port;
    }
    public java.lang.Integer getPort() {
        return this.port;
    }

    public java.lang.String chapUserName;
    public void setChapUserName(java.lang.String chapUserName) {
        this.chapUserName = chapUserName;
    }
    public java.lang.String getChapUserName() {
        return this.chapUserName;
    }

    public java.lang.String chapUserPassword;
    public void setChapUserPassword(java.lang.String chapUserPassword) {
        this.chapUserPassword = chapUserPassword;
    }
    public java.lang.String getChapUserPassword() {
        return this.chapUserPassword;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.util.List iscsiTargets;
    public void setIscsiTargets(java.util.List iscsiTargets) {
        this.iscsiTargets = iscsiTargets;
    }
    public java.util.List getIscsiTargets() {
        return this.iscsiTargets;
    }

    public java.util.List iscsiClusterRefs;
    public void setIscsiClusterRefs(java.util.List iscsiClusterRefs) {
        this.iscsiClusterRefs = iscsiClusterRefs;
    }
    public java.util.List getIscsiClusterRefs() {
        return this.iscsiClusterRefs;
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
