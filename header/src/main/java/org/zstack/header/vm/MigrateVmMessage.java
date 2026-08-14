package org.zstack.header.vm;

import org.zstack.header.allocator.AllocationScene;

public interface MigrateVmMessage {
    String getHostUuid();
    String getStrategy();
    Integer getDownTime();
    boolean isMigrateFromDestination();
    boolean isAllowUnknown();

    default AllocationScene getAllocationScene() {
        return null;
    }
}
