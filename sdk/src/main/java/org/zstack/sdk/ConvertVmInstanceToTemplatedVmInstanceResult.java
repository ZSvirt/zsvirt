package org.zstack.sdk;

import org.zstack.sdk.TemplatedVmInstanceInventory;

public class ConvertVmInstanceToTemplatedVmInstanceResult {
    public TemplatedVmInstanceInventory inventory;
    public void setInventory(TemplatedVmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public TemplatedVmInstanceInventory getInventory() {
        return this.inventory;
    }

}
