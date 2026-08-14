package org.zstack.sdk;

import org.zstack.sdk.VmSchedulingRuleInventory;

public class ChangeVmSchedulingRuleStateResult {
    public VmSchedulingRuleInventory inventory;
    public void setInventory(VmSchedulingRuleInventory inventory) {
        this.inventory = inventory;
    }
    public VmSchedulingRuleInventory getInventory() {
        return this.inventory;
    }

}
