package org.zstack.sdk.attribute.entity;



public class ResourceAttributeConstraintInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String keyUuid;
    public void setKeyUuid(java.lang.String keyUuid) {
        this.keyUuid = keyUuid;
    }
    public java.lang.String getKeyUuid() {
        return this.keyUuid;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String parameter;
    public void setParameter(java.lang.String parameter) {
        this.parameter = parameter;
    }
    public java.lang.String getParameter() {
        return this.parameter;
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
