package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.AutoScalingConstants;

/**
 * Created by lining on 2018/9/21.
 */
public enum AutoScalingRuleType {
    AddingNewInstanceRule(AutoScalingConstants.AutoScalingRule.RULE_TYPE_ADDING_NEW_INSTANCE),
    RemovalInstanceRule(AutoScalingConstants.AutoScalingRule.RULE_TYPE_REMOVAL_INSTANCE);

    private String name;

    AutoScalingRuleType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
