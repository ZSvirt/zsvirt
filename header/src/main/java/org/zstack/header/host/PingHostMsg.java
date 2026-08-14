package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class PingHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
