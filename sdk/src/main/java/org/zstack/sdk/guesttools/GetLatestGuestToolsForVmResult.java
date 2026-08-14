package org.zstack.sdk.guesttools;

import org.zstack.sdk.guesttools.GuestToolsInventory;

public class GetLatestGuestToolsForVmResult {
    public GuestToolsInventory inventory;
    public void setInventory(GuestToolsInventory inventory) {
        this.inventory = inventory;
    }
    public GuestToolsInventory getInventory() {
        return this.inventory;
    }

}
