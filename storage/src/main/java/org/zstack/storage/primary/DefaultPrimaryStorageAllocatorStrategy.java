package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;

class DefaultPrimaryStorageAllocatorStrategy extends AbstractPrimaryStorageAllocatorStrategy {
    DefaultPrimaryStorageAllocatorStrategy(FlowChainBuilder allocateBuilder) {
        super(allocateBuilder);
    }

    DefaultPrimaryStorageAllocatorStrategy(FlowChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        super(allocateBuilder, sortBuilder);
    }
}
