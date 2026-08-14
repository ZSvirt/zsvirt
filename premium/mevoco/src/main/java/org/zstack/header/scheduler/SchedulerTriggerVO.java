package org.zstack.header.scheduler;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by AlanJager on 2017/6/7.
 */

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ManagementNodeVO.class, myField = "managementNodeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = SchedulerJobSchedulerTriggerRefVO.class, myField = "uuid", targetField = "schedulerTriggerUuid")
        }
)
public class SchedulerTriggerVO extends ResourceVO implements OwnedByAccount {

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String schedulerType;

    @Column
    private Integer schedulerInterval;

    @Column
    private Integer repeatCount;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    @Deprecated
    private String managementNodeUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp startTime;

    @Column
    private Timestamp stopTime;

    @Column
    private String cron;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedulerTriggerUuid", insertable = false, updatable = false)
    @NoView
    private Set<SchedulerJobSchedulerTriggerRefVO> addedJobRefs = new HashSet<SchedulerJobSchedulerTriggerRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedulerTriggerUuid", insertable = false, updatable = false)
    @NoView
    private Set<SchedulerJobGroupSchedulerTriggerRefVO> addedJobGroupRefs = new HashSet<SchedulerJobGroupSchedulerTriggerRefVO>();

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public SchedulerTriggerVO() {
    }

    public SchedulerTriggerVO(SchedulerTriggerVO other) {
        this.addedJobRefs = other.addedJobRefs;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
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

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
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

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Set<SchedulerJobSchedulerTriggerRefVO> getAddedJobRefs() {
        return addedJobRefs;
    }

    public void setAddedJobRefs(Set<SchedulerJobSchedulerTriggerRefVO> addedJobRefs) {
        this.addedJobRefs = addedJobRefs;
    }

    public Set<SchedulerJobGroupSchedulerTriggerRefVO> getAddedJobGroupRefs() {
        return addedJobGroupRefs;
    }

    public void setAddedJobGroupRefs(Set<SchedulerJobGroupSchedulerTriggerRefVO> addedJobGroupRefs) {
        this.addedJobGroupRefs = addedJobGroupRefs;
    }
}
