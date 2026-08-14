package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 */
public class VmAttachNicReply extends MessageReply {
    private VmNicInventory inventroy;

    public VmNicInventory getInventroy() {
        return inventroy;
    }

    public void setInventroy(VmNicInventory inventroy) {
        this.inventroy = inventroy;
    }
}
