package org.zstack.header.baremetal.network;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteBaremetalNicMsg extends NeedReplyMessage {
    private String uuid;
    private String baremetalInstanceUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBaremetalInstanceUuid() {
        return baremetalInstanceUuid;
    }

    public void setBaremetalInstanceUuid(String baremetalInstanceUuid) {
        this.baremetalInstanceUuid = baremetalInstanceUuid;
    }
}
