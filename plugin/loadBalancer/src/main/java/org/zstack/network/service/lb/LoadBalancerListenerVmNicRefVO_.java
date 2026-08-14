package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@StaticMetamodel(LoadBalancerListenerVmNicRefVO.class)
public class LoadBalancerListenerVmNicRefVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerVmNicRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerListenerVmNicRefVO, String> listenerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerVmNicRefVO, String> vmNicUuid;
    public static volatile SingularAttribute<LoadBalancerListenerVmNicRefVO, LoadBalancerVmNicStatus> status;
    public static volatile SingularAttribute<LoadBalancerListenerVmNicRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerVmNicRefVO, Timestamp> lastOpDate;
}
