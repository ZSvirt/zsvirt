package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategyType;

public class DefaultHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.DEFAULT_HOST_ALLOCATOR_STRATEGY_TYPE);

	@Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }
}
