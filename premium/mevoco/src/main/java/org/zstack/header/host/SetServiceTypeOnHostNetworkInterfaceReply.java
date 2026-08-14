package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class SetServiceTypeOnHostNetworkInterfaceReply extends MessageReply {
    List<HostNetworkInterfaceServiceRefInventory> inventory;

    public List<HostNetworkInterfaceServiceRefInventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<HostNetworkInterfaceServiceRefInventory> inventory) {
        this.inventory = inventory;
    }
}
