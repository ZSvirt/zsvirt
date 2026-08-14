package org.zstack.network.service.lb;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 8/14/2015.
 */
@GlobalConfigDefinition
public class LoadBalancerGlobalConfig {
    public static final String CATEGORY = "loadBalancer";

    @GlobalConfigValidation
    public static GlobalConfig CONNECTION_IDLE_TIMEOUT = new GlobalConfig(CATEGORY, "connectionIdleTimeout");
    @GlobalConfigValidation
    public static GlobalConfig HEALTHY_THRESHOLD = new GlobalConfig(CATEGORY, "healthyThreshold");
    @GlobalConfigValidation
    public static GlobalConfig HEALTH_INTERVAL = new GlobalConfig(CATEGORY, "healthCheckInterval");
    @GlobalConfigValidation
    public static GlobalConfig HEALTH_TARGET = new GlobalConfig(CATEGORY, "healthCheckTarget");
    @GlobalConfigValidation
    public static GlobalConfig HEALTH_TIMEOUT = new GlobalConfig(CATEGORY, "healthCheckTimeout");
    @GlobalConfigValidation
    public static GlobalConfig UNHEALTHY_THRESHOLD = new GlobalConfig(CATEGORY, "unhealthyThreshold");
    @GlobalConfigValidation
    public static GlobalConfig MAX_CONNECTION = new GlobalConfig(CATEGORY, "maxConnection");
    @GlobalConfigValidation
    public static GlobalConfig NUMBER_OF_PROCESS = new GlobalConfig(CATEGORY, "Nbprocess");
    @GlobalConfigValidation
    public static GlobalConfig BALANCER_ALGORITHM = new GlobalConfig(CATEGORY, "balancerAlgorithm");
    @GlobalConfigValidation
    public static GlobalConfig ACL_MAX_COUNT = new GlobalConfig(CATEGORY, "aclMaxCount");
    @GlobalConfigValidation
    public static GlobalConfig ACL_REDIRECT_MAX_COUNT = new GlobalConfig(CATEGORY, "redirectRuleMaxCount");
    @GlobalConfigValidation
    public static GlobalConfig HTTP_MODE = new GlobalConfig(CATEGORY, "httpMode");


}
