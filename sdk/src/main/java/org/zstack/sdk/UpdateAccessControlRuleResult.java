package org.zstack.sdk;

import org.zstack.sdk.AccessControlRuleInventory;

public class UpdateAccessControlRuleResult {
    public AccessControlRuleInventory inventory;
    public void setInventory(AccessControlRuleInventory inventory) {
        this.inventory = inventory;
    }
    public AccessControlRuleInventory getInventory() {
        return this.inventory;
    }

}
