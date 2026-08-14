package org.zstack.sdk.attribute.entity;

import org.zstack.sdk.attribute.entity.ResourceAttributeKeyInventory;

public class ResourceAttributeValueInventory  {

    public java.lang.String keyUuid;
    public void setKeyUuid(java.lang.String keyUuid) {
        this.keyUuid = keyUuid;
    }
    public java.lang.String getKeyUuid() {
        return this.keyUuid;
    }

    public ResourceAttributeKeyInventory key;
    public void setKey(ResourceAttributeKeyInventory key) {
        this.key = key;
    }
    public ResourceAttributeKeyInventory getKey() {
        return this.key;
    }

    public java.lang.String value;
    public void setValue(java.lang.String value) {
        this.value = value;
    }
    public java.lang.String getValue() {
        return this.value;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.lang.String resourceType;
    public void setResourceType(java.lang.String resourceType) {
        this.resourceType = resourceType;
    }
    public java.lang.String getResourceType() {
        return this.resourceType;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

}
