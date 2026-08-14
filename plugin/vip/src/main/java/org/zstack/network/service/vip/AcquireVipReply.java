package org.zstack.network.service.vip;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/11/19.
 */
public class AcquireVipReply extends MessageReply {
    private VipInventory vip;

    public VipInventory getVip() {
        return vip;
    }

    public void setVip(VipInventory vip) {
        this.vip = vip;
    }
}
