package org.zstack.sns;

import org.zstack.header.message.DeletionMessage;

public class SNSTopicDeletionMsg extends DeletionMessage implements SNSTopicMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getTopicUuid() {
        return uuid;
    }
}
