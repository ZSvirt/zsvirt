package org.zstack.header.vmscheduling;

import org.zstack.header.affinitygroup.AffinityGroupVO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmSchedulingRuleGroupVO.class)
public class VmSchedulingRuleGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmSchedulingRuleGroupVO, String> name;
    public static volatile SingularAttribute<VmSchedulingRuleGroupVO, String> description;
    public static volatile SingularAttribute<VmSchedulingRuleGroupVO, String> appliance;
    public static volatile SingularAttribute<VmSchedulingRuleGroupVO, String> zoneUuid;
    public static volatile SingularAttribute<VmSchedulingRuleGroupVO, String> srcUuid;
    public static volatile SingularAttribute<AffinityGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<AffinityGroupVO, Timestamp> lastOpDate;
}
