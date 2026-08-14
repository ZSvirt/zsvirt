package org.zstack.sdk.external.service;

import org.zstack.sdk.external.service.ExternalServiceConfigurationInventory;

public class UpdateExternalServiceConfigurationResult {
    public ExternalServiceConfigurationInventory inventory;
    public void setInventory(ExternalServiceConfigurationInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalServiceConfigurationInventory getInventory() {
        return this.inventory;
    }

}
