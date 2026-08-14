package org.zstack.storage.snapshot;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 2019/1/10.
 */
public interface MarkRootVolumeAsSnapshotExtension {
    List<Flow> markRootVolumeAsSnapshot(final VolumeInventory vol, VolumeSnapshotVO vo, String accountUuid);
    String getExtensionPrimaryStorageType();
}
