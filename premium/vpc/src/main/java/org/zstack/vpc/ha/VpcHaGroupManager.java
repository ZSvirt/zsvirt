package org.zstack.vpc.ha;

import org.zstack.appliancevm.ApplianceVmHaStatus;

public interface VpcHaGroupManager {
    boolean setVirtualRouterHaStatus(String vrUuid, ApplianceVmHaStatus status);
}
