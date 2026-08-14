package org.zstack.sdk;



public class AutoScalingGroupInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String scalingResourceType;
    public void setScalingResourceType(java.lang.String scalingResourceType) {
        this.scalingResourceType = scalingResourceType;
    }
    public java.lang.String getScalingResourceType() {
        return this.scalingResourceType;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.Long defaultCooldown;
    public void setDefaultCooldown(java.lang.Long defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }
    public java.lang.Long getDefaultCooldown() {
        return this.defaultCooldown;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.Integer minResourceSize;
    public void setMinResourceSize(java.lang.Integer minResourceSize) {
        this.minResourceSize = minResourceSize;
    }
    public java.lang.Integer getMinResourceSize() {
        return this.minResourceSize;
    }

    public java.lang.Integer maxResourceSize;
    public void setMaxResourceSize(java.lang.Integer maxResourceSize) {
        this.maxResourceSize = maxResourceSize;
    }
    public java.lang.Integer getMaxResourceSize() {
        return this.maxResourceSize;
    }

    public java.lang.String removalPolicy;
    public void setRemovalPolicy(java.lang.String removalPolicy) {
        this.removalPolicy = removalPolicy;
    }
    public java.lang.String getRemovalPolicy() {
        return this.removalPolicy;
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

    public java.util.List attachedTemplates;
    public void setAttachedTemplates(java.util.List attachedTemplates) {
        this.attachedTemplates = attachedTemplates;
    }
    public java.util.List getAttachedTemplates() {
        return this.attachedTemplates;
    }

    public java.util.List systemTags;
    public void setSystemTags(java.util.List systemTags) {
        this.systemTags = systemTags;
    }
    public java.util.List getSystemTags() {
        return this.systemTags;
    }

}
