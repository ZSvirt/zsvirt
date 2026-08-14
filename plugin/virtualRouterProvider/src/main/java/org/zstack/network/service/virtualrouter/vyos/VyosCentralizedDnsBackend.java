package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterStruct;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.dns.VirtualRouterCentralizedDnsBackend;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class VyosCentralizedDnsBackend extends VirtualRouterCentralizedDnsBackend {
    @Override
    public NetworkServiceProviderType getProviderType() {
        return VyosConstants.PROVIDER_TYPE;
    }

    @Override
    protected void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        struct.setApplianceVmType(VyosConstants.VYOS_VM_TYPE);
        struct.setProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        struct.setVirtualRouterOfferingSelector(new VyosOfferingSelector());
        struct.setApplianceVmAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);
        super.acquireVirtualRouterVm(struct, completion);
    }
}
