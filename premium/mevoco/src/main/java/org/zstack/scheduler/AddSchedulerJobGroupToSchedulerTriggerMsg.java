package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class AddSchedulerJobGroupToSchedulerTriggerMsg extends NeedReplyMessage {
    private String schedulerJobGroupUuid;
    private List<String> schedulerTriggerUuids;
    private String triggerNowId;

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

    public boolean isTriggerNow() {
        return triggerNowId != null;
    }

    public String getTriggerNowId() {
        return triggerNowId;
    }

    public void setTriggerNowId(String triggerNowId) {
        this.triggerNowId = triggerNowId;
    }
}
