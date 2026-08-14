package org.zstack.scheduler;

import org.zstack.header.message.DeletionMessage;

public class SchedulerDeletionMsg extends DeletionMessage implements SchedulerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
}
