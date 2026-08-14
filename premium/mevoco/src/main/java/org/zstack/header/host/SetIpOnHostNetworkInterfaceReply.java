package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

public class SetIpOnHostNetworkInterfaceReply extends MessageReply {
    HostNetworkInterfaceInventory inventory;

    public HostNetworkInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkInterfaceInventory inventory) {
        this.inventory = inventory;
    }
}
