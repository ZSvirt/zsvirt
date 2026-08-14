package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2021/6/4.
 */
public class ChangeVolumeTypeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volume;
    private List<VolumeSnapshotInventory> snapshots = new ArrayList<>();
    private VolumeType targetType;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public VolumeType getTargetType() {
        return targetType;
    }

    public void setTargetType(VolumeType targetType) {
        this.targetType = targetType;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return volume.getPrimaryStorageUuid();
    }

    public List<VolumeSnapshotInventory> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<VolumeSnapshotInventory> snapshots) {
        this.snapshots = snapshots;
    }
}
