package org.zstack.sdk;



public class NetworkServiceProviderInventory  {

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

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
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

    public java.util.Set networkServiceTypes;
    public void setNetworkServiceTypes(java.util.Set networkServiceTypes) {
        this.networkServiceTypes = networkServiceTypes;
    }
    public java.util.Set getNetworkServiceTypes() {
        return this.networkServiceTypes;
    }

    public java.util.Set attachedL2NetworkUuids;
    public void setAttachedL2NetworkUuids(java.util.Set attachedL2NetworkUuids) {
        this.attachedL2NetworkUuids = attachedL2NetworkUuids;
    }
    public java.util.Set getAttachedL2NetworkUuids() {
        return this.attachedL2NetworkUuids;
    }

}
