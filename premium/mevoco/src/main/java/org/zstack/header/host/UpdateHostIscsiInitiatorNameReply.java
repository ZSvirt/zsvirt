package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostInventory;

public class UpdateHostIscsiInitiatorNameReply extends MessageReply {
    KVMHostInventory inventory;

    public KVMHostInventory getInventory() {
        return inventory;
    }

    public void setInventory(KVMHostInventory inventory) {
        this.inventory = inventory;
    }
}
