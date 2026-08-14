package org.zstack.header.vm;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 */
public class GetVmMigrationTargetHostReply extends MessageReply {
    private List<HostInventory> hosts;

    public List<HostInventory> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostInventory> hosts) {
        this.hosts = hosts;
    }
}
