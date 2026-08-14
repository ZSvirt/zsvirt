package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.service.*;

public class VirtualRouterProviderFactory implements NetworkServiceProviderFactory {
	public static final NetworkServiceProviderType type = new NetworkServiceProviderType(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);

    @Autowired
    private DatabaseFacade dbf;

	public NetworkServiceProviderType getType() {
		return type;
	}

	public void createNetworkServiceProvider(APIAddNetworkServiceProviderMsg msg, NetworkServiceProviderVO vo) {
	}

	public NetworkServiceProvider getNetworkServiceProvider(NetworkServiceProviderVO vo) {
		return new VirtualRouterProvider(vo);
	}
}