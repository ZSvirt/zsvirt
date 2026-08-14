package org.zstack.sdk;

import org.zstack.sdk.SftpBackupStorageInventory;

public class ReconnectSftpBackupStorageResult {
    public SftpBackupStorageInventory inventory;
    public void setInventory(SftpBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public SftpBackupStorageInventory getInventory() {
        return this.inventory;
    }

}
