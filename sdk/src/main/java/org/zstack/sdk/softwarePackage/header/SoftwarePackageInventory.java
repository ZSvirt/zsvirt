package org.zstack.sdk.softwarePackage.header;



public class SoftwarePackageInventory  {

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

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String managementNodeUuid;
    public void setManagementNodeUuid(java.lang.String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
    public java.lang.String getManagementNodeUuid() {
        return this.managementNodeUuid;
    }

    public java.lang.String installPath;
    public void setInstallPath(java.lang.String installPath) {
        this.installPath = installPath;
    }
    public java.lang.String getInstallPath() {
        return this.installPath;
    }

    public java.lang.String unzipInstallPath;
    public void setUnzipInstallPath(java.lang.String unzipInstallPath) {
        this.unzipInstallPath = unzipInstallPath;
    }
    public java.lang.String getUnzipInstallPath() {
        return this.unzipInstallPath;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String md5sum;
    public void setMd5sum(java.lang.String md5sum) {
        this.md5sum = md5sum;
    }
    public java.lang.String getMd5sum() {
        return this.md5sum;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public long size;
    public void setSize(long size) {
        this.size = size;
    }
    public long getSize() {
        return this.size;
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
