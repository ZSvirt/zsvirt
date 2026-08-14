package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.host.HostAllocateExtensionPoint;

/**
 * Created by lining on 2018/09/27.
 */
public class StoppedVmAwareLeastVmPreferredHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory implements HostAllocateExtensionPoint {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.STOPPED_VM_AWARE_LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);

	@Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }

	@Override
	public void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
		if (!HostAllocatorConstant.STOPPED_VM_AWARE_LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE.equals(spec.getAllocatorStrategy())) {
			return;
		}
	}
}
