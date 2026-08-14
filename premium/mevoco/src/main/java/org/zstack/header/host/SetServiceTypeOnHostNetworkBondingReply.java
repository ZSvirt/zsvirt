package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class SetServiceTypeOnHostNetworkBondingReply extends MessageReply {
    List<HostNetworkBondingServiceRefInventory> inventory;

    public List<HostNetworkBondingServiceRefInventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<HostNetworkBondingServiceRefInventory> inventory) {
        this.inventory = inventory;
    }
}
