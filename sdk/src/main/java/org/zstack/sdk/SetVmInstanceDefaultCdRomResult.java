package org.zstack.sdk;

import org.zstack.sdk.VmCdRomInventory;

public class SetVmInstanceDefaultCdRomResult {
    public VmCdRomInventory inventory;
    public void setInventory(VmCdRomInventory inventory) {
        this.inventory = inventory;
    }
    public VmCdRomInventory getInventory() {
        return this.inventory;
    }

}
