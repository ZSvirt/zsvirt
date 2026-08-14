package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;

/**
 * Created by MaJin on 2021/6/23.
 */
public class CreateVolumeSnapshotGroupReply extends MessageReply {
    private VolumeSnapshotGroupInventory inventory;

    public VolumeSnapshotGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotGroupInventory inventory) {
        this.inventory = inventory;
    }
}
