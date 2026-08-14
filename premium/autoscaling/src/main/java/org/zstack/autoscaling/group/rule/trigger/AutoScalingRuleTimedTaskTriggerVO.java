package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.tag.AutoDeleteTag;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by lining on 2018/9/5.
 */
@Entity
@Table
@AutoDeleteTag
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class AutoScalingRuleTimedTaskTriggerVO extends AutoScalingRuleTriggerVO {
    /*
    @Column
    private Timestamp launchTime;

    @Column
    private Timestamp LaunchExpirationTime;

    @Column
    private TimedTaskTriggerRecurrenceType RecurrenceType;

    @Column
    private Timestamp RecurrenceEndTime;

    @Column
    private boolean TaskEnabled;

    @Column
    private String RecurrenceValue;
    */
    private String schedulerJobUuid;
}
