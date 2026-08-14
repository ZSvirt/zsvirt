package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class AllocateHostPortMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private int allocateCount;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public int getAllocateCount() {
        return allocateCount;
    }

    public void setAllocateCount(int allocateCount) {
        this.allocateCount = allocateCount;
    }
}
