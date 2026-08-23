package org.zstack.sdk.allocator.domain.entity;



public class AllocatorDomainInventory  {

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

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
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

    public java.util.List rules;
    public void setRules(java.util.List rules) {
        this.rules = rules;
    }
    public java.util.List getRules() {
        return this.rules;
    }

    public java.util.List scenes;
    public void setScenes(java.util.List scenes) {
        this.scenes = scenes;
    }
    public java.util.List getScenes() {
        return this.scenes;
    }

}
