package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by MaJin on 2019/3/25.
 */
public class UnscheduleJobMsg extends NeedReplyMessage {
    private String schedulerJobUuid;
    private List<String> schedulerTriggerUuids;

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public List<String> getSchedulerTriggerUuids() {
        return schedulerTriggerUuids;
    }

    public void setSchedulerTriggerUuids(List<String> schedulerTriggerUuids) {
        this.schedulerTriggerUuids = schedulerTriggerUuids;
    }
}
