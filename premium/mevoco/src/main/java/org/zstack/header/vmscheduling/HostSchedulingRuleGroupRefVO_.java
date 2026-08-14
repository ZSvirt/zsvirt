package org.zstack.header.vmscheduling;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostSchedulingRuleGroupRefVO.class)
public class HostSchedulingRuleGroupRefVO_ {
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, String> hostGroupUuid;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, String> hostUuid;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, Timestamp> lastOpDate;

}
