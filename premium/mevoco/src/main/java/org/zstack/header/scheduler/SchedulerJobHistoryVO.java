package org.zstack.header.scheduler;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/4/22.
 */
@Entity
@Table
public class SchedulerJobHistoryVO {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @Column
    private String triggerUuid;

    @Column
    private String schedulerJobUuid;

    @Column
    private String schedulerJobGroupUuid;

    @Column
    private String jobType;

    @Column
    private Timestamp startTime;

    @Column
    private long executeTime;

    @Column
    private String targetResourceUuid;

    @Column
    private String requestDump;

    @Column
    private String resultDump;

    @Column
    private boolean success;

    @Column
    private String fireInstanceId;

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getResultDump() {
        return resultDump;
    }

    public void setResultDump(String resultDump) {
        this.resultDump = resultDump;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRequestDump() {
        return requestDump;
    }

    public void setRequestDump(String requestDump) {
        this.requestDump = requestDump;
    }

    public String getFireInstanceId() {
        return fireInstanceId;
    }

    public void setFireInstanceId(String fireInstanceId) {
        this.fireInstanceId = fireInstanceId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public boolean isRunning() {
        return this.executeTime < 0;
    }
}
