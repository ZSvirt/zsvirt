package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.message.MessageReply;

public class CreateHostKernelInterfaceReply extends MessageReply {

    private HostKernelInterfaceInventory inventory;

    public HostKernelInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostKernelInterfaceInventory inventory) {
        this.inventory = inventory;
    }

}
