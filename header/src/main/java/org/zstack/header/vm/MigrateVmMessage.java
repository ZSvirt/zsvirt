package org.zstack.header.vm;

import org.zstack.header.allocator.AllocatorDomainScene;

public interface MigrateVmMessage {
    String getHostUuid();
    String getStrategy();
    Integer getDownTime();
    boolean isMigrateFromDestination();
    boolean isAllowUnknown();

    default AllocatorDomainScene getAllocationScene() {
        return null;
    }

    default Boolean getEnableMigrationTls() {
        return null;
    }
}
