package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;

public class AllocateIpReply extends MessageReply {
    private UsedIpInventory ipInventory;

    public UsedIpInventory getIpInventory() {
        return ipInventory;
    }

    public void setIpInventory(UsedIpInventory ipInventory) {
        this.ipInventory = ipInventory;
    }
}
