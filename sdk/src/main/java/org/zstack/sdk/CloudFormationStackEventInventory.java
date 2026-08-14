package org.zstack.sdk;



public class CloudFormationStackEventInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String action;
    public void setAction(java.lang.String action) {
        this.action = action;
    }
    public java.lang.String getAction() {
        return this.action;
    }

    public java.lang.String content;
    public void setContent(java.lang.String content) {
        this.content = content;
    }
    public java.lang.String getContent() {
        return this.content;
    }

    public java.lang.String resourceName;
    public void setResourceName(java.lang.String resourceName) {
        this.resourceName = resourceName;
    }
    public java.lang.String getResourceName() {
        return this.resourceName;
    }

    public java.lang.String actionStatus;
    public void setActionStatus(java.lang.String actionStatus) {
        this.actionStatus = actionStatus;
    }
    public java.lang.String getActionStatus() {
        return this.actionStatus;
    }

    public java.lang.String stackUuid;
    public void setStackUuid(java.lang.String stackUuid) {
        this.stackUuid = stackUuid;
    }
    public java.lang.String getStackUuid() {
        return this.stackUuid;
    }

    public java.lang.String duration;
    public void setDuration(java.lang.String duration) {
        this.duration = duration;
    }
    public java.lang.String getDuration() {
        return this.duration;
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
