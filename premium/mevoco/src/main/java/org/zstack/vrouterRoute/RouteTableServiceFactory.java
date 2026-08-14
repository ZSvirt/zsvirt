package org.zstack.vrouterRoute;

public interface RouteTableServiceFactory {
    String getApplianceVmType();

    String getProviderTypeFromVRouter(String vrouterVmUuid);
}
