package org.zstack.sdk;

import org.zstack.sdk.VmVfNicInventory;

public class ChangeVfNicHaStateResult {
    public VmVfNicInventory inventory;
    public void setInventory(VmVfNicInventory inventory) {
        this.inventory = inventory;
    }
    public VmVfNicInventory getInventory() {
        return this.inventory;
    }

}
