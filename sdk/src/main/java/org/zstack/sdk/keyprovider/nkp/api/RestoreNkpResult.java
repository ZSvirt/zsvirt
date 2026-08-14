package org.zstack.sdk.keyprovider.nkp.api;

import org.zstack.sdk.NkpInventory;

public class RestoreNkpResult {
    public NkpInventory inventory;
    public void setInventory(NkpInventory inventory) {
        this.inventory = inventory;
    }
    public NkpInventory getInventory() {
        return this.inventory;
    }

}
