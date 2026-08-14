package org.zstack.storage.primary.nfs;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * Created by xing5 on 2016/4/29.
 */
public class CreateTemporaryVolumeFromSnapshotMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String temporaryVolumeUuid;
    private VolumeSnapshotInventory snapshot;
    private String primaryStorageUuid;
    private String hypervisorType;

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getTemporaryVolumeUuid() {
        return temporaryVolumeUuid;
    }

    public void setTemporaryVolumeUuid(String temporaryVolumeUuid) {
        this.temporaryVolumeUuid = temporaryVolumeUuid;
    }

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
