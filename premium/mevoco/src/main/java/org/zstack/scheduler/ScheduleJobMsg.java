package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.scheduler.SchedulerJobDesc;

import java.util.List;

/**
 * Created by MaJin on 2019/3/25.
 */
public class ScheduleJobMsg extends NeedReplyMessage {
    private String schedulerJobUuid;
    private List<String> schedulerTriggerUuids;
    private String triggerNowId;

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
