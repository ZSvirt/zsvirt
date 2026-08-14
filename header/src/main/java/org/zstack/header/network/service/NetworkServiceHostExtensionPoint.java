package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;

public interface NetworkServiceHostExtensionPoint {
    String getNetworkServiceName();

    void afterHostConnected(HostInventory host, Completion completion);

    void beforeDeleteHost(HostInventory host, Completion completion);

}
