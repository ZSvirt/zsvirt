package org.zstack.pluginpremium.compute.allocator;

import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by lining on 2018/3/6.
 */
@TagDefinition
public class HostAllocatorSystemTags {

    public static String MAX_INSTANCE_PER_HOST_TOKEN = "maxInstancePerHost";
    public static PatternedSystemTag MAX_INSTANCE_PER_HOST = new PatternedSystemTag(String.format("maxInstancePerHost::{%s}", MAX_INSTANCE_PER_HOST_TOKEN), InstanceOfferingVO.class);

    // The default is soft mode
    // If not set, the default is soft mode
    public static String MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN = "minimumCPUUsageHostAllocatorStrategyMode";
    public static PatternedSystemTag MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE = new PatternedSystemTag(String.format("minimumCPUUsageHostAllocatorStrategyMode::{%s}", MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN), InstanceOfferingVO.class);

    // The default is soft mode
    // If not set, the default is soft mode
    public static String MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN = "minimumMemoryUsageHostAllocatorStrategyMode";
    public static PatternedSystemTag MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE = new PatternedSystemTag(String.format("minimumMemoryUsageHostAllocatorStrategyMode::{%s}", MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN), InstanceOfferingVO.class);

}
