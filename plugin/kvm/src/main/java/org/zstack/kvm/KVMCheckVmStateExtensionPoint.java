package org.zstack.kvm;

import org.zstack.header.host.CheckVmStateOnHypervisorMsg;
import org.zstack.header.host.HostInventory;

import java.util.Map;

public interface KVMCheckVmStateExtensionPoint {
    void beforeCheckVmState(KVMHostInventory host, CheckVmStateOnHypervisorMsg msg, KVMAgentCommands.CheckVmStateCmd cmd);

    void afterCheckVmState(HostInventory host, Map<String, String> vmStateMap);
}
