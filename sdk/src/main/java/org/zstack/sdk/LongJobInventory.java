package org.zstack.sdk;

import org.zstack.sdk.LongJobState;

public class LongJobInventory  {

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

    public java.lang.String apiId;
    public void setApiId(java.lang.String apiId) {
        this.apiId = apiId;
    }
    public java.lang.String getApiId() {
        return this.apiId;
    }

    public java.lang.String jobName;
    public void setJobName(java.lang.String jobName) {
        this.jobName = jobName;
    }
    public java.lang.String getJobName() {
        return this.jobName;
    }

    public java.lang.String jobData;
    public void setJobData(java.lang.String jobData) {
        this.jobData = jobData;
    }
    public java.lang.String getJobData() {
        return this.jobData;
    }

    public java.lang.String jobResult;
    public void setJobResult(java.lang.String jobResult) {
        this.jobResult = jobResult;
    }
    public java.lang.String getJobResult() {
        return this.jobResult;
    }

    public LongJobState state;
    public void setState(LongJobState state) {
        this.state = state;
    }
    public LongJobState getState() {
        return this.state;
    }

    public java.lang.String targetResourceUuid;
    public void setTargetResourceUuid(java.lang.String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }
    public java.lang.String getTargetResourceUuid() {
        return this.targetResourceUuid;
    }

    public java.lang.String managementNodeUuid;
    public void setManagementNodeUuid(java.lang.String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
    public java.lang.String getManagementNodeUuid() {
        return this.managementNodeUuid;
    }

    public java.lang.String parentUuid;
    public void setParentUuid(java.lang.String parentUuid) {
        this.parentUuid = parentUuid;
    }
    public java.lang.String getParentUuid() {
        return this.parentUuid;
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

    public java.lang.Long executeTime;
    public void setExecuteTime(java.lang.Long executeTime) {
        this.executeTime = executeTime;
    }
    public java.lang.Long getExecuteTime() {
        return this.executeTime;
    }

}
