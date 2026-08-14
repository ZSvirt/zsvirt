package org.zstack.header.scheduler;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceAttributes;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@ResourceAttributes
@org.zstack.header.vo.EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ManagementNodeVO.class, myField = "managementNodeUuid", targetField = "uuid")
        }
)
public class SchedulerJobGroupVO extends ResourceVO implements SchedulerJobDesc, OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String jobClassName;

    @Column
    private String jobData;

    @Column
    private String jobType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private String state;

    @Column
    private String zoneUuid;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String managementNodeUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedulerJobGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<SchedulerJobGroupSchedulerTriggerRefVO> addedTriggerRefs = new HashSet<SchedulerJobGroupSchedulerTriggerRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedulerJobGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<SchedulerJobGroupJobRefVO> addedJobsRefs = new HashSet<SchedulerJobGroupJobRefVO>();

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

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Set<SchedulerJobGroupSchedulerTriggerRefVO> getAddedTriggerRefs() {
        return addedTriggerRefs;
    }

    public void setAddedTriggerRefs(Set<SchedulerJobGroupSchedulerTriggerRefVO> addedTriggerRefs) {
        this.addedTriggerRefs = addedTriggerRefs;
    }

    public Set<SchedulerJobGroupJobRefVO> getAddedJobsRefs() {
        return addedJobsRefs;
    }

    public void setAddedJobsRefs(Set<SchedulerJobGroupJobRefVO> addedJobsRefs) {
        this.addedJobsRefs = addedJobsRefs;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
}
