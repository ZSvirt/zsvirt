package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by david on 9/12/16.
 */
public class AddHostReply extends MessageReply {
    private HostInventory inventory;

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
}
