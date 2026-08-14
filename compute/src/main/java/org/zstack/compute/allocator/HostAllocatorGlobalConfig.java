package org.zstack.compute.allocator;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class HostAllocatorGlobalConfig {
    public static final String CATEGORY = "hostAllocator";

    @GlobalConfigValidation
    public static GlobalConfig ZONE_LEVEL_RESERVE_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity.zoneLevel");
    @GlobalConfigValidation
    public static GlobalConfig CLUSTER_LEVEL_RESERVE_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity.clusterLevel");
    @GlobalConfigValidation
    public static GlobalConfig HOST_LEVEL_RESERVE_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity.hostLevel");
    @GlobalConfigValidation
    public static GlobalConfig USE_PAGINATION = new GlobalConfig(CATEGORY, "usePagination");
    @GlobalConfigValidation
    public static GlobalConfig PAGINATION_LIMIT = new GlobalConfig(CATEGORY, "paginationLimit");
    @GlobalConfigValidation
    public static GlobalConfig HOST_ALLOCATOR_ALLOW_CONCURRENT = new GlobalConfig(CATEGORY, "hostAllocator.concurrent");
    @GlobalConfigValidation(inNumberRange = {1, 255})
    @GlobalConfigDef(defaultValue = "10", type = Integer.class,  description = "allocator concurrency level, if enabled.")
    public static GlobalConfig HOST_ALLOCATOR_CONCURRENT_LEVEL = new GlobalConfig(CATEGORY, "hostAllocator.concurrent.level");
    @GlobalConfigValidation
    public static GlobalConfig HOST_ALLOCATOR_MAX_MEMORY = new GlobalConfig(CATEGORY, "hostAllocator.checkHostMem");
}
