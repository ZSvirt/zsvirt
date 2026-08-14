package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VirtualRouterVmVO.class)
public class VirtualRouterVmVO_ extends ApplianceVmVO_ {
    public static volatile SingularAttribute<VirtualRouterVmVO, String> publicNetworkUuid;
}
