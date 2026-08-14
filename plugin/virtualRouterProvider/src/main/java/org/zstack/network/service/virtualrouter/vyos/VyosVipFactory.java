package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipFactory;

/**
 * Created by xing5 on 2016/12/3.
 */
public class VyosVipFactory extends VirtualRouterVipFactory {
    @Override
    public String getNetworkServiceProviderType() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }
}
