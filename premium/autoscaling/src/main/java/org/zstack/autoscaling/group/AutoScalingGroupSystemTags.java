package org.zstack.autoscaling.group;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Create by weiwang at 2018/8/16
 */
@TagDefinition
public class AutoScalingGroupSystemTags {

    public static final String INITIAL_INSTANCE_NUMBER_TOKEN = "initialInstanceNumber";
    public static SystemTag INITIAL_INSTANCE_NUMBER = new SystemTag(
            String.format("initialInstanceNumber::{%s}", INITIAL_INSTANCE_NUMBER_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String INITIALIZATION_INSTANCE_COMPLETED_TOKEN = "initializationInstanceCompleted";
    public static SystemTag INITIALIZATION_INSTANCE_COMPLETED = new SystemTag(
            String.format("initializationInstanceCompleted::{%s}", INITIALIZATION_INSTANCE_COMPLETED_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE_TOKEN = "automaticallyRemoveUnhealthyInstance";
    public static SystemTag AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE = new SystemTag(
            String.format("automaticallyRemoveUnhealthyInstance::{%s}", AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String COOLDOWN_DATE_TOKEN = "cooldownData";
    public static SystemTag COOLDOWN_DATE = new SystemTag(
            String.format("cooldownData::{%s}", COOLDOWN_DATE_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String VM_INSTANCE_HEALTH_STRATEGY_TOKEN = "vmInstanceHealthStrategy";
    public static SystemTag VM_INSTANCE_HEALTH_STRATEGY = new SystemTag(
            String.format("vmInstanceHealthStrategy::{%s}", VM_INSTANCE_HEALTH_STRATEGY_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS_TOKEN = "vmNicLoadbalancerListenerHealthCheckGraceTimeSeconds";
    public static SystemTag VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS = new SystemTag(
            String.format("vmNicLoadbalancerListenerHealthCheckGraceTimeSeconds::{%s}", VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS_TOKEN),
            AutoScalingGroupVO.class
    );

}
