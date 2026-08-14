package org.zstack.sdk;

import org.zstack.sdk.SecurityGroupInventory;

public class UpdateSecurityGroupRulePriorityResult {
    public SecurityGroupInventory inventory;
    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
    public SecurityGroupInventory getInventory() {
        return this.inventory;
    }

}
