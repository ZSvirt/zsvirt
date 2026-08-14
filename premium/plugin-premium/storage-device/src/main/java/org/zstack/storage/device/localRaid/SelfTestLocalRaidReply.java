package org.zstack.storage.device.localRaid;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class SelfTestLocalRaidReply extends MessageReply {
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
