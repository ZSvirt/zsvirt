package org.zstack.sns;

import org.zstack.header.message.DeletionMessage;

public class SNSApplicationPlatformDeletionMsg extends DeletionMessage implements SNSApplicationPlatformMessage {
    private String uuid;

    @Override
    public String getApplicationPlatformUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
