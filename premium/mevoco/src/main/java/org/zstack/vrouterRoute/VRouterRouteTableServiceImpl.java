package org.zstack.vrouterRoute;

import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.virtualrouter.vyos.VyosVmFactory;
import org.zstack.utils.DebugUtils;

import java.util.List;

public class VRouterRouteTableServiceImpl implements RouteTableServiceFactory {
    @Override
    public String getApplianceVmType() {
        return VyosVmFactory.applianceVmType.toString();
    }

    @Override
    public String getProviderTypeFromVRouter(String vrouterVmUuid) {
        // Note(WeiW): Suppose all l3 have same provider, this have checked in attach
        List<VmNicVO> vos = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vrouterVmUuid)
                .list();

        String l3Uuid = "";
        for (VmNicVO vo : vos) {
            if (Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, false).eq(L3NetworkVO_.uuid, vo.getL3NetworkUuid()).isExists() &&
                    Q.New(NetworkServiceL3NetworkRefVO.class)
                            .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, vo.getL3NetworkUuid())
                            .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, VRouterRouteConstants.VROUTER_ROUTE_NETWORK_SERVICE_TYPE.toString())
                            .isExists()) {
                l3Uuid = vo.getL3NetworkUuid();
                break;
            }
        }

        DebugUtils.Assert(!l3Uuid.equals(""), String.format("not found guest l3 attached to the vrouter[%s]", vrouterVmUuid));
        String bkd = getBackend(l3Uuid);
        return bkd;
    }

    private String getBackend(String l3Uuid) {
        return getBackend(l3Uuid, true);
    }

    private String getBackend(String l3Uuid, boolean errOnException) {
        String providerUuid = Q.New(NetworkServiceL3NetworkRefVO.class)
                .select(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid)
                .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3Uuid)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, VRouterRouteConstants.VROUTER_ROUTE_NETWORK_SERVICE_TYPE.toString())
                .limit(1)
                .findValue();

        if (errOnException) {
            DebugUtils.Assert(providerUuid != null, String.format("the L3 network[uuid: %s] does not has VRouterRoute service attached", l3Uuid));
        }

        String providerType = Q.New(NetworkServiceProviderVO.class)
                .select(NetworkServiceProviderVO_.type)
                .eq(NetworkServiceProviderVO_.uuid, providerUuid)
                .limit(1)
                .findValue();

        return providerType;
    }
}
