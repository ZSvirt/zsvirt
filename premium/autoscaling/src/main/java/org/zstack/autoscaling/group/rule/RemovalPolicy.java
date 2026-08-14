package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.AutoScalingConstants;

/**
 * Created by lining on 2018/9/4.
 */
public enum RemovalPolicy {
    OldestInstance(AutoScalingConstants.REMOVAL_POLICY_OLDEST_INSTANCE),
    NewestInstance(AutoScalingConstants.REMOVAL_POLICY_NEWEST_INSTANCE),
    OldestScalingConfiguration(AutoScalingConstants.REMOVAL_POLICY_OLDEST_SCALING_CONFIGURATION),
    MinimumCPUUsageInstance(AutoScalingConstants.REMOVAL_POLICY_MINIMUM_CPU_USAGE_INSTANCE),
    MinimumMemoryUsageInstance(AutoScalingConstants.REMOVAL_POLICY_MINIMUM_MEMORY_USAGE_INSTANCE);

    private String name;

    RemovalPolicy(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
