package org.zstack.header.host;

import org.zstack.header.message.Message;

public class RemovePingTaskMsg extends Message {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
