package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class InstantiateNewCreatedVmInstanceReply extends MessageReply {
    private VmInstanceInventory vmInventory;

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }
}
