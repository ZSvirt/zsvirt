package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerVO;
import org.zstack.autoscaling.template.AutoScalingTemplateGroupRefVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

/**
 * Created by lining on 2018/9/4.
 */
@Entity
@Table
@BaseResource
@Inheritance(strategy = InheritanceType.JOINED)
public class AutoScalingRuleVO extends ResourceVO implements ToInventory {

    @Column
    private String name;

    @Column
    @ForeignKey(parentEntityClass = AutoScalingGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String scalingGroupUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingRuleType type;

    @Column
    private Long cooldown;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingRuleState state;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingRuleStatus status;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "resourceUuid", insertable = false, updatable = false)
    @NoView
    private Set<SystemTagVO> systemTags;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "ruleUuid", insertable = false, updatable = false)
    @NoView
    private Set<AutoScalingRuleTriggerVO> triggers;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public AutoScalingRuleType getType() {
        return type;
    }

    public void setType(AutoScalingRuleType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AutoScalingRuleState getState() {
        return state;
    }

    public void setState(AutoScalingRuleState state) {
        this.state = state;
    }

    public AutoScalingRuleStatus getStatus() {
        return status;
    }

    public void setStatus(AutoScalingRuleStatus status) {
        this.status = status;
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

    public Long getCooldown() {
        return cooldown;
    }

    public void setCooldown(Long cooldown) {
        this.cooldown = cooldown;
    }

    public Set<SystemTagVO> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(Set<SystemTagVO> systemTags) {
        this.systemTags = systemTags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public Set<AutoScalingRuleTriggerVO> getTriggers() {
        return triggers;
    }

    public void setTriggers(Set<AutoScalingRuleTriggerVO> triggers) {
        this.triggers = triggers;
    }
}
