package org.zstack.network.service.vip;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/11/20.
 */
public class ReleaseVipReply extends MessageReply {
    private VipInventory inventory;

    public VipInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }
}
