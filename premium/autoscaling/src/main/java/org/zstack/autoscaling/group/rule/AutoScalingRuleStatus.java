package org.zstack.autoscaling.group.rule;

/**
 * Create by lining at 2018/9/11
 */
public enum AutoScalingRuleStatus {
    Created,
    WaitingForTrigger,
    Triggering,
    Error,
}