package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class GetHostNumaTopologyMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
