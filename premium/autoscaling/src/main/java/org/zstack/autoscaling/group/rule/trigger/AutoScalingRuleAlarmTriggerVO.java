package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;
import org.zstack.zwatch.alarm.AlarmVO;
import javax.persistence.Column;
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
public class AutoScalingRuleAlarmTriggerVO extends AutoScalingRuleTriggerVO {

    @Column
    @ForeignKey(parentEntityClass = AlarmVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String alarmUuid;

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}
