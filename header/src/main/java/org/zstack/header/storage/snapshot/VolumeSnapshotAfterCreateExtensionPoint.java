package org.zstack.header.storage.snapshot;

import org.zstack.header.volume.VolumeInventory;

public interface VolumeSnapshotAfterCreateExtensionPoint {
    void volumeSnapshotAfterCreate(VolumeInventory volume, VolumeSnapshotInventory sp);
}
