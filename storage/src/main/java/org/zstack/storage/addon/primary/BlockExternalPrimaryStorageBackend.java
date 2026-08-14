package org.zstack.storage.addon.primary;

import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.block.GetAccessPathMsg;

public interface BlockExternalPrimaryStorageBackend {
    String getType();

    void handle(InstantiateVolumeOnPrimaryStorageMsg msg);

    void handle(DeleteVolumeOnPrimaryStorageMsg msg);

    void handle(TakeSnapshotMsg msg);

    void handle(final DeleteSnapshotOnPrimaryStorageMsg msg);

    void handle(GetAccessPathMsg msg);
}
