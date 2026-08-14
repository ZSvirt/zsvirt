package org.zstack.network.service.virtualrouter.ha;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.service.NetworkServiceProviderType;

public interface VirtualRouterHaConstant {
    @PythonClass
    String VIRTUAL_ROUTER_HA_PROVIDER_TYPE_NAME = "VirtualRouterHa";
    NetworkServiceProviderType VIRTUAL_ROUTER_HA_PROVIDER_TYPE = new NetworkServiceProviderType(VIRTUAL_ROUTER_HA_PROVIDER_TYPE_NAME);

}
