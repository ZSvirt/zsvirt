package org.zstack.sdk;

import org.zstack.sdk.AccessControlListEntryInventory;

public class ChangeAccessControlListRedirectRuleResult {
    public AccessControlListEntryInventory inventory;
    public void setInventory(AccessControlListEntryInventory inventory) {
        this.inventory = inventory;
    }
    public AccessControlListEntryInventory getInventory() {
        return this.inventory;
    }

}
