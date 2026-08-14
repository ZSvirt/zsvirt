package org.zstack.sdk;



public class JsonLabelInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String labelKey;
    public void setLabelKey(java.lang.String labelKey) {
        this.labelKey = labelKey;
    }
    public java.lang.String getLabelKey() {
        return this.labelKey;
    }

    public java.lang.String labelValue;
    public void setLabelValue(java.lang.String labelValue) {
        this.labelValue = labelValue;
    }
    public java.lang.String getLabelValue() {
        return this.labelValue;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
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
