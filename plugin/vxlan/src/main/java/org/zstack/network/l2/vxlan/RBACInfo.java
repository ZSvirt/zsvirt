package org.zstack.network.l2.vxlan;

import org.zstack.core.db.Q;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.network.l2.APIDeleteL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.search.SearchConstant;
import org.zstack.network.l2.vxlan.vtep.APIQueryVtepMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APIDeleteVxlanL2Network;
import org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg;

import java.util.Collections;

import static org.zstack.utils.CollectionDSL.list;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vxlan";
    }

    {
        permissionBuilder()
                .normalAPIs(
                        APIQueryVniRangeMsg.class,
                        APIQueryL2VxlanNetworkPoolMsg.class,
                        APIDeleteL2NetworkMsg.class,
                        APIDeleteVxlanL2Network.class
                )
                .normalAPIs("org.zstack.network.l2.vxlan.vxlanNetwork.**")
                .adminOnlyForAll()
                .targetResources(VxlanNetworkVO.class)
                .communityAvailable()
                .zsvAdvancedAvailable()
                .build();

        expandedPermission(APIDeleteL2NetworkMsg.class, api -> {
            boolean vxlan = Q.New(L2NetworkVO.class)
                    .eq(L2NetworkVO_.uuid, api.getUuid())
                    .eq(L2NetworkVO_.type, VxlanNetworkConstant.VXLAN_NETWORK_TYPE)
                    .isExists();
            if (vxlan) {
                APIDeleteVxlanL2Network expendMsg = new APIDeleteVxlanL2Network();
                expendMsg.setUuid(api.getUuid());
                return list(expendMsg);
            }

            return Collections.emptyList();
        });

        roleContributorBuilder()
                .roleName("networks")
                .actionsInThisPermission()
                .build();

        apis()
                .inPackage("org.zstack.network.l2.vxlan.vtep")
                .toService("network.l2")
                .build();
        apis()
                .api(APIQueryVtepMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.network.l2.vxlan.vxlanNetwork")
                .toService("network.l2")
                .build();
        apis()
                .api(APIQueryL2VxlanNetworkMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.network.l2.vxlan.vxlanNetworkPool")
                .toService("network.l2")
                .build();
        apis()
                .api(
                        APIQueryL2VxlanNetworkPoolMsg.class,
                        APIQueryVniRangeMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
