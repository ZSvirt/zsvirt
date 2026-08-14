package org.zstack.autoscaling.group.instance;

import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityVO;
import org.zstack.autoscaling.template.AutoScalingTemplateVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2018/9/4.
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
public class AutoScalingGroupInstanceVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String instanceUuid;

    @Column
    @ForeignKey(parentEntityClass = AutoScalingGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String scalingGroupUuid;

    @Column
    @ForeignKey(parentEntityClass = AutoScalingTemplateVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String templateUuid;

    @Column
    @ForeignKey(parentEntityClass = AutoScalingGroupActivityVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String scalingGroupActivityUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingGroupInstanceStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingGroupInstanceHealthStatus healthStatus;

    @Column
    private String description;

    @Column
    private String protectionStrategy;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getScalingGroupActivityUuid() {
        return scalingGroupActivityUuid;
    }

    public void setScalingGroupActivityUuid(String scalingGroupActivityUuid) {
        this.scalingGroupActivityUuid = scalingGroupActivityUuid;
    }

    public AutoScalingGroupInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(AutoScalingGroupInstanceStatus status) {
        this.status = status;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public AutoScalingGroupInstanceHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(AutoScalingGroupInstanceHealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtectionStrategy() {
        return protectionStrategy;
    }

    public void setProtectionStrategy(String protectionStrategy) {
        this.protectionStrategy = protectionStrategy;
    }
}
