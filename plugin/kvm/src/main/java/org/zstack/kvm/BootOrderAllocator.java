package org.zstack.kvm;

import org.zstack.header.vm.VmInstanceSpec;

public interface BootOrderAllocator {
    String getDeviceType();

    int allocateBootOrder(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec, int bootOrderNum);
}
