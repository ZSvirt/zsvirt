package org.zstack.kvm;

import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceState;

import java.util.Map;
import java.util.Set;

public interface KvmVmSyncExtensionPoint {
    void afterVmSync(HostInventory host, Map<String, VmInstanceState> states, Set<String> vmsToSkipSetHostSide);
}
