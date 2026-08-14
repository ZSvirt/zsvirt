package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;
/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class AddSchedulerJobToSchedulerTriggerMsg extends NeedReplyMessage implements SchedulerMessage  {
    private String schedulerJobUuid;
    private String schedulerTriggerUuid;
    private boolean triggerNow;

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    @Override
    public String getSchedulerUuid() {
        return schedulerJobUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public boolean isTriggerNow() {
        return triggerNow;
    }

    public void setTriggerNow(boolean triggerNow) {
        this.triggerNow = triggerNow;
    }

    public String getTriggerNowId() {
        return triggerNow ? getId() : null;
    }
}

