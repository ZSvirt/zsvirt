package org.zstack.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceExtensionPoint;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

public interface NetworkServiceManager {
    NetworkServiceProviderType getTypeOfNetworkServiceProviderForService(String l3NetworkUuid, NetworkServiceType serviceType);
    boolean isVmNeedNetworkService(String vmType, NetworkServiceType serviceType);

    void releaseNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPoint.NetworkServiceExtensionPosition position, Completion completion);
    void applyNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPoint.NetworkServiceExtensionPosition position, Completion completion);
    List<String> getL3NetworkDns(String l3NetworkUuid);
    List<String> getVmDns(String vmUuid, String l3NetworkUuid);
    List<String> getVmNicDns(String vmNicUuid, Integer ipVersion, String l3NetworkUuid);

    void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType,
                              NetworkServiceType nsType, List<String> systemTags, Completion completion);

    void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, NetworkServiceType nsType, Completion completion);
}
