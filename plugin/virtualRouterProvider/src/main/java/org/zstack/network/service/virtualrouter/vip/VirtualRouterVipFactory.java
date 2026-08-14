package org.zstack.network.service.virtualrouter.vip;

import org.zstack.network.service.vip.VipBaseBackend;
import org.zstack.network.service.vip.VipFactory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;

/**
 * Created by xing5 on 2016/12/2.
 */
public class VirtualRouterVipFactory implements VipFactory {
    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public VipBaseBackend getVip(VipVO self) {
        return new VirtualRouterVipBaseBackend(self);
    }
}
