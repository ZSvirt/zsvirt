package org.zstack.header.vmscheduling;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostSchedulingRuleGroupVO.class)
public class HostSchedulingRuleGroupVO_ {
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, String> uuid;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, String> name;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, String> description;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, String> zoneUUid;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostSchedulingRuleGroupVO, Timestamp> lastOpDate;

}
