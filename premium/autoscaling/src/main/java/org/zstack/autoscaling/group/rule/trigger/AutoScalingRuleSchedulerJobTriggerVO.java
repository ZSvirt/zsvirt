package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.scheduler.*;
import org.zstack.header.tag.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/8 10:53 AM
 */
@Entity
@Table
@AutoDeleteTag
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class AutoScalingRuleSchedulerJobTriggerVO extends AutoScalingRuleTriggerVO {
    @Column
    @ForeignKey(parentEntityClass = SchedulerJobVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String schedulerJobUuid;

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }
}
