package org.zstack.sdk;



public class NvmeServerInventory  {

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

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String transport;
    public void setTransport(java.lang.String transport) {
        this.transport = transport;
    }
    public java.lang.String getTransport() {
        return this.transport;
    }

    public java.util.List nvmeTargets;
    public void setNvmeTargets(java.util.List nvmeTargets) {
        this.nvmeTargets = nvmeTargets;
    }
    public java.util.List getNvmeTargets() {
        return this.nvmeTargets;
    }

    public java.util.List nvmeClusterRefs;
    public void setNvmeClusterRefs(java.util.List nvmeClusterRefs) {
        this.nvmeClusterRefs = nvmeClusterRefs;
    }
    public java.util.List getNvmeClusterRefs() {
        return this.nvmeClusterRefs;
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
