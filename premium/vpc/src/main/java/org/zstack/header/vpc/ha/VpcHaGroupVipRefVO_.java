package org.zstack.header.vpc.ha;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VpcHaGroupVipRefVO.class)
public class VpcHaGroupVipRefVO_ {
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, Long> id;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, String> vpcHaRouterUuid;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, String> vipUuid;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, String> ip;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, String> netmask;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VpcHaGroupVipRefVO, Timestamp> lastOpDate;
}
