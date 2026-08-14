package org.zstack.header.scheduler;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SchedulerJobGroupJobRefVO.class)
public class SchedulerJobGroupJobRefVO_ {
    public static volatile SingularAttribute<SchedulerJobGroupJobRefVO, String> schedulerJobGroupUuid;
    public static volatile SingularAttribute<SchedulerJobGroupJobRefVO, String> schedulerJobUuid;
    public static volatile SingularAttribute<SchedulerJobGroupJobRefVO, Integer> priority;
    public static volatile SingularAttribute<SchedulerJobGroupJobRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerJobGroupJobRefVO, Timestamp> lastOpDate;
}
