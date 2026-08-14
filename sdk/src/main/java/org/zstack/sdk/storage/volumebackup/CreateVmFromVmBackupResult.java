package org.zstack.sdk.storage.volumebackup;

import org.zstack.sdk.VmInstanceInventory;

public class CreateVmFromVmBackupResult {
    public VmInstanceInventory inventory;
    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstanceInventory getInventory() {
        return this.inventory;
    }

}
