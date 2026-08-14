package org.zstack.sdk;



public class SchedulerJobInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String targetResourceUuid;
    public void setTargetResourceUuid(java.lang.String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }
    public java.lang.String getTargetResourceUuid() {
        return this.targetResourceUuid;
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

    public java.lang.String jobData;
    public void setJobData(java.lang.String jobData) {
        this.jobData = jobData;
    }
    public java.lang.String getJobData() {
        return this.jobData;
    }

    public java.lang.String jobClassName;
    public void setJobClassName(java.lang.String jobClassName) {
        this.jobClassName = jobClassName;
    }
    public java.lang.String getJobClassName() {
        return this.jobClassName;
    }

    public java.util.List triggersUuid;
    public void setTriggersUuid(java.util.List triggersUuid) {
        this.triggersUuid = triggersUuid;
    }
    public java.util.List getTriggersUuid() {
        return this.triggersUuid;
    }

    public java.util.List schedulerJobGroupUuids;
    public void setSchedulerJobGroupUuids(java.util.List schedulerJobGroupUuids) {
        this.schedulerJobGroupUuids = schedulerJobGroupUuids;
    }
    public java.util.List getSchedulerJobGroupUuids() {
        return this.schedulerJobGroupUuids;
    }

}
