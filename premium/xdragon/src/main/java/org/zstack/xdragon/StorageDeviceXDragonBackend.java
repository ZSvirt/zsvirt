package org.zstack.xdragon;

import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.storage.device.StorageDeviceKvmBackend;

public class StorageDeviceXDragonBackend extends StorageDeviceKvmBackend {
    @Override
    public String getSupportHypervisorType() {
        return XDragonConstant.HYPERVISOR_TYPE;
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (XDragonConstant.HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            super.addAddon(host, spec, cmd);
        }
    }
}
