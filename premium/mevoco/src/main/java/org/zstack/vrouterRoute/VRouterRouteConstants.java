package org.zstack.vrouterRoute;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.service.NetworkServiceType;

/**
 * Created by weiwang on 16/06/2017.
 */
@PythonClass
public class VRouterRouteConstants {
    @PythonClass
    public static final String SERVICE_ID = "vrouterRoute";
    public static final NetworkServiceType VROUTER_ROUTE_NETWORK_SERVICE_TYPE = new NetworkServiceType("VRouterRoute");
}
