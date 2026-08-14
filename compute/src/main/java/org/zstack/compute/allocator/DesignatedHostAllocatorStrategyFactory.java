package org.zstack.compute.allocator;

import org.zstack.header.allocator.*;

public class DesignatedHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory {
    private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE, false);
    
    @Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
        return type;
    }

    @Override
    public void marshalSpec(HostAllocatorSpec spec, AllocateHostMsg msg) {
        DesignatedAllocateHostMsg dmsg = (DesignatedAllocateHostMsg)msg;
        spec.setZoneUuid(dmsg.getZoneUuid());
        spec.setClusterUuids(dmsg.getClusterUuids());
        spec.setHostUuid(dmsg.getHostUuid());
    }
}
