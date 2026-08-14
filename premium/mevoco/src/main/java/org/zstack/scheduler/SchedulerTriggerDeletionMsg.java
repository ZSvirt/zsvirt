package org.zstack.scheduler;

import org.zstack.header.message.DeletionMessage;

public class SchedulerTriggerDeletionMsg extends DeletionMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
