package org.zstack.monitoring;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/12.
 */
@StaticMetamodel(MonitorTriggerActionRefVO.class)
public class MonitorTriggerActionRefVO_ {
    public static volatile SingularAttribute<MonitorTriggerActionRefVO, String> actionUuid;
    public static volatile SingularAttribute<MonitorTriggerActionRefVO, String> triggerUuid;
    public static volatile SingularAttribute<MonitorTriggerActionRefVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<MonitorTriggerActionRefVO, Timestamp> createDate;
}
