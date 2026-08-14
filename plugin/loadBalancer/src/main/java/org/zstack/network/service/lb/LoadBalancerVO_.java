package org.zstack.network.service.lb;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/9/2015.
 */
@StaticMetamodel(LoadBalancerVO.class)
public class LoadBalancerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<LoadBalancerVO, String> name;
    public static volatile SingularAttribute<LoadBalancerVO, String> description;
    public static volatile SingularAttribute<LoadBalancerVO, String> providerType;
    public static volatile SingularAttribute<LoadBalancerVO, String> vipUuid;
    public static volatile SingularAttribute<LoadBalancerVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerVO, LoadBalancerState> state;
    public static volatile SingularAttribute<LoadBalancerVO, LoadBalancerType> type;
    public static volatile SingularAttribute<LoadBalancerVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerVO, Timestamp> lastOpDate;
}
