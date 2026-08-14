package org.zstack.sdk;

import org.zstack.sdk.AccessControlListInventory;

public class CreateAccessControlListResult {
    public AccessControlListInventory inventory;
    public void setInventory(AccessControlListInventory inventory) {
        this.inventory = inventory;
    }
    public AccessControlListInventory getInventory() {
        return this.inventory;
    }

}
