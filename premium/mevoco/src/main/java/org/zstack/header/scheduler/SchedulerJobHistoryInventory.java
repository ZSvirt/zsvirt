package org.zstack.header.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/4/22.
 */
@Inventory(mappingVOClass = SchedulerJobHistoryVO.class)
@PythonClassInventory
public class SchedulerJobHistoryInventory {
    private long id;
    private String triggerUuid;
    private String schedulerJobUuid;
    private String schedulerJobGroupUuid;
    private String jobType;
    private Timestamp startTime;
    private long executeTime;
    private String targetResourceUuid;
    private String requestDump;
    private String resultDump;
    private boolean success;
    private String fireInstanceId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static SchedulerJobHistoryInventory valueOf(SchedulerJobHistoryVO vo) {
        SchedulerJobHistoryInventory inv = new SchedulerJobHistoryInventory();
        inv.id = vo.getId();
        inv.triggerUuid = vo.getTriggerUuid();
        inv.schedulerJobUuid = vo.getSchedulerJobUuid();
        inv.schedulerJobGroupUuid = vo.getSchedulerJobGroupUuid();
        inv.targetResourceUuid = vo.getTargetResourceUuid();
        inv.executeTime = vo.getExecuteTime();
        inv.startTime = vo.getStartTime();
        inv.success = vo.isSuccess();
        inv.fireInstanceId = vo.getFireInstanceId();
        inv.jobType = vo.getJobType();
        inv.resultDump = vo.getResultDump();
        inv.requestDump = vo.getRequestDump();
        return inv;
    }

    public static List<SchedulerJobHistoryInventory> valueOf(Collection<SchedulerJobHistoryVO> vos) {
        return vos.stream().map(SchedulerJobHistoryInventory::valueOf).collect(Collectors.toList());
    }

    public String getRequestDump() {
        return requestDump;
    }

    public void setRequestDump(String requestDump) {
        this.requestDump = requestDump;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getFireInstanceId() {
        return fireInstanceId;
    }

    public void setFireInstanceId(String fireInstanceId) {
        this.fireInstanceId = fireInstanceId;
    }
}
