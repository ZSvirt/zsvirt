package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetHostPhysicalCpuFactsReply extends MessageReply {
    List<HostPhysicalCpuInventory> inventories;

    public List<HostPhysicalCpuInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostPhysicalCpuInventory> inventories) {
        this.inventories = inventories;
    }
}
