package org.zstack.sdk;

import org.zstack.sdk.SessionInventory;

public class RenewSessionResult {
    public SessionInventory inventory;
    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }
    public SessionInventory getInventory() {
        return this.inventory;
    }

}
