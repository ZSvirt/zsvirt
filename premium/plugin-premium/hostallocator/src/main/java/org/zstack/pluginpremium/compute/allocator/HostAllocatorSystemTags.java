package org.zstack.pluginpremium.compute.allocator;

import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by lining on 2018/3/6.
 */
@TagDefinition
public class HostAllocatorSystemTags {

    public static String MAX_INSTANCE_PER_HOST_TOKEN = "maxInstancePerHost";
    public static SystemTag MAX_INSTANCE_PER_HOST = new SystemTag(String.format("maxInstancePerHost::{%s}", MAX_INSTANCE_PER_HOST_TOKEN), InstanceOfferingVO.class);

    // The default is soft mode
    // If not set, the default is soft mode
    public static String MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN = "minimumCPUUsageHostAllocatorStrategyMode";
    public static SystemTag MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE = new SystemTag(String.format("minimumCPUUsageHostAllocatorStrategyMode::{%s}", MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN), InstanceOfferingVO.class);

    // The default is soft mode
    // If not set, the default is soft mode
    public static String MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN = "minimumMemoryUsageHostAllocatorStrategyMode";
    public static SystemTag MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE = new SystemTag(String.format("minimumMemoryUsageHostAllocatorStrategyMode::{%s}", MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN), InstanceOfferingVO.class);

}
