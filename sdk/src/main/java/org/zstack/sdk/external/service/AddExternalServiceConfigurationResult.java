package org.zstack.sdk.external.service;

import org.zstack.sdk.external.service.ExternalServiceConfigurationInventory;

public class AddExternalServiceConfigurationResult {
    public ExternalServiceConfigurationInventory inventory;
    public void setInventory(ExternalServiceConfigurationInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalServiceConfigurationInventory getInventory() {
        return this.inventory;
    }

}
