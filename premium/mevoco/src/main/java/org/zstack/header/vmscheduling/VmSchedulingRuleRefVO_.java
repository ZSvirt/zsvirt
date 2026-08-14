package org.zstack.header.vmscheduling;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmSchedulingRuleRefVO.class)
public class VmSchedulingRuleRefVO_ {
    public static volatile SingularAttribute<VmSchedulingRuleRefVO, Long> id;
    public static volatile SingularAttribute<VmSchedulingRuleRefVO, String> vmGroupUuid;
    public static volatile SingularAttribute<VmSchedulingRuleRefVO, String> hostGroupUuid;
    public static volatile SingularAttribute<VmSchedulingRuleRefVO, String> vmSchedulingRuleUuid;
    public static volatile SingularAttribute<VmSchedulingRuleRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmSchedulingRuleRefVO, Timestamp> lastOpDate;
}
