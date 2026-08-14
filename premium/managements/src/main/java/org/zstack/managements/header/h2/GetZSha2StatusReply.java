package org.zstack.managements.header.h2;

import org.zstack.header.message.MessageReply;
import org.zstack.managements.entity.common.ManagementsStatusView;

public class GetZSha2StatusReply extends MessageReply {
    private ManagementsStatusView inventory;

    public ManagementsStatusView getInventory() {
        return inventory;
    }

    public void setInventory(ManagementsStatusView inventory) {
        this.inventory = inventory;
    }
}
