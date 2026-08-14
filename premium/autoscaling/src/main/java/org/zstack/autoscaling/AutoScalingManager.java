package org.zstack.autoscaling;

import org.zstack.autoscaling.group.ScalingResourceType;
import org.zstack.autoscaling.group.instance.ScalingGroupInstanceFactory;
import org.zstack.autoscaling.group.rule.AutoScalingRuleFactory;
import org.zstack.autoscaling.group.rule.AutoScalingRuleType;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerFactory;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerType;

/**
 * Created by lining on 2018/9/13.
 */
public interface AutoScalingManager {
    ScalingGroupInstanceFactory getScalingGroupInstanceFactory(ScalingResourceType type);

    AutoScalingRuleFactory getAutoScalingRuleFactory(AutoScalingRuleType type);

    AutoScalingRuleTriggerFactory getAutoScalingRuleTriggerFactory(AutoScalingRuleTriggerType type);

}
