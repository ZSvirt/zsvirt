package org.zstack.zwatch.message;

import org.zstack.header.message.DeletionMessage;
import org.zstack.zwatch.alarm.EventSubscriptionMessage;

public class EventSubscriptionDeletionMsg extends DeletionMessage implements EventSubscriptionMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSubscriptionUuid() {
        return uuid;
    }
}
