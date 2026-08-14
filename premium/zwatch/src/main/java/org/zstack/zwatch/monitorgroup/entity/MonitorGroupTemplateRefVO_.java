package org.zstack.zwatch.monitorgroup.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(MonitorGroupTemplateRefVO.class)
public class MonitorGroupTemplateRefVO_ {
    public static volatile SingularAttribute<MonitorGroupTemplateRefVO, String> uuid;
    public static volatile SingularAttribute<MonitorGroupTemplateRefVO, String> templateUuid;
    public static volatile SingularAttribute<MonitorGroupTemplateRefVO, String> groupUuid;
    public static volatile SingularAttribute<MonitorGroupTemplateRefVO, Boolean> isApplied;
}
