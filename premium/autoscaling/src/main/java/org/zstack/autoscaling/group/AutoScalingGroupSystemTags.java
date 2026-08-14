package org.zstack.autoscaling.group;

import org.zstack.autoscaling.group.rule.AutoScalingRuleConstants;
import org.zstack.autoscaling.group.rule.AutoScalingRuleVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Create by weiwang at 2018/8/16
 */
@TagDefinition
public class AutoScalingGroupSystemTags {

    public static final String INITIAL_INSTANCE_NUMBER_TOKEN = "initialInstanceNumber";
    public static PatternedSystemTag INITIAL_INSTANCE_NUMBER = new PatternedSystemTag(
            String.format("initialInstanceNumber::{%s}", INITIAL_INSTANCE_NUMBER_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String INITIALIZATION_INSTANCE_COMPLETED_TOKEN = "initializationInstanceCompleted";
    public static PatternedSystemTag INITIALIZATION_INSTANCE_COMPLETED = new PatternedSystemTag(
            String.format("initializationInstanceCompleted::{%s}", INITIALIZATION_INSTANCE_COMPLETED_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE_TOKEN = "automaticallyRemoveUnhealthyInstance";
    public static PatternedSystemTag AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE = new PatternedSystemTag(
            String.format("automaticallyRemoveUnhealthyInstance::{%s}", AUTOMATICALLY_REMOVE_UNHEALTHY_INSTANCE_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String COOLDOWN_DATE_TOKEN = "cooldownData";
    public static PatternedSystemTag COOLDOWN_DATE = new PatternedSystemTag(
            String.format("cooldownData::{%s}", COOLDOWN_DATE_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String VM_INSTANCE_HEALTH_STRATEGY_TOKEN = "vmInstanceHealthStrategy";
    public static PatternedSystemTag VM_INSTANCE_HEALTH_STRATEGY = new PatternedSystemTag(
            String.format("vmInstanceHealthStrategy::{%s}", VM_INSTANCE_HEALTH_STRATEGY_TOKEN),
            AutoScalingGroupVO.class
    );

    public static final String VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS_TOKEN = "vmNicLoadbalancerListenerHealthCheckGraceTimeSeconds";
    public static PatternedSystemTag VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS = new PatternedSystemTag(
            String.format("vmNicLoadbalancerListenerHealthCheckGraceTimeSeconds::{%s}", VMNIC_LOADBALANCER_LISTENER_HEALTH_CHECK_GRACE_TIME_SECONDS_TOKEN),
            AutoScalingGroupVO.class
    );

}
