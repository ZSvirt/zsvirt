package org.zstack.header.vmscheduling;


import org.zstack.header.affinitygroup.AffinityGroupVO;
import org.zstack.header.affinitygroup.AffinityGroupVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmSchedulingRuleVO.class)
public class VmSchedulingRuleVO_ extends AffinityGroupVO_ {
    public static volatile SingularAttribute<AffinityGroupVO, VMSchedulingRuleType> rule;
    public static volatile SingularAttribute<AffinityGroupVO, VMSchedulingRuleMode> mode;

}
