package org.zstack.autoscaling.group.rule;

import org.zstack.header.configuration.PythonClass;

/**
 * Create by weiwang at 2018/8/15
 */
@PythonClass
public class AutoScalingRuleConstants {
    @PythonClass
    public static final String HORIZONTAL_SCALING_PROFILE = "HorizontalScalingProfile";
    @PythonClass
    public static final String HEALTH_PROFILE = "HealthProfile";
    @PythonClass
    public static final String LOAD_BALANCE_PROFILE = "LoadBalanceProfile";
    @PythonClass
    public static final String ALARM_PROFILE = "AlarmProfile";

    public static final String AUTO_SCALING_EVENT_PATH = "autoscaling/event";
}
