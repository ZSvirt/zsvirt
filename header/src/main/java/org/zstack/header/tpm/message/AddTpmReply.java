package org.zstack.header.tpm.message;

import org.zstack.header.message.MessageReply;
import org.zstack.header.tpm.entity.TpmInventory;

public class AddTpmReply extends MessageReply {
    private TpmInventory inventory;

    public TpmInventory getInventory() {
        return inventory;
    }

    public void setInventory(TpmInventory inventory) {
        this.inventory = inventory;
    }
}
