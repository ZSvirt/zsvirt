package org.zstack.header.scheduler;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SchedulerJobGroupSchedulerTriggerRefVO.class)
public class SchedulerJobGroupSchedulerTriggerRefVO_ {
    public static volatile SingularAttribute<SchedulerJobGroupSchedulerTriggerRefVO, String> schedulerJobGroupUuid;
    public static volatile SingularAttribute<SchedulerJobGroupSchedulerTriggerRefVO, String> schedulerTriggerUuid;
    public static volatile SingularAttribute<SchedulerJobGroupSchedulerTriggerRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerJobGroupSchedulerTriggerRefVO, Timestamp> lastOpDate;
}
