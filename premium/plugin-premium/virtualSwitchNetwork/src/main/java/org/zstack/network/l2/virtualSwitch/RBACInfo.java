package org.zstack.network.l2.virtualSwitch;

import org.zstack.header.description.PackageDescription;
import org.zstack.network.l2.virtualSwitch.header.*;

import org.zstack.header.search.SearchConstant;
/**
 * @ Author : yh.w
 * @ Date   : Created in 19:10 2019/7/7
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "virtual-switch";
    }

    {
        permissionBuilder()
                .targetResources(L2VirtualSwitchNetworkVO.class, PortGroupVO.class)
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("c3b42792e2fb4466a0b979920f5784fe")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .roleName("legacy")
                .actions(
                        APICreatePortGroupMsg.class,
                        APIQueryHostKernelInterfaceMsg.class,
                        APIQueryL2PortGroupNetworkMsg.class,
                        APIQueryL2VirtualSwitchNetworkMsg.class,
                        APIQueryPortGroupMsg.class,
                        APIQueryUplinkGroupMsg.class,
                        APIUpdatePortGroupMsg.class
                )
                .build();

        apis()
                .inPackage("org.zstack.network.l2.virtualSwitch.header")
                .toService("network.l2")
                .build();
        apis()
                .api(
                        APIQueryHostKernelInterfaceMsg.class,
                        APIQueryL2PortGroupNetworkMsg.class,
                        APIQueryL2VirtualSwitchNetworkMsg.class,
                        APIQueryPortGroupMsg.class,
                        APIQueryUplinkGroupMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
