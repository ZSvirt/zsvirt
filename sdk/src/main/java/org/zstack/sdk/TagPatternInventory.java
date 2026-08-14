package org.zstack.sdk;

import org.zstack.sdk.TagPatternType;

public class TagPatternInventory  {

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

    public java.lang.String value;
    public void setValue(java.lang.String value) {
        this.value = value;
    }
    public java.lang.String getValue() {
        return this.value;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String color;
    public void setColor(java.lang.String color) {
        this.color = color;
    }
    public java.lang.String getColor() {
        return this.color;
    }

    public TagPatternType type;
    public void setType(TagPatternType type) {
        this.type = type;
    }
    public TagPatternType getType() {
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

}
