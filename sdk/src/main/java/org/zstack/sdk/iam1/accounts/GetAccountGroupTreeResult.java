package org.zstack.sdk.iam1.accounts;

import org.zstack.sdk.iam1.accounts.AccountGroupView;

public class GetAccountGroupTreeResult {
    public AccountGroupView inventory;
    public void setInventory(AccountGroupView inventory) {
        this.inventory = inventory;
    }
    public AccountGroupView getInventory() {
        return this.inventory;
    }

}
