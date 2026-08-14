package org.zstack.sdk;



public class SchedulerJobHistoryInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String triggerUuid;
    public void setTriggerUuid(java.lang.String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }
    public java.lang.String getTriggerUuid() {
        return this.triggerUuid;
    }

    public java.lang.String schedulerJobUuid;
    public void setSchedulerJobUuid(java.lang.String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }
    public java.lang.String getSchedulerJobUuid() {
        return this.schedulerJobUuid;
    }

    public java.lang.String schedulerJobGroupUuid;
    public void setSchedulerJobGroupUuid(java.lang.String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }
    public java.lang.String getSchedulerJobGroupUuid() {
        return this.schedulerJobGroupUuid;
    }

    public java.lang.String jobType;
    public void setJobType(java.lang.String jobType) {
        this.jobType = jobType;
    }
    public java.lang.String getJobType() {
        return this.jobType;
    }

    public java.sql.Timestamp startTime;
    public void setStartTime(java.sql.Timestamp startTime) {
        this.startTime = startTime;
    }
    public java.sql.Timestamp getStartTime() {
        return this.startTime;
    }

    public long executeTime;
    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }
    public long getExecuteTime() {
        return this.executeTime;
    }

    public java.lang.String targetResourceUuid;
    public void setTargetResourceUuid(java.lang.String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }
    public java.lang.String getTargetResourceUuid() {
        return this.targetResourceUuid;
    }

    public java.lang.String requestDump;
    public void setRequestDump(java.lang.String requestDump) {
        this.requestDump = requestDump;
    }
    public java.lang.String getRequestDump() {
        return this.requestDump;
    }

    public java.lang.String resultDump;
    public void setResultDump(java.lang.String resultDump) {
        this.resultDump = resultDump;
    }
    public java.lang.String getResultDump() {
        return this.resultDump;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

    public java.lang.String fireInstanceId;
    public void setFireInstanceId(java.lang.String fireInstanceId) {
        this.fireInstanceId = fireInstanceId;
    }
    public java.lang.String getFireInstanceId() {
        return this.fireInstanceId;
    }

}
