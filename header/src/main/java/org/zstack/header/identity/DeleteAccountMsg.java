package org.zstack.header.identity;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteAccountMsg extends NeedReplyMessage implements AccountMessage {
    private String uuid;

    @Override
    public String getAccountUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
