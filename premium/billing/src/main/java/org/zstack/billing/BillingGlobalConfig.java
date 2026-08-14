package org.zstack.billing;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by xing5 on 2016/6/11.
 */
@GlobalConfigDefinition
public class BillingGlobalConfig {
    public static final String CATEGORY = "billing";

    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig SAMPLING_INTERVAL = new PremiumGlobalConfig(CATEGORY, "sampling.interval");

    @GlobalConfigValidation(min = 2)
    public static GlobalConfig BILLING_SYNC_LEVEL = new GlobalConfig(CATEGORY, "billing.syncLevel");

    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig BILLING_GENERATION_INTERVAL_DAY = new PremiumGlobalConfig(CATEGORY, "billing.generation.interval");

    @GlobalConfigValidation(min = -1, max = 24)
    public static PremiumGlobalConfig BILLING_GENERATION_HOUR_POINT = new PremiumGlobalConfig(CATEGORY, "billing.generation.hour.point");

    @GlobalConfigValidation(min = 10, max = 1000)
    public static PremiumGlobalConfig BILLING_GENERATION_RESOURCE_SLICE_SIZE = new PremiumGlobalConfig(CATEGORY, "billing.generation.resource.sliceSize");

    @GlobalConfigValidation(validValues = {"CNY", "USD", "EUR", "GBP", "AUD", "HKD", "JPY", "CHF", "CAD", "IDR"})
    public static GlobalConfig BILLING_CURRENCY_SYMBOL = new PremiumGlobalConfig(CATEGORY, "billing.currency.symbol");

    @GlobalConfigValidation(min = -1, max = 100)
    @GlobalConfigDef(defaultValue = "20", type = Integer.class, description = "the sync level of persist resource usage")
    public static GlobalConfig PERSIST_RESOURCE_USAGE_SYNC_LEVEL = new GlobalConfig(CATEGORY, "billing.persistUsageSyncLevel");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "1000000", type = Long.class, description = "maximum number of query resource usages")
    public static GlobalConfig MAX_QUERY_USAGE_NUMBER = new GlobalConfig(CATEGORY, "billing.maxQueryUsageNum");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "enable billing")
    public static GlobalConfig BILLING_ENABLE = new GlobalConfig(CATEGORY, "billing.enable");
}
