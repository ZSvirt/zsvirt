package org.zstack.sdk;

import org.zstack.sdk.MdevDeviceType;
import org.zstack.sdk.MdevDeviceSpecState;

public class MdevDeviceSpecInventory  {

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

    public java.lang.String specification;
    public void setSpecification(java.lang.String specification) {
        this.specification = specification;
    }
    public java.lang.String getSpecification() {
        return this.specification;
    }

    public MdevDeviceType type;
    public void setType(MdevDeviceType type) {
        this.type = type;
    }
    public MdevDeviceType getType() {
        return this.type;
    }

    public MdevDeviceSpecState state;
    public void setState(MdevDeviceSpecState state) {
        this.state = state;
    }
    public MdevDeviceSpecState getState() {
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
