package org.zstack.header.vmscheduling;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmSchedulingRuleGroupRefVO.class)
public class VmSchedulingRuleGroupRefVO_ {
    public static volatile SingularAttribute<VmSchedulingRuleGroupRefVO, Long> id;
    public static volatile SingularAttribute<VmSchedulingRuleGroupRefVO, String> vmGroupUuid;
    public static volatile SingularAttribute<VmSchedulingRuleGroupRefVO, String> vmUuid;
    public static volatile SingularAttribute<VmSchedulingRuleGroupRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmSchedulingRuleGroupRefVO, Timestamp> lastOpDate;
}
