package org.zstack.compute.vm;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by LiangHanYu on 2021/6/22 15:36
 */
public class ExecuteCrashStrategyReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
