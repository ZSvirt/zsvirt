package org.zstack.sdk;



public class ImageCacheInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public java.lang.String imageUuid;
    public void setImageUuid(java.lang.String imageUuid) {
        this.imageUuid = imageUuid;
    }
    public java.lang.String getImageUuid() {
        return this.imageUuid;
    }

    public java.lang.String installUrl;
    public void setInstallUrl(java.lang.String installUrl) {
        this.installUrl = installUrl;
    }
    public java.lang.String getInstallUrl() {
        return this.installUrl;
    }

    public java.lang.String mediaType;
    public void setMediaType(java.lang.String mediaType) {
        this.mediaType = mediaType;
    }
    public java.lang.String getMediaType() {
        return this.mediaType;
    }

    public long size;
    public void setSize(long size) {
        this.size = size;
    }
    public long getSize() {
        return this.size;
    }

    public java.lang.String md5sum;
    public void setMd5sum(java.lang.String md5sum) {
        this.md5sum = md5sum;
    }
    public java.lang.String getMd5sum() {
        return this.md5sum;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
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
