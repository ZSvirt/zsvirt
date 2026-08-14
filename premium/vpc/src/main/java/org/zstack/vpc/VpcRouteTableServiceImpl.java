package org.zstack.vpc;

import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.vrouterRoute.RouteTableServiceFactory;

public class VpcRouteTableServiceImpl implements RouteTableServiceFactory {
    @Override
    public String getApplianceVmType() {
        return VpcVRouterFactory.applianceVmType.toString();
    }

    @Override
    public String getProviderTypeFromVRouter(String vrouterVmUuid) {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }
}
