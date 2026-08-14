package org.zstack.sdk;

import org.zstack.sdk.ImagePackageInventory;

public class UpdateImagePackageResult {
    public ImagePackageInventory inventory;
    public void setInventory(ImagePackageInventory inventory) {
        this.inventory = inventory;
    }
    public ImagePackageInventory getInventory() {
        return this.inventory;
    }

}
