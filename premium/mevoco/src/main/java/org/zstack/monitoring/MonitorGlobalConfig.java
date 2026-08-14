package org.zstack.monitoring;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by xing5 on 2017/6/13.
 */
@GlobalConfigDefinition
public class MonitorGlobalConfig {
    public static final String CATEGORY = "monitoring";

    @GlobalConfigValidation(min = 1)
    public static PremiumGlobalConfig TRIGGER_RECOVERY_CHECKER_INTERVAL = new PremiumGlobalConfig(CATEGORY, "trigger.recovery.checker.interval");
}
