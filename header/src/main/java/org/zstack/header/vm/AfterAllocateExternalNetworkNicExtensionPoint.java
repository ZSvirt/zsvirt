package org.zstack.header.vm;

import org.zstack.header.core.Completion;

import java.util.Collections;
import java.util.List;

public interface AfterAllocateExternalNetworkNicExtensionPoint {
    void afterAllocateExternalNetworkNic(VmInstanceSpec spec, List<VmNicInventory> nics, Completion completion);

    void rollbackExternalNetworkNic(VmInstanceSpec spec, List<VmNicInventory> nics, Completion completion);

    void releaseExternalNetworkNics(List<VmNicInventory> nics, Completion completion);

    default List<VmNicInventory> getExternalNetworkNicsToRelease(List<VmNicInventory> nics) {
        return Collections.emptyList();
    }

    default void expungeExternalNetworkNics(List<VmNicInventory> nics, Completion completion) {
        releaseExternalNetworkNics(nics, completion);
    }

    default void releaseNicIps(List<VmNicInventory> nics, Completion completion) {
        completion.success();
    }
}
