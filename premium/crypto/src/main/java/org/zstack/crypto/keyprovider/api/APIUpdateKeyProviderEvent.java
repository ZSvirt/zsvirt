package org.zstack.crypto.keyprovider.api;

import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.message.APIEvent;

public abstract class APIUpdateKeyProviderEvent extends APIEvent {
    private KeyProviderInventory inventory;

    public APIUpdateKeyProviderEvent() {
        super(null);
    }

    public APIUpdateKeyProviderEvent(String apiId) {
        super(apiId);
    }

    public KeyProviderInventory getInventory() {
        return inventory;
    }

    public void setInventory(KeyProviderInventory inventory) {
        this.inventory = inventory;
    }

}
