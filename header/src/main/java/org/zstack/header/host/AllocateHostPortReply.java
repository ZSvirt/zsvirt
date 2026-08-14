package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class AllocateHostPortReply extends MessageReply {
    List<Long> hostPortIds;

    public List<Long> getHostPortIds() {
        return hostPortIds;
    }

    public void setHostPortIds(List<Long> hostPortIds) {
        this.hostPortIds = hostPortIds;
    }
}
