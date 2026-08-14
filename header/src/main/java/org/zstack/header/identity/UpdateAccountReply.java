package org.zstack.header.identity;

import org.zstack.header.message.MessageReply;

public class UpdateAccountReply extends MessageReply {
    private AccountInventory inventory;

    public AccountInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }
}
