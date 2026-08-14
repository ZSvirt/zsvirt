package org.zstack.header.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by AlanJager on 2017/6/7.
 */

@Inventory(mappingVOClass = SchedulerTriggerVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "schedulerJobSchedulerTriggerRef", inventoryClass = SchedulerJobSchedulerTriggerInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "schedulerTriggerUuid", hidden = true),
        @ExpandedQuery(expandedField = "schedulerJobGroupSchedulerTriggerRef", inventoryClass = SchedulerJobGroupSchedulerTriggerRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "schedulerTriggerUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "job", expandedField = "schedulerJobSchedulerTriggerRef.job"),
        @ExpandedQueryAlias(alias = "jobGroup", expandedField = "schedulerJobGroupSchedulerTriggerRef.jobGroup"),
})
public class SchedulerTriggerInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String cron;
    private String schedulerType;
    private Integer schedulerInterval;
    private Integer repeatCount;
    private Timestamp startTime;
    private Timestamp stopTime;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = SchedulerJobSchedulerTriggerInventory.class,
            joinColumn = @JoinColumn(name = "schedulerTriggerUuid", referencedColumnName = "schedulerJobUuid"))
    private List<String> jobsUuid;

    @Queryable(mappingClass = SchedulerJobGroupSchedulerTriggerRefInventory.class,
            joinColumn = @JoinColumn(name = "schedulerTriggerUuid", referencedColumnName = "schedulerJobUuid"))
    private List<String> jobGroupsUuid;

    protected SchedulerTriggerInventory(SchedulerTriggerVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.schedulerType = vo.getSchedulerType();
        this.schedulerInterval = vo.getSchedulerInterval();
        this.repeatCount = vo.getRepeatCount();
        this.cron = vo.getCron();
        this.startTime = vo.getStartTime();
        this.stopTime = vo.getStopTime();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();

        jobsUuid = new ArrayList<String>(vo.getAddedJobRefs().size());
        for (SchedulerJobSchedulerTriggerRefVO ref : vo.getAddedJobRefs()) {
            jobsUuid.add(ref.getSchedulerJobUuid());
        }

        jobGroupsUuid = new ArrayList<>(vo.getAddedJobGroupRefs().size());
        for (SchedulerJobGroupSchedulerTriggerRefVO ref : vo.getAddedJobGroupRefs()) {
            jobGroupsUuid.add(ref.getSchedulerJobGroupUuid());
        }
    }

    public SchedulerTriggerInventory() {

    }

    public static SchedulerTriggerInventory valueOf(SchedulerTriggerVO vo) {
        return new SchedulerTriggerInventory(vo);
    }

    public static List<SchedulerTriggerInventory> valueOf(Collection<SchedulerTriggerVO> vos) {
        List<SchedulerTriggerInventory> invs = new ArrayList<SchedulerTriggerInventory>(vos.size());
        for (SchedulerTriggerVO vo : vos) {
            invs.add(SchedulerTriggerInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public Integer getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public List<String> getJobsUuid() {
        return jobsUuid;
    }

    public void setJobsUuid(List<String> jobsUuid) {
        this.jobsUuid = jobsUuid;
    }

    public List<String> getJobGroupsUuid() {
        return jobGroupsUuid;
    }

    public void setJobGroupsUuid(List<String> jobGroupsUuid) {
        this.jobGroupsUuid = jobGroupsUuid;
    }
}
