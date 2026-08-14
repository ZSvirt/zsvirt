package org.zstack.kvm;

import org.zstack.header.vm.VmInstanceSpec;

public interface KVMSubTypeVmConfigurationFactory {
    String getVmInstanceType();

    void createConfigurations(VmInstanceSpec spec);
}
