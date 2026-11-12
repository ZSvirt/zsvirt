package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.Collections;
import java.util.List;

public interface AfterAllocateExternalNetworkNicExtensionPoint {
    default Flow createNicCleanTrafficFlow(VmNicInventory nic, boolean enable) {
        return null;
    }

    default boolean supportsNicMacChange(VmNicInventory nic) {
        return false;
    }

    default void changeNicMac(VmNicInventory nic, String mac, Completion completion) {
        completion.success();
    }

    default boolean supportsNicNetworkChange(VmNicInventory nic, L3NetworkInventory destination) {
        return false;
    }

    default void changeNicNetwork(VmInstanceSpec spec, VmNicInventory nic, Completion completion) {
        completion.success();
    }

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
