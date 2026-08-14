package org.zstack.header.network.service;

import org.zstack.header.message.Message;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.L2NetworkInventory;

public interface NetworkServiceProvider {
    void handleMessage(Message msg);

    void attachToL2Network(L2NetworkInventory l2Network, APIAttachNetworkServiceProviderToL2NetworkMsg msg) throws NetworkException;

    void detachFromL2Network(L2NetworkInventory l2Network, APIDetachNetworkServiceProviderFromL2NetworkMsg msg) throws NetworkException;
}
