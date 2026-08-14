package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

public class SetIpOnHostNetworkBondingReply extends MessageReply {
    HostNetworkBondingInventory inventory;

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }
}
