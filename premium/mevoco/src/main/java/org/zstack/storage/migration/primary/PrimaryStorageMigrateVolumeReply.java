package org.zstack.storage.migration.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by GuoYi on 12/7/17.
 */
public class PrimaryStorageMigrateVolumeReply extends MessageReply {
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
