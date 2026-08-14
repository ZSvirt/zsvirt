package org.zstack.ovf.message;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
public class CreateVmInstanceFromOvfReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
