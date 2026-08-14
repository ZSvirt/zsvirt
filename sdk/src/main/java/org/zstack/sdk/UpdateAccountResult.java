package org.zstack.sdk;

import org.zstack.sdk.AccountInventory;

public class UpdateAccountResult {
    public AccountInventory inventory;
    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }
    public AccountInventory getInventory() {
        return this.inventory;
    }

}
