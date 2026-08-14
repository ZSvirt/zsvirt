package org.zstack.pluginpremium.compute.allocator;

public interface HostAllocatorConstant {

    int periodSecs = 60;
    String MINIMUM_CPU_USAGE_HOST_ALLOCATOR_STRATEGY_TYPE = "MinimumCPUUsageHostAllocatorStrategy";
    String MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_TYPE = "MinimumMemoryUsageHostAllocatorStrategy";

    String MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE = "MaxInstancePerHostHostAllocatorStrategy";

    String HOST_ALLOCATOR_STRATEGY_MODE_HARD = "hard";
    String HOST_ALLOCATOR_STRATEGY_MODE_SOFT = "soft";
}
