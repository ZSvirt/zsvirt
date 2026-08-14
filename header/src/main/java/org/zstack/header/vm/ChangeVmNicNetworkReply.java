package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2022/6/24 10:59
 */
public class ChangeVmNicNetworkReply extends MessageReply {
    private VmNicInventory inventory;

    public VmNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicInventory inventory) {
        this.inventory = inventory;
    }
}
