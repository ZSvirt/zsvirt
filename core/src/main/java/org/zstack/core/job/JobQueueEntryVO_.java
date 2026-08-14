package org.zstack.core.job;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(JobQueueEntryVO.class)
public class JobQueueEntryVO_ {
    public static volatile SingularAttribute<JobQueueEntryVO, Long> id;
    public static volatile SingularAttribute<JobQueueEntryVO, Long> jobQueueId;
    public static volatile SingularAttribute<JobQueueEntryVO, Date> inDate;
    public static volatile SingularAttribute<JobQueueEntryVO, Date> doneDate;
    public static volatile SingularAttribute<JobQueueEntryVO, JobState> state;
    public static volatile SingularAttribute<JobQueueEntryVO, String> owner;
    public static volatile SingularAttribute<JobQueueEntryVO, Boolean> restartable;
    public static volatile SingularAttribute<JobQueueEntryVO, String> issuerManagementNodeId;
    public static volatile SingularAttribute<JobQueueEntryVO, String> name;
    public static volatile SingularAttribute<JobQueueEntryVO, String> errText;
    public static volatile SingularAttribute<JobQueueEntryVO, Byte[]> context;
}
