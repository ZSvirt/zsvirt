package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.VolumeBackupInventory;

public class SyncBackupFromImageStoreBackupStorageReply extends MessageReply {
    private VolumeBackupInventory inventory;

    public VolumeBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }
}
