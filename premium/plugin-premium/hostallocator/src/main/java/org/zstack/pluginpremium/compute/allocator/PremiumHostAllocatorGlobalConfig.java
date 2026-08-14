package org.zstack.pluginpremium.compute.allocator;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by lining on 2018/03/06.
 */
@GlobalConfigDefinition
public class PremiumHostAllocatorGlobalConfig {
    public static final String CATEGORY = "premiumHostAllocator";

    // seconds
    @GlobalConfigValidation(min = 0, max = 24 * 3600)
    public static PremiumGlobalConfig MINIMUM_CPU_USAGE_HOST_ALLOCATOR_COLLECT_HOST_DATA_DURATION = new PremiumGlobalConfig(CATEGORY, "minimumCPUUsageHostAllocatorStrategy.collectHostDataDuration");

    // seconds
    @GlobalConfigValidation(min = 0, max = 24 * 3600)
    public static PremiumGlobalConfig MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_COLLECT_HOST_DATA_DURATION = new PremiumGlobalConfig(CATEGORY, "minimumMemoryUsageHostAllocatorStrategy.collectHostDataDuration");

}
