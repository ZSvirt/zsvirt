package org.zstack.sdk;

import org.zstack.sdk.ZoneInventory;

public class ChangeZoneStateResult {
    public ZoneInventory inventory;
    public void setInventory(ZoneInventory inventory) {
        this.inventory = inventory;
    }
    public ZoneInventory getInventory() {
        return this.inventory;
    }

}
