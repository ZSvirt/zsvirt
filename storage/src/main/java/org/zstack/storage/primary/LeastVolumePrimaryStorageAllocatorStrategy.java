package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;

public class LeastVolumePrimaryStorageAllocatorStrategy extends AbstractPrimaryStorageAllocatorStrategy {
    LeastVolumePrimaryStorageAllocatorStrategy(FlowChainBuilder allocateBuilder) {
        super(allocateBuilder);
    }

    LeastVolumePrimaryStorageAllocatorStrategy(FlowChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        super(allocateBuilder, sortBuilder);
    }
}
