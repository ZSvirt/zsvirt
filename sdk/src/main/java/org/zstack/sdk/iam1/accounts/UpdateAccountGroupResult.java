package org.zstack.sdk.iam1.accounts;

import org.zstack.sdk.iam1.accounts.AccountGroupInventory;

public class UpdateAccountGroupResult {
    public AccountGroupInventory inventory;
    public void setInventory(AccountGroupInventory inventory) {
        this.inventory = inventory;
    }
    public AccountGroupInventory getInventory() {
        return this.inventory;
    }

}
