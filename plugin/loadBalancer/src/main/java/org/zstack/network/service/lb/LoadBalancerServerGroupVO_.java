package org.zstack.network.service.lb;


import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerServerGroupVO.class)
public class LoadBalancerServerGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<LoadBalancerServerGroupVO, String> name;
    public static volatile SingularAttribute<LoadBalancerServerGroupVO, String> description;
    public static volatile SingularAttribute<LoadBalancerServerGroupVO, String> loadBalancerUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerServerGroupVO, Timestamp> lastOpDate;
}
