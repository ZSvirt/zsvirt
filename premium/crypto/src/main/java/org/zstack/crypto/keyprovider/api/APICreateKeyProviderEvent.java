package org.zstack.crypto.keyprovider.api;

import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.message.APIEvent;

public abstract class APICreateKeyProviderEvent extends APIEvent {
    private KeyProviderInventory inventory;

    public APICreateKeyProviderEvent() {
        super(null);
    }

    public APICreateKeyProviderEvent(String apiId) {
        super(apiId);
    }

    public KeyProviderInventory getInventory() {
        return inventory;
    }

    public void setInventory(KeyProviderInventory inventory) {
        this.inventory = inventory;
    }

}
