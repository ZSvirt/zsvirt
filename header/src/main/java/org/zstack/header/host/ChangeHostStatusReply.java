package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2023/7/4
 */
public class ChangeHostStatusReply extends MessageReply {
    private HostInventory inventory;

    public ChangeHostStatusReply() {
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
}
