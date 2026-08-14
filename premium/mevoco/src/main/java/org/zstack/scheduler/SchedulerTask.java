package org.zstack.scheduler;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/8.
 */
public class SchedulerTask {
    private String type;
    private String jobUuid;
    private String triggerUuid;
    private Integer taskRepeatCount;
    private Integer taskInterval;
    private String jobClassName;
    private String jobData;
    private String cron;
    private String triggerNowFireInstanceId;
    private Timestamp startTime;
    private Timestamp stopTime;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public Integer getTaskRepeatCount() {
        return taskRepeatCount;
    }

    public void setTaskRepeatCount(Integer taskRepeatCount) {
        this.taskRepeatCount = taskRepeatCount;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public Integer getTaskInterval() {
        return taskInterval;
    }

    public void setTaskInterval(Integer taskInterval) {
        this.taskInterval = taskInterval;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getStopTime() {
        return stopTime;
    }

    public void setStopTime(Timestamp stopTime) {
        this.stopTime = stopTime;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public boolean isTriggerNow() {
        return triggerNowFireInstanceId != null;
    }

    public String getTriggerNowFireInstanceId() {
        return triggerNowFireInstanceId;
    }

    public void setTriggerNowFireInstanceId(String triggerNowFireInstanceId) {
        this.triggerNowFireInstanceId = triggerNowFireInstanceId;
    }
}
