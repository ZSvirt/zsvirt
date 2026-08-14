package org.zstack.header.vpc;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VpcRouterDnsVO.class)
public class VpcRouterDnsVO_ {
    public static volatile SingularAttribute<VpcRouterDnsVO, Long> id;
    public static volatile SingularAttribute<VpcRouterDnsVO, String> vpcRouterUuid;
    public static volatile SingularAttribute<VpcRouterDnsVO, String> dns;
    public static volatile SingularAttribute<VpcRouterDnsVO, Timestamp> createDate;
    public static volatile SingularAttribute<VpcRouterDnsVO, Timestamp> lastOpDate;
}
