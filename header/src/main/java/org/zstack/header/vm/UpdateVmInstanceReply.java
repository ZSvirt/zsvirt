package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2022/9/22 10:04
 */
public class UpdateVmInstanceReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
