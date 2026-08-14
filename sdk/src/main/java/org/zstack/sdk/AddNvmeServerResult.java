package org.zstack.sdk;

import org.zstack.sdk.NvmeServerInventory;

public class AddNvmeServerResult {
    public NvmeServerInventory inventory;
    public void setInventory(NvmeServerInventory inventory) {
        this.inventory = inventory;
    }
    public NvmeServerInventory getInventory() {
        return this.inventory;
    }

}
