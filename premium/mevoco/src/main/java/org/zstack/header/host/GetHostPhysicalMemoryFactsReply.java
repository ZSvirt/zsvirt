package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * author:kaicai.hu
 * Date:2021/8/6
 */
public class GetHostPhysicalMemoryFactsReply extends MessageReply {
    List<HostPhysicalMemoryInventory> inventories;

    public List<HostPhysicalMemoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostPhysicalMemoryInventory> inventories) {
        this.inventories = inventories;
    }
}
