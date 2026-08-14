package org.zstack.header.vpc.ha;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VpcHaGroupApplianceVmRefVO.class)
public class VpcHaGroupApplianceVmRefVO_ {
    public static volatile SingularAttribute<VpcHaGroupApplianceVmRefVO, String> uuid;
    public static volatile SingularAttribute<VpcHaGroupApplianceVmRefVO, String> vpcHaRouterUuid;
}
