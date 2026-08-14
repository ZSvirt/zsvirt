package org.zstack.storage.primary.imagestore.ceph;

import org.zstack.storage.volume.block.BlockPrimaryStorageFactory;

public interface CephBlockPrimaryStorageManager {
    BlockPrimaryStorageFactory getBlockPrimaryStorageFactory(String type);
}
