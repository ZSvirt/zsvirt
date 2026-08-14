package org.zstack.network.service.vip;

import org.zstack.header.message.MessageReply;

/**
 * Created by shixin on 2019/05/17.
 */
public class CreateVipReply extends MessageReply {
    VipInventory vip;

    public VipInventory getVip() {
        return vip;
    }

    public void setVip(VipInventory vip) {
        this.vip = vip;
    }
}
