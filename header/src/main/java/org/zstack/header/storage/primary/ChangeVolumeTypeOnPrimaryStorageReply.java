package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2021/6/4.
 */
public class ChangeVolumeTypeOnPrimaryStorageReply extends MessageReply {
    private VolumeInventory volume;
    private List<VolumeSnapshotInventory> snapshots = new ArrayList<>();
    private String installPathToGc;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public List<VolumeSnapshotInventory> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<VolumeSnapshotInventory> snapshots) {
        this.snapshots = snapshots;
    }

    public String getInstallPathToGc() {
        return installPathToGc;
    }

    public void setInstallPathToGc(String installPathToGc) {
        this.installPathToGc = installPathToGc;
    }
}
