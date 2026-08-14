package org.zstack.sdk;

import org.zstack.sdk.ImagePackageState;

public class ImagePackageInventory  {

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

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String vmUuid;
    public void setVmUuid(java.lang.String vmUuid) {
        this.vmUuid = vmUuid;
    }
    public java.lang.String getVmUuid() {
        return this.vmUuid;
    }

    public java.lang.String backupStorageUuid;
    public void setBackupStorageUuid(java.lang.String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
    public java.lang.String getBackupStorageUuid() {
        return this.backupStorageUuid;
    }

    public ImagePackageState state;
    public void setState(ImagePackageState state) {
        this.state = state;
    }
    public ImagePackageState getState() {
        return this.state;
    }

    public java.lang.String exportUrl;
    public void setExportUrl(java.lang.String exportUrl) {
        this.exportUrl = exportUrl;
    }
    public java.lang.String getExportUrl() {
        return this.exportUrl;
    }

    public java.lang.String md5Sum;
    public void setMd5Sum(java.lang.String md5Sum) {
        this.md5Sum = md5Sum;
    }
    public java.lang.String getMd5Sum() {
        return this.md5Sum;
    }

    public java.lang.String format;
    public void setFormat(java.lang.String format) {
        this.format = format;
    }
    public java.lang.String getFormat() {
        return this.format;
    }

    public java.lang.Long size;
    public void setSize(java.lang.Long size) {
        this.size = size;
    }
    public java.lang.Long getSize() {
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
