package org.zstack.scheduler;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Date;

/**
 * Created by Mei Lei on 7/11/16.
 */
public interface SchedulerJob {
    Timestamp getCreateDate();
    String getQueueName();
    String getTargetResourceUuid();
    String getTriggerUuid();
    String getUuid();
    String getType();
    void setTriggerUuid(String triggerUuid);
    void setUuid(String uuid);
    void setFireInstanceId(String fireInstanceId);
    void run(NopeCompletion completion);

    default void updateSchedulerJob(Map<String, String> jobParameters) {

    }

    default boolean lastJobIsRunning() {
        return false;
    }

    default String getZoneUuid() {
        return null;
    }
}
