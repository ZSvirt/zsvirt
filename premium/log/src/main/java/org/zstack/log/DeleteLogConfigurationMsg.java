package org.zstack.log;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteLogConfigurationMsg extends NeedReplyMessage {
    private long configId;

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }
}
