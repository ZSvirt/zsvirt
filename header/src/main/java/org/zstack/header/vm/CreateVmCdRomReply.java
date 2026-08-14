package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.cdrom.VmCdRomInventory;

public class CreateVmCdRomReply extends MessageReply {
    VmCdRomInventory inventory;

    public VmCdRomInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmCdRomInventory inventory) {
        this.inventory = inventory;
    }
}
