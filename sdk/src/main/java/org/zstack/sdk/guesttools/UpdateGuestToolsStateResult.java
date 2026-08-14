package org.zstack.sdk.guesttools;

import org.zstack.sdk.guesttools.GuestToolsStateInventory;

public class UpdateGuestToolsStateResult {
    public GuestToolsStateInventory inventory;
    public void setInventory(GuestToolsStateInventory inventory) {
        this.inventory = inventory;
    }
    public GuestToolsStateInventory getInventory() {
        return this.inventory;
    }

}
