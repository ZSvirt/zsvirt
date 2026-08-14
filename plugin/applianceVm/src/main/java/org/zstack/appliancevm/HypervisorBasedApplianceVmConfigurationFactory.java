package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstanceSpec;

public interface HypervisorBasedApplianceVmConfigurationFactory {
    ApplianceVmType getApplianceVmType();

    void createHypervisorBasedConfigurations(VmInstanceSpec spec);
}
