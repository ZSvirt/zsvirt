package org.zstack.header.scheduler;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceAttributes;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by Mei Lei on 7/11/16.
 */
@Entity
@Table
@ResourceAttributes(nameField = "jobName")
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ManagementNodeVO.class, myField = "managementNodeUuid", targetField = "uuid")
        }
)
public class SchedulerVO extends ResourceVO {
    @Column
    private String targetResourceUuid;
    @Column
    private String schedulerName;
    @Column
    private String schedulerJob;
    @Column
    private String schedulerDescription;
    @Column
    private String schedulerType;
    @Column
    private Integer schedulerInterval;
    @Column
    private Integer repeatCount;
    @Column
    private String cronScheduler;
    @Column
    private String jobName;
    @Column
    private String jobGroup;
    @Column
    private String triggerName;
    @Column
    private String triggerGroup;
    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String managementNodeUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp startTime;
    @Column
    private Timestamp stopTime;
    @Column
    private Timestamp lastOpDate;
    /**
     * @desc jobClassName define the job
     */
    @Column
    private String jobClassName;
    @Column
    private String jobData;
    @Column
    private String state;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getSchedulerDescription() {
        return schedulerDescription;
    }

    public void setSchedulerDescription(String schedulerDescription) {
        this.schedulerDescription = schedulerDescription;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
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

    public Integer getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getCronScheduler() {
        return cronScheduler;
    }

    public void setCronScheduler(String cronScheduler) {
        this.cronScheduler = cronScheduler;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getSchedulerJob() {
        return schedulerJob;
    }

    public void setSchedulerJob(String schedulerJob) {
        this.schedulerJob = schedulerJob;
    }
}
