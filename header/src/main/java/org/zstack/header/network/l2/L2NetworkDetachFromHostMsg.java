package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

public class L2NetworkDetachFromHostMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private String hostUuid;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
