package org.zstack.sdk;



public class NvmeTargetInventory  {

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

    public java.lang.String nqn;
    public void setNqn(java.lang.String nqn) {
        this.nqn = nqn;
    }
    public java.lang.String getNqn() {
        return this.nqn;
    }

    public java.lang.String nvmeServerUuid;
    public void setNvmeServerUuid(java.lang.String nvmeServerUuid) {
        this.nvmeServerUuid = nvmeServerUuid;
    }
    public java.lang.String getNvmeServerUuid() {
        return this.nvmeServerUuid;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.util.List nvmeLuns;
    public void setNvmeLuns(java.util.List nvmeLuns) {
        this.nvmeLuns = nvmeLuns;
    }
    public java.util.List getNvmeLuns() {
        return this.nvmeLuns;
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
