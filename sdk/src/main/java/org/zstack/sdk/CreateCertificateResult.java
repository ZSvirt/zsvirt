package org.zstack.sdk;

import org.zstack.sdk.CertificateInventory;

public class CreateCertificateResult {
    public CertificateInventory inventory;
    public void setInventory(CertificateInventory inventory) {
        this.inventory = inventory;
    }
    public CertificateInventory getInventory() {
        return this.inventory;
    }

}
