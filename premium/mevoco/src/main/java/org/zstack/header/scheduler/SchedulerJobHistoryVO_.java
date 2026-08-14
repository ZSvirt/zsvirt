package org.zstack.header.scheduler;

import javax.persistence.metamodel.SingularAttribute;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/4/22.
 */
public class SchedulerJobHistoryVO_ {
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, Long> id;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> triggerUuid;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> schedulerJobUuid;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> schedulerJobGroupUuid;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> jobType;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, Timestamp> startTime;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, Long> executeTime;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> targetResourceUuid;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> requestDump;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> resultDump;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, Boolean> success;
    public static volatile SingularAttribute<SchedulerJobHistoryVO_, String> fireInstanceId;
}
