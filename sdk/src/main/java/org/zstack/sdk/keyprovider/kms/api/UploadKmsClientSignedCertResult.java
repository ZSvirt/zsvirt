package org.zstack.sdk.keyprovider.kms.api;

import org.zstack.sdk.KmsIdentityInventory;

public class UploadKmsClientSignedCertResult {
    public KmsIdentityInventory inventory;
    public void setInventory(KmsIdentityInventory inventory) {
        this.inventory = inventory;
    }
    public KmsIdentityInventory getInventory() {
        return this.inventory;
    }

}
