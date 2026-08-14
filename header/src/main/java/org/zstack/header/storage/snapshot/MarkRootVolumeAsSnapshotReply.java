package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

/**
 * Created by liangbo.zhou on 17-6-23.
 */
public class MarkRootVolumeAsSnapshotReply extends MessageReply {
    private VolumeSnapshotInventory inventory;

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
}
