package org.zstack.autoscaling.group.rule.trigger;

import javax.persistence.metamodel.*;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/8 10:56 AM
 */
@StaticMetamodel(AutoScalingRuleSchedulerJobTriggerVO.class)
public class AutoScalingRuleSchedulerJobTriggerVO_ extends AutoScalingRuleTriggerVO_ {
    public static volatile SingularAttribute<AutoScalingRuleAlarmTriggerVO, String> schedulerJobUuid;
}
