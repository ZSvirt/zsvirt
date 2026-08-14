package org.zstack.autoscaling.group.activity;

import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.rule.*;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;
import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2018/9/4.
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
public class AutoScalingGroupActivityVO extends ResourceVO implements ToInventory {
    @Column
    private String name;

    @Column
    @ForeignKey(parentEntityClass = AutoScalingGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String scalingGroupUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingGroupActivityAction activityAction;

    @Column
    private String instanceUuids;

    @Column
    @ForeignKey(parentEntityClass = AutoScalingRuleVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String scalingGroupRuleUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingGroupActivityCause cause;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingGroupActivityStatus status;

    @Column
    private String activityActionResultMessage;

    @Column
    private Timestamp endDate;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public AutoScalingGroupActivityAction getActivityAction() {
        return activityAction;
    }

    public void setActivityAction(AutoScalingGroupActivityAction activityAction) {
        this.activityAction = activityAction;
    }

    public String getInstanceUuids() { return instanceUuids; }

    public void setInstanceUuids(String instanceUuids) { this.instanceUuids = instanceUuids; }

    public String getScalingGroupRuleUuid() {
        return scalingGroupRuleUuid;
    }

    public void setScalingGroupRuleUuid(String scalingGroupRuleUuid) {
        this.scalingGroupRuleUuid = scalingGroupRuleUuid;
    }

    public AutoScalingGroupActivityCause getCause() {
        return cause;
    }

    public void setCause(AutoScalingGroupActivityCause cause) {
        this.cause = cause;
    }

    public AutoScalingGroupActivityStatus getStatus() {
        return status;
    }

    public void setStatus(AutoScalingGroupActivityStatus status) {
        this.status = status;
    }

    public String getActivityActionResultMessage() {
        return activityActionResultMessage;
    }

    public void setActivityActionResultMessage(String activityActionResultMessage) {
        this.activityActionResultMessage = activityActionResultMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
