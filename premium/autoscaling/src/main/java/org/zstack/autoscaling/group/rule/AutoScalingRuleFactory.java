package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.activity.CreateAutoScalingGroupActivityMsg;

/**
 * Created by lining on 2018/9/21.
 */
public interface AutoScalingRuleFactory {
    AutoScalingRuleType getType();

    CreateAutoScalingGroupActivityMsg makeAutoScalingGroupActivity(String ruleUuid);

    boolean skipAutoScalingGroupActivity(String ruleUuid);
}
