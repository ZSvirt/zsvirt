package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategyType;

public class LastHostPreferredAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory {
    private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE, false);
    
    @Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
        return type;
    }
}
