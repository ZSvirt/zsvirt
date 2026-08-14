package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class UnscheduleJobGroupMsg extends NeedReplyMessage implements SchedulerJobGroupMessage {
    private String schedulerJobGroupUuid;
    private List<String> schedulerTriggerUuids;

    @Override
    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public List<String> getSchedulerTriggerUuids() {
        return schedulerTriggerUuids;
    }

    public void setSchedulerTriggerUuids(List<String> schedulerTriggerUuids) {
        this.schedulerTriggerUuids = schedulerTriggerUuids;
    }
}
