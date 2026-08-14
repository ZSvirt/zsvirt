package org.zstack.compute.sriov;

import org.zstack.header.core.Completion;
import org.zstack.header.sriov.VmVfNicInventory;

public interface VmVfNicHaStateChangeExtensionPoint {
    void afterVmVfNicHaStateChange(VmVfNicInventory inventory, String haState, Completion completion);
}
