package org.zstack.network.hostNetworkInterface;

import java.util.List;

public interface HostNetworkInterfaceUpdateExtensionPoint {
    public void afterCreated(String hostUuid, List<HostNetworkInterfaceVO> interfaceVOS, List<HostNetworkBondingVO> bondingVOS);
}
