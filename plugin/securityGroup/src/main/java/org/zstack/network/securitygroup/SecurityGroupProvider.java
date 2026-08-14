package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.message.Message;
import org.zstack.header.network.*;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.APIAttachNetworkServiceProviderToL2NetworkMsg;
import org.zstack.header.network.service.APIDetachNetworkServiceProviderFromL2NetworkMsg;
import org.zstack.header.network.service.NetworkServiceProvider;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SecurityGroupProvider implements NetworkServiceProvider {
    @Autowired
    private CloudBus bus;
    
    @Override
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public void attachToL2Network(L2NetworkInventory l2Network, APIAttachNetworkServiceProviderToL2NetworkMsg msg) throws NetworkException {
    }

    @Override
    public void detachFromL2Network(L2NetworkInventory l2Network, APIDetachNetworkServiceProviderFromL2NetworkMsg msg) throws NetworkException {
    }

}
