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

/**
 * Created by AlanJager on 2017/6/7.
 */

@Entity
@Table
@ResourceAttributes
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ResourceVO.class, myField = "targetResourceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ManagementNodeVO.class, myField = "managementNodeUuid", targetField = "uuid")
        }
)
public class SchedulerJobVO extends ResourceVO implements SchedulerJobDesc, OwnedByAccount {
    @Column
    private String targetResourceUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String jobClassName;

    @Column
    private String jobData;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String managementNodeUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private String state;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedulerJobUuid", insertable = false, updatable = false)
    @NoView
    private Set<SchedulerJobSchedulerTriggerRefVO> addedTriggerRefs = new HashSet<SchedulerJobSchedulerTriggerRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedulerJobUuid", insertable = false, updatable = false)
    @NoView
    private Set<SchedulerJobGroupJobRefVO> jobGroupRefs = new HashSet<SchedulerJobGroupJobRefVO>();

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

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
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

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Set<SchedulerJobSchedulerTriggerRefVO> getAddedTriggerRefs() {
        return addedTriggerRefs;
    }

    public Set<SchedulerJobGroupJobRefVO> getJobGroupRefs() {
        return jobGroupRefs;
    }

    public void setJobGroupRefs(Set<SchedulerJobGroupJobRefVO> jobGroupRefs) {
        this.jobGroupRefs = jobGroupRefs;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setAddedTriggerRefs(Set<SchedulerJobSchedulerTriggerRefVO> addedTriggerRefs) {
        this.addedTriggerRefs = addedTriggerRefs;
    }
}
