package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.storage.primary.allocate.PrimaryStorageAllocatorChainBuilder;

class DefaultPrimaryStorageAllocatorStrategy extends AbstractPrimaryStorageAllocatorStrategy {
    DefaultPrimaryStorageAllocatorStrategy(PrimaryStorageAllocatorChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        super(allocateBuilder, sortBuilder);
    }
}
