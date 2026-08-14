package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 */
public class TakeSnapshotReply extends MessageReply {
    private VolumeSnapshotInventory inventory;
    private String newVolumeInstallPath;

    public String getNewVolumeInstallPath() {
        return newVolumeInstallPath;
    }

    public void setNewVolumeInstallPath(String newVolumeInstallPath) {
        this.newVolumeInstallPath = newVolumeInstallPath;
    }

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
}
