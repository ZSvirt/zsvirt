package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;

public class CustomOrderPrimaryStorageAllocatorStrategy extends AbstractPrimaryStorageAllocatorStrategy {
    CustomOrderPrimaryStorageAllocatorStrategy(FlowChainBuilder allocateBuilder) {
        super(allocateBuilder);
    }

    CustomOrderPrimaryStorageAllocatorStrategy(FlowChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        super(allocateBuilder, sortBuilder);
    }
}
