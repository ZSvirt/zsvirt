package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.autoscaling.AutoScalingConstants;

/**
 * Created by lining on 2018/9/17.
 */
public enum  AutoScalingRuleTriggerType {
    Alarm(AutoScalingConstants.AutoScalingRule.TriggerType.Alarm),
    TimedTask(AutoScalingConstants.AutoScalingRule.TriggerType.TimedTask);

    private String name;
    AutoScalingRuleTriggerType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
