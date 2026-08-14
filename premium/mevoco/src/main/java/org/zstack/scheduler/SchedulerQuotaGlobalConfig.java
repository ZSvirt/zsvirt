package org.zstack.scheduler;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class SchedulerQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig SCHEDULER_NUM = new PremiumGlobalConfig(CATEGORY, SchedulerQuotaConstant.SCHEDULER_NUM);

    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig SCHEDULER_TRIGGER_NUM = new PremiumGlobalConfig(CATEGORY, SchedulerQuotaConstant.SCHEDULER_TRIGGER_NUM);
}
