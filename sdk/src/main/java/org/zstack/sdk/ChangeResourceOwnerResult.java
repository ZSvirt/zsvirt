package org.zstack.sdk;

import org.zstack.sdk.AccountResourceRefInventory;

public class ChangeResourceOwnerResult {
    public AccountResourceRefInventory inventory;
    public void setInventory(AccountResourceRefInventory inventory) {
        this.inventory = inventory;
    }
    public AccountResourceRefInventory getInventory() {
        return this.inventory;
    }

}
