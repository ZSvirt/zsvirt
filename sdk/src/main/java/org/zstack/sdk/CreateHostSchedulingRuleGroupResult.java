package org.zstack.sdk;

import org.zstack.sdk.HostSchedulingRuleGroupInventory;

public class CreateHostSchedulingRuleGroupResult {
    public HostSchedulingRuleGroupInventory inventory;
    public void setInventory(HostSchedulingRuleGroupInventory inventory) {
        this.inventory = inventory;
    }
    public HostSchedulingRuleGroupInventory getInventory() {
        return this.inventory;
    }

}
