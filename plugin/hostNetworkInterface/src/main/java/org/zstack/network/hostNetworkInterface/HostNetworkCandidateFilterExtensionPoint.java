package org.zstack.network.hostNetworkInterface;

import java.util.List;

public interface HostNetworkCandidateFilterExtensionPoint {
    List<HostNetworkBondingVO> filterBondingCandidates(List<HostNetworkBondingVO> candidates, List<String> hostUuids);
    List<HostNetworkInterfaceVO> filterInterfaceCandidates(List<HostNetworkInterfaceVO> candidates, List<String> hostUuids);
}
