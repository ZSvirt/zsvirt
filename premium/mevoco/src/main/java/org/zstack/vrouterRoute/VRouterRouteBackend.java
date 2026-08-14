package org.zstack.vrouterRoute;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;

/**
 * Created by weiwang on 19/06/2017.
 */
public interface VRouterRouteBackend {
    void syncRouteEntries(List<VRouterRouteEntryTo> tos, List<String> vrouterUuids, boolean syncToHaRouter);

    void getVRouterRouteTable(String vrouterUuid, ReturnValueCompletion<String> completion);

    String getNetworkServiceProviderType();
}
