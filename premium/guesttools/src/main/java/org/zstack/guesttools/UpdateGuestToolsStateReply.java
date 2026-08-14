package org.zstack.guesttools;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class UpdateGuestToolsStateReply extends MessageReply {
    GuestToolsStateInventory inventory;
    List<GuestToolsStateInventory> inventories;

    public GuestToolsStateInventory getInventory() {
        return inventory;
    }

    public void setInventory(GuestToolsStateInventory inventory) {
        this.inventory = inventory;
    }

    public List<GuestToolsStateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GuestToolsStateInventory> inventories) {
        this.inventories = inventories;
    }
}
