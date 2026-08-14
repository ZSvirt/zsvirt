package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.autoscaling.group.rule.AutoScalingRuleVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;
import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2018/9/5.
 */
@Entity
@Table
@BaseResource
@Inheritance(strategy = InheritanceType.JOINED)
public class AutoScalingRuleTriggerVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    @ForeignKey(parentEntityClass = AutoScalingRuleVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String ruleUuid;

    @Column
    private String name;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingRuleTriggerType type;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingRuleTriggerState state;

    @Column
    private String description;

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

    public AutoScalingRuleTriggerType getType() {
        return type;
    }

    public void setType(AutoScalingRuleTriggerType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AutoScalingRuleTriggerState getState() {
        return state;
    }

    public void setState(AutoScalingRuleTriggerState state) {
        this.state = state;
    }
}
