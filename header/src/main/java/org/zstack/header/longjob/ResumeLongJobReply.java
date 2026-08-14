package org.zstack.header.longjob;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2020/4/23.
 */
public class ResumeLongJobReply extends MessageReply {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }
}
