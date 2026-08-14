package org.zstack.autoscaling.group.instance.vm;

/**
 * Create by lining at 2018/9/29
 */
public enum AutoScalingGroupVmInstanceHealthStrategy {
    VmInstanceStatus,
    LoadBalanceBackendStatus,
    Any
}
