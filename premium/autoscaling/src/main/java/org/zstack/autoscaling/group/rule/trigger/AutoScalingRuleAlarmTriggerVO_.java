package org.zstack.autoscaling.group.rule.trigger;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by lining at 2018/9/22
 */
@StaticMetamodel(AutoScalingRuleAlarmTriggerVO.class)
public class AutoScalingRuleAlarmTriggerVO_ extends AutoScalingRuleTriggerVO_ {
    public static volatile SingularAttribute<AutoScalingRuleAlarmTriggerVO, String> alarmUuid;
}
