package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class ReconnectHostReply extends MessageReply {
    private HostInventory inventory;

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
}
