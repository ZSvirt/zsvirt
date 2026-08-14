package org.zstack.autoscaling;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by lining on 2018/10/12.
 */
@GlobalConfigDefinition
public class AutoScalingGlobalConfig {
    public static final String CATEGORY = "autoscaling";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VMNIC_LOADBALANCERLISTENER_HEALTH_CHECK_INTERVAL = new GlobalConfig(CATEGORY, "vmNicLoadBalancerListenerHealthCheck.interval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VMNIC_LOADBALANCERLISTENER_HEALTH_CHECK_THREADS_NUM = new GlobalConfig(CATEGORY, "vmNicLoadBalancerListenerHealthCheck.threadNum");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig REMOVE_UNHEALTHY_INSTANCE_INTERVAL = new GlobalConfig(CATEGORY, "removeUnhealthyInstance.interval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig REMOVE_UNHEALTHY_INSTANCE_THREADS_NUM = new GlobalConfig(CATEGORY, "removeUnhealthyInstance.threadNum");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig CHECK_THE_NUMBER_OF_INSTANCES_IN_THE_GROUP_INTERVAL = new GlobalConfig(CATEGORY, "checkTheNumberOfInstancesInTheGroup.interval");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig AutoScalingGroup_Activity_Retention_Amount = new GlobalConfig(CATEGORY, "autoScalingGroup.activity.retention.amount");
}

