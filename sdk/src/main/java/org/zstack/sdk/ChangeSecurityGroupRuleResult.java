package org.zstack.sdk;

import org.zstack.sdk.SecurityGroupRuleInventory;

public class ChangeSecurityGroupRuleResult {
    public SecurityGroupRuleInventory inventory;
    public void setInventory(SecurityGroupRuleInventory inventory) {
        this.inventory = inventory;
    }
    public SecurityGroupRuleInventory getInventory() {
        return this.inventory;
    }

}
