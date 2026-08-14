package org.zstack.sns;

import org.zstack.header.message.DeletionMessage;

public class SNSApplicationEndpointDeletionMsg extends DeletionMessage implements SNSApplicationEndpointMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return uuid;
    }
}
