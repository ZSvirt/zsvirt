package org.zstack.sdk.storage.volumebackup;

import org.zstack.sdk.VolumeInventory;

public class CreateDataVolumeFromVolumeBackupResult {
    public VolumeInventory inventory;
    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeInventory getInventory() {
        return this.inventory;
    }

}
