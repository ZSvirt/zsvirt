package org.zstack.sdk;

import org.zstack.sdk.VipQosInventory;

public class SetVipQosResult {
    public VipQosInventory inventory;
    public void setInventory(VipQosInventory inventory) {
        this.inventory = inventory;
    }
    public VipQosInventory getInventory() {
        return this.inventory;
    }

}
