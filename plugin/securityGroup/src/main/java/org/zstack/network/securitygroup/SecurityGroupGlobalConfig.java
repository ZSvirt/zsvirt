package org.zstack.network.securitygroup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class SecurityGroupGlobalConfig {
    public static final String CATEGORY = "securityGroup";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig FAILURE_HOST_WORKER_INTERVAL = new GlobalConfig(CATEGORY, "host.failureWorkerInterval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig FAILURE_HOST_EACH_TIME_TO_TAKE = new GlobalConfig(CATEGORY, "host.failureResolvePerTime");
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig DELAY_REFRESH_INTERVAL = new GlobalConfig(CATEGORY, "refresh.delayInterval");
    @GlobalConfigValidation(validValues = {"accept", "deny", "drop"})
    public static GlobalConfig INGRESS_RULE_DEFAULT_POLICY = new GlobalConfig(CATEGORY, "ingress.defaultPolicy");
    @GlobalConfigValidation(validValues = {"accept", "deny"})
    public static GlobalConfig EGRESS_RULE_DEFAULT_POLICY = new GlobalConfig(CATEGORY, "egress.defaultPolicy");

    @GlobalConfigValidation(inNumberRange = {100, 1000})
    @GlobalConfigDef(defaultValue = "100", type = Integer.class,  description = "security group rules num limit")
    public static GlobalConfig SECURITY_GROUP_RULES_NUM_LIMIT = new GlobalConfig(CATEGORY, "rules.num.limit");
}
