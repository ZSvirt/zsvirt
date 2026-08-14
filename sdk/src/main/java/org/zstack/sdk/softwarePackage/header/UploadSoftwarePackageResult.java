package org.zstack.sdk.softwarePackage.header;

import org.zstack.sdk.softwarePackage.header.SoftwarePackageInventory;

public class UploadSoftwarePackageResult {
    public SoftwarePackageInventory inventory;
    public void setInventory(SoftwarePackageInventory inventory) {
        this.inventory = inventory;
    }
    public SoftwarePackageInventory getInventory() {
        return this.inventory;
    }

}
