package org.zstack.scheduler;

import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

/**
 * Created by Mei Lei on 6/22/16.
 */
public interface SchedulerFacade extends Component {
    ErrorCode runScheduler(SchedulerTask job);
    void pauseSchedulerJob(String uuid);
    void resumeSchedulerJob(String uuid);
    void deleteSchedulerJobByResourceUuid(String uuid, Completion completion);
    List<String> getJobUuids(String triggerUuid);
    void handleJobUpdated(List<String> jobUuids, List<String> groupUuids, boolean needDisable);
    List<String> getResourceSchedulerJobTypes(String resourceUuid);
}
