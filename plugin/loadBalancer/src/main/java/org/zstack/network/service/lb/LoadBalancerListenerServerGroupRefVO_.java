package org.zstack.network.service.lb;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerListenerServerGroupRefVO.class)
public class LoadBalancerListenerServerGroupRefVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupRefVO, String> listenerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupRefVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupRefVO, Timestamp> lastOpDate;
}
