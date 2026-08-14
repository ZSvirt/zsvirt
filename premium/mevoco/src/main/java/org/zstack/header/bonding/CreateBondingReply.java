package org.zstack.header.bonding;

import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.header.message.MessageReply;

public class CreateBondingReply extends MessageReply {
    HostNetworkBondingInventory inventory;

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }
}
