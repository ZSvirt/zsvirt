package org.zstack.sdk;

import org.zstack.sdk.VipInventory;

public class ChangeVipStateResult {
    public VipInventory inventory;
    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }
    public VipInventory getInventory() {
        return this.inventory;
    }

}
