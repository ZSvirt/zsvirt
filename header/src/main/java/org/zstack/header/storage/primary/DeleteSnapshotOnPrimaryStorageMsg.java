package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 */
public class DeleteSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeSnapshotInventory snapshot;
    private Long capacityToReturn;

    @Override
    public String getPrimaryStorageUuid() {
        return snapshot.getPrimaryStorageUuid();
    }

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }

    public long getCapacityToReturn() {
        return capacityToReturn == null ? snapshot.getSize() : capacityToReturn;
    }

    public void setCapacityToReturn(long capacityToReturn) {
        this.capacityToReturn = capacityToReturn;
    }
}
