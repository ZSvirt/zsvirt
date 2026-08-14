package org.zstack.sdk;



public class NvmeServerClusterRefInventory  {

    public java.lang.String nvmeServerUuid;
    public void setNvmeServerUuid(java.lang.String nvmeServerUuid) {
        this.nvmeServerUuid = nvmeServerUuid;
    }
    public java.lang.String getNvmeServerUuid() {
        return this.nvmeServerUuid;
    }

    public java.lang.String clusterUuid;
    public void setClusterUuid(java.lang.String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
    public java.lang.String getClusterUuid() {
        return this.clusterUuid;
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
