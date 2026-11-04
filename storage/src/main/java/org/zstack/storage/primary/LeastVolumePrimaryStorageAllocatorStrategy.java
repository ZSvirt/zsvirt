package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.storage.primary.allocate.PrimaryStorageAllocatorChainBuilder;

public class LeastVolumePrimaryStorageAllocatorStrategy extends AbstractPrimaryStorageAllocatorStrategy {
    LeastVolumePrimaryStorageAllocatorStrategy(PrimaryStorageAllocatorChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        super(allocateBuilder, sortBuilder);
    }
}
