package org.zstack.storage.device.localRaid;

import org.zstack.header.message.NeedReplyMessage;

public class SelfTestLocalRaidMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
