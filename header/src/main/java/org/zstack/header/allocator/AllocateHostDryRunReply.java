package org.zstack.header.allocator;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;

import java.util.List;

public class AllocateHostDryRunReply extends MessageReply {
    private List<HostInventory> hosts;

    public AllocateHostDryRunReply() {
    }

    public List<HostInventory> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostInventory> hosts) {
        this.hosts = hosts;
    }
}
