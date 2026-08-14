package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmInstanceSpec;

public interface ApplyNetworkServiceExtensionPoint {
    NetworkServiceProviderType getProviderType();

    void applyNetworkService(VmInstanceSpec servedVm, Completion complete);

    void releaseNetworkService(VmInstanceSpec servedVm, Completion complete);
}
