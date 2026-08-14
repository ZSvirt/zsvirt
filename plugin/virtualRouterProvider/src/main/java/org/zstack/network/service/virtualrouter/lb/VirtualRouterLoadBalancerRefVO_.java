package org.zstack.network.service.virtualrouter.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/9/2015.
 */
@StaticMetamodel(VirtualRouterLoadBalancerRefVO.class)
public class VirtualRouterLoadBalancerRefVO_ {
    public static volatile SingularAttribute<VirtualRouterLoadBalancerRefVO, Long> id;
    public static volatile SingularAttribute<VirtualRouterLoadBalancerRefVO, String> virtualRouterVmUuid;
    public static volatile SingularAttribute<VirtualRouterLoadBalancerRefVO, String> loadBalancerUuid;
    public static volatile SingularAttribute<VirtualRouterLoadBalancerRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VirtualRouterLoadBalancerRefVO, Timestamp> lastOpDate;
}
