package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeLuksAgentSpec;

public class CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String volumeUuid;
    private String primaryStorageUuid;
    private VolumeSnapshotInventory snapshot;
    private VolumeLuksAgentSpec volumeLuksAgentSpec;

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public VolumeLuksAgentSpec getVolumeLuksAgentSpec() {
        return volumeLuksAgentSpec;
    }

    public void setVolumeLuksAgentSpec(VolumeLuksAgentSpec volumeLuksAgentSpec) {
        this.volumeLuksAgentSpec = volumeLuksAgentSpec;
    }
}
