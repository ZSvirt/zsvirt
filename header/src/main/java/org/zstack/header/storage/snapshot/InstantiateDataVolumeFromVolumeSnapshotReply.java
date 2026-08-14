package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

/**
 */
public class InstantiateDataVolumeFromVolumeSnapshotReply extends MessageReply {
    private VolumeInventory inventory;
    private long actualSize;

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
