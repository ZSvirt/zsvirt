package org.zstack.storage.volume.block;

import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.block.GetAccessPathMsg;

public interface BlockPrimaryStorageBackend {
    String getType();

    void handle(InstantiateVolumeOnPrimaryStorageMsg msg);

    void handle(DeleteVolumeOnPrimaryStorageMsg msg);

    void handle(ResizeVolumeOnPrimaryStorageMsg msg);

    void handle(TakeSnapshotMsg msg);

    void handle(final DeleteSnapshotOnPrimaryStorageMsg msg);

    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg);

    void handle(GetAccessPathMsg msg);
}
