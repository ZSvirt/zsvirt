package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * Created by david on 10/7/16.
 */
public class VolumeCreateSnapshotReply extends MessageReply {
    private VolumeSnapshotInventory inventory;

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
}
