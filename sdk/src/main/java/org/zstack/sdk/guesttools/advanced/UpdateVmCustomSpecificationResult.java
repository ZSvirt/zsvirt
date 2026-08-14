package org.zstack.sdk.guesttools.advanced;

import org.zstack.sdk.guesttools.advanced.VmCustomSpecificationInventory;

public class UpdateVmCustomSpecificationResult {
    public VmCustomSpecificationInventory inventory;
    public void setInventory(VmCustomSpecificationInventory inventory) {
        this.inventory = inventory;
    }
    public VmCustomSpecificationInventory getInventory() {
        return this.inventory;
    }

}
