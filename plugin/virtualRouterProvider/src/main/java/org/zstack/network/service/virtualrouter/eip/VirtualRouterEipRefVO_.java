package org.zstack.network.service.virtualrouter.eip;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VirtualRouterEipRefVO.class)
public class VirtualRouterEipRefVO_ {
    public static volatile SingularAttribute<VirtualRouterEipRefVO, String> eipUuid;
    public static volatile SingularAttribute<VirtualRouterEipRefVO, String> virtualRouterVmUuid;
}
