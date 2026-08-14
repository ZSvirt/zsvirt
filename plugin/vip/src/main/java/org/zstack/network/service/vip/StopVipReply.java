package org.zstack.network.service.vip;

import org.zstack.header.message.MessageReply;

/**
 * @author: zhanyong.miao
 * @date: 2019-05-05
 **/
public class StopVipReply extends MessageReply {
    private VipInventory inventory;

    public VipInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }
}

