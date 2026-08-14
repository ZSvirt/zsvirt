package org.zstack.kvm;

import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;

/**
 */
public interface KVMStartVmAddonExtensionPoint {
    VmInstanceType getVmTypeForAddonExtension();

    void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd);
}
