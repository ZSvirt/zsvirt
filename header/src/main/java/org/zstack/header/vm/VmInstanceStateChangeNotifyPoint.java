package org.zstack.header.vm;

import org.zstack.header.host.HypervisorType;

public interface VmInstanceStateChangeNotifyPoint {
    void notifyVmInstanceStateChange(VmInstanceInventory inv, VmInstanceState previousState, VmInstanceState currentState);

    HypervisorType getSupportedHypervisorTypeForVmInstanceStateChangeNotifyPoint();
}
