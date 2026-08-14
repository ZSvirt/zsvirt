package org.zstack.sdk.keyprovider.kms.api;

import org.zstack.sdk.KeyProviderInventory;

public class CreateKmsResult {
    public KeyProviderInventory inventory;
    public void setInventory(KeyProviderInventory inventory) {
        this.inventory = inventory;
    }
    public KeyProviderInventory getInventory() {
        return this.inventory;
    }

}
