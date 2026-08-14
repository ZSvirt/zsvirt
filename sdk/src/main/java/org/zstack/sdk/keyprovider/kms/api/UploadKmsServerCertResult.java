package org.zstack.sdk.keyprovider.kms.api;

import org.zstack.sdk.KmsInventory;

public class UploadKmsServerCertResult {
    public KmsInventory inventory;
    public void setInventory(KmsInventory inventory) {
        this.inventory = inventory;
    }
    public KmsInventory getInventory() {
        return this.inventory;
    }

}
