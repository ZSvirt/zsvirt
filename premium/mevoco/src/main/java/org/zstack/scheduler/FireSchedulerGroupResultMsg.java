package org.zstack.scheduler;

import org.zstack.header.message.Message;
import org.zstack.header.scheduler.SchedulerCanonicalEvents;

/**
 * Created by MaJin on 2020/4/7.
 */
public class FireSchedulerGroupResultMsg extends Message implements SchedulerJobGroupMessage {
    private String schedulerJobGroupUuid;
    private String fireInstanceId;
    private SchedulerCanonicalEvents.SchedulerGroupExecutedData data;

    @Override
    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public String getFireInstanceId() {
        return fireInstanceId;
    }

    public void setFireInstanceId(String fireInstanceId) {
        this.fireInstanceId = fireInstanceId;
    }

    public SchedulerCanonicalEvents.SchedulerGroupExecutedData getData() {
        return data;
    }

    public void setData(SchedulerCanonicalEvents.SchedulerGroupExecutedData data) {
        this.data = data;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }
}
