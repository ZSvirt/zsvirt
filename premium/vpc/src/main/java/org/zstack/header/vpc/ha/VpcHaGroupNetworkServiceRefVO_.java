package org.zstack.header.vpc.ha;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(VpcHaGroupNetworkServiceRefVO.class)
public class VpcHaGroupNetworkServiceRefVO_ {
    public static volatile SingularAttribute<VpcHaGroupNetworkServiceRefVO, Long> id;
    public static volatile SingularAttribute<VpcHaGroupNetworkServiceRefVO, String> vpcHaRouterUuid;
    public static volatile SingularAttribute<VpcHaGroupNetworkServiceRefVO, String> networkServiceName;
    public static volatile SingularAttribute<VpcHaGroupNetworkServiceRefVO, String> networkServiceUuid;
    public static volatile SingularAttribute<VpcHaGroupNetworkServiceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VpcHaGroupNetworkServiceRefVO, Timestamp> lastOpDate;
}
