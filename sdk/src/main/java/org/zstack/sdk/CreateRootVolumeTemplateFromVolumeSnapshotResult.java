package org.zstack.sdk;

import org.zstack.sdk.ImageInventory;

public class CreateRootVolumeTemplateFromVolumeSnapshotResult {
    public ImageInventory inventory;
    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
    public ImageInventory getInventory() {
        return this.inventory;
    }

    public java.util.List failures;
    public void setFailures(java.util.List failures) {
        this.failures = failures;
    }
    public java.util.List getFailures() {
        return this.failures;
    }

}
