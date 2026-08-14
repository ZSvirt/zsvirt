package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceBaseExtensionFactory;
import org.zstack.header.vm.HypervisorBasedVmConfigurationFactory;
import org.zstack.header.vm.VmInstanceFactory;
import org.zstack.header.vm.VmInstanceNicFactory;

public interface VmFactoryManager {
    VmInstanceFactory getVmInstanceFactory(String vmInstanceType);

    VmInstanceBaseExtensionFactory getVmInstanceBaseExtensionFactory(Class vmInstanceClass);

    VmInstanceNicFactory getVmInstanceNicFactory(String vmNicType);

    VmNicQosConfigBackend getVmNicQosConfigBackend(String vmInstanceType);

    VmDnsBackend getVmDnsBackend(String vmInstanceType);

    HypervisorBasedVmConfigurationFactory getVmInstanceConfigurationFactory(String hypervisorType);
}
