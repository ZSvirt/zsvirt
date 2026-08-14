package org.zstack.sdk.storage.volumebackup;

import org.zstack.sdk.VolumeBackupInventory;

public class SyncBackupFromImageStoreBackupStorageResult {
    public VolumeBackupInventory inventory;
    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeBackupInventory getInventory() {
        return this.inventory;
    }

}
