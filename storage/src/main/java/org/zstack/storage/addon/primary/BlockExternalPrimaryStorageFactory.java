package org.zstack.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO;

public interface BlockExternalPrimaryStorageFactory {
    String getType();

    BlockExternalPrimaryStorageBackend getBlockExternalPrimaryStorageBackend(PrimaryStorageVO vo);
}
