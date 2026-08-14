package org.zstack.sdk.tpm.api;

import org.zstack.sdk.tpm.entity.TpmCapabilityView;

public class GetTpmCapabilityResult {
    public TpmCapabilityView inventory;
    public void setInventory(TpmCapabilityView inventory) {
        this.inventory = inventory;
    }
    public TpmCapabilityView getInventory() {
        return this.inventory;
    }

}
