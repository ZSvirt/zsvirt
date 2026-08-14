package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.Message;
import org.zstack.header.network.*;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.APIAttachNetworkServiceProviderToL2NetworkMsg;
import org.zstack.header.network.service.APIDetachNetworkServiceProviderFromL2NetworkMsg;
import org.zstack.header.network.service.NetworkServiceProvider;
import org.zstack.header.network.service.NetworkServiceProviderVO;

public class VirtualRouterProvider implements NetworkServiceProvider {
	
	private NetworkServiceProviderVO self;

	VirtualRouterProvider(NetworkServiceProviderVO vo) {
		self = vo;
	}
	
	public void handleMessage(Message msg) {
	}

	public void attachToL2Network(L2NetworkInventory l2Network, APIAttachNetworkServiceProviderToL2NetworkMsg msg) {
	}

	public void detachFromL2Network(L2NetworkInventory l2Network, APIDetachNetworkServiceProviderFromL2NetworkMsg msg) throws NetworkException {
	}
}
