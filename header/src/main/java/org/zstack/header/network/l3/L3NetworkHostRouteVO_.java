package org.zstack.header.network.l3;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(L3NetworkHostRouteVO.class)
public class L3NetworkHostRouteVO_ {
    public static volatile SingularAttribute<L3NetworkHostRouteVO, Long> id;
    public static volatile SingularAttribute<L3NetworkHostRouteVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<L3NetworkHostRouteVO, String> prefix;
    public static volatile SingularAttribute<L3NetworkHostRouteVO, String> nexthop;
    public static volatile SingularAttribute<L3NetworkHostRouteVO, Timestamp> createDate;
    public static volatile SingularAttribute<L3NetworkHostRouteVO, Timestamp> lastOpDate;
}
