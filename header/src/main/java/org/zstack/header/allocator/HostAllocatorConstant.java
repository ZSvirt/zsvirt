package org.zstack.header.allocator;

import org.zstack.header.errorcode.ErrorCode;

public interface HostAllocatorConstant {
    String SERVICE_ID = "host.allocator";
    String DEFAULT_HOST_ALLOCATOR_STRATEGY_TYPE = "DefaultHostAllocatorStrategy";
    String DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE = "DesignatedHostAllocatorStrategy";
    String LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE = "LastHostPreferredAllocatorStrategy";
    String MIGRATE_VM_ALLOCATOR_TYPE = "MigrateVmAllocatorStrategy";
    String LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE = "LeastVmPreferredHostAllocatorStrategy";
    String STOPPED_VM_AWARE_LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE = "StoppedVmAwareLeastVmPreferredHostAllocatorStrategy";

    ErrorCode PAGINATION_INTERMEDIATE_ERROR = new ErrorCode(
            "NO_AVAILABLE_HOST_BUT_PAGINATION_HAS_NOT_DONE",
            "some allocator flow cannot find candidate hosts; given it's still in pagination process, will continue allocating"
    );

    enum Param {
        HOST,
        SPEC,
    }
}
