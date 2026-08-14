package org.zstack.sdk;

import org.zstack.sdk.VmSchedulingRuleGroupInventory;

public class CreateVmSchedulingRuleGroupResult {
    public VmSchedulingRuleGroupInventory inventory;
    public void setInventory(VmSchedulingRuleGroupInventory inventory) {
        this.inventory = inventory;
    }
    public VmSchedulingRuleGroupInventory getInventory() {
        return this.inventory;
    }

}
