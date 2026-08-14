package org.zstack.sdk.keyprovider.nkp.api;

import org.zstack.sdk.KeyProviderInventory;

public class UpdateNkpResult {
    public KeyProviderInventory inventory;
    public void setInventory(KeyProviderInventory inventory) {
        this.inventory = inventory;
    }
    public KeyProviderInventory getInventory() {
        return this.inventory;
    }

}
