package org.zstack.header.storage.backup;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListBackupStorageReply extends APIReply {
    private List<BackupStorageInventory> inventories;

    public List<BackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BackupStorageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIListBackupStorageReply __example__() {
        APIListBackupStorageReply msg = new APIListBackupStorageReply();
        return msg;
    }
    
}