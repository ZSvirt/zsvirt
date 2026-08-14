package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

/**
 */
public class MergeVolumeSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeSnapshotInventory from;
    private VolumeInventory to;
    private boolean fullRebase;

    public VolumeSnapshotInventory getFrom() {
        return from;
    }

    public void setFrom(VolumeSnapshotInventory from) {
        this.from = from;
    }

    public VolumeInventory getTo() {
        return to;
    }

    public void setTo(VolumeInventory to) {
        this.to = to;
    }

    public boolean isFullRebase() {
        return fullRebase;
    }

    public void setFullRebase(boolean fullRebase) {
        this.fullRebase = fullRebase;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return from.getPrimaryStorageUuid();
    }
}
