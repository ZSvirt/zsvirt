package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.volume.CreateVolumeSnapshotGroupMessage;

import java.util.List;

public interface VolumeSnapshotCreationExtensionPoint {
    void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion);

    void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply);

    void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup, ConsistentType consistentType, Completion completion);

    void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion);

    default List<Flow> beforeCreateVolumeSnapshotFlow(CreateVolumeSnapshotGroupMessage msg) {
        return null;
    }
}
