package org.zstack.log.server;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteLogServerConfigurationMsg extends NeedReplyMessage implements LogServerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLogServerUuid() {
        return uuid;
    }
}
