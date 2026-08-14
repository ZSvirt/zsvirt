package org.zstack.header.bonding;

import org.zstack.header.message.MessageReply;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

public class UpdateBondingReply extends MessageReply {
    HostNetworkBondingInventory inventory;

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }
}
