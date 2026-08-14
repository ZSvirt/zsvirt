package org.zstack.header.allocator;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;

public class AllocateHostReply extends MessageReply {
    private HostInventory host;

    public AllocateHostReply() {
    }

    public AllocateHostReply(HostInventory host) {
        super();
        this.host = host;
    }

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
    }
}
