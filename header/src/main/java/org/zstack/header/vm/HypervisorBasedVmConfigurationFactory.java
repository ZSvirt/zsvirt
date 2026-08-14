package org.zstack.header.vm;

public interface HypervisorBasedVmConfigurationFactory {
    String getHypervisorType();

    void createVmConfigurations(VmInstanceSpec spec);
}
