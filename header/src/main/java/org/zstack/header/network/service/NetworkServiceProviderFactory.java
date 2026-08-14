package org.zstack.header.network.service;

public interface NetworkServiceProviderFactory {
    NetworkServiceProviderType getType();

    void createNetworkServiceProvider(APIAddNetworkServiceProviderMsg msg, NetworkServiceProviderVO vo);

    NetworkServiceProvider getNetworkServiceProvider(NetworkServiceProviderVO vo);
}
