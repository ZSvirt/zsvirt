package org.zstack.storage.primary;


import org.zstack.core.workflow.FlowChainBuilder;

public class MaximumAvailableCapacityAllocatorStrategy extends AbstractPrimaryStorageAllocatorStrategy{
    MaximumAvailableCapacityAllocatorStrategy(FlowChainBuilder allocateBuilder) {
        super(allocateBuilder);
    }

    MaximumAvailableCapacityAllocatorStrategy(FlowChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        super(allocateBuilder, sortBuilder);
    }
}
