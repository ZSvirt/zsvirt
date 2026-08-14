package org.zstack.autoscaling.group.rule;

/**
 * Create by weiwang at 2018/8/15
 */
public enum ScaleInStrategy {
    LatestCreated,
    EarliestCreated,
    MinimalCpu,
    MinimalMemory
}
