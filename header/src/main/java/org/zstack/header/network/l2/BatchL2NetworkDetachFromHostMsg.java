package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class BatchL2NetworkDetachFromHostMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private List<String> hostUuids;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }
}
