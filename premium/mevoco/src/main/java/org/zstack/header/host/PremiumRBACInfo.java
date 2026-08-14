package org.zstack.header.host;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class PremiumRBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "premium-host";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIGetCandidateInterfaceVlanIdsMsg.class,
                        APIGetCandidateNetworkBondingsMsg.class,
                        APIGetCandidateNetworkInterfacesMsg.class,
                        APIGetClusterHostNetworkFactsMsg.class,
                        APIGetHostNetworkFactsMsg.class,
                        APIGetHostNUMATopologyMsg.class,
                        APIGetHostPhysicalMemoryFactsMsg.class,
                        APIGetInterfaceServiceTypeStatisticMsg.class,
                        APIQueryHostNetworkBondingMsg.class,
                        APIQueryHostNetworkInterfaceMsg.class,
                        APIQueryHostPhysicalMemoryMsg.class,
                        APIQueryHostPhysicalCpuMsg.class
                )
                .zsvBasicAvailable()
                .zsvProAvailable()
                .communityAvailable()
                .build();

        contributeNormalApiToOtherRole();

        apis()
                .api(
                        APIQueryHostNetworkBondingMsg.class,
                        APIQueryHostNetworkInterfaceMsg.class,
                        APIQueryHostPhysicalCpuMsg.class,
                        APIQueryHostPhysicalMemoryMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
