package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.host.Host;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;


public interface NetworkServiceHostRouteBackend {
    NetworkServiceProviderType getProviderType();

    void addHostRoute(String l3Uuid, List<AddHostRouteMsg> routes, Completion completion);

    void removeHostRoute(String l3Uuid, List<RemoveHostRouteMsg> routes, Completion completion);
}
