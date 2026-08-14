package org.zstack.storage.volume.block;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.Volume;
import org.zstack.header.volume.block.*;

/**
 * @author shenjin
 * @date 2023/6/20 15:49
 */
public interface BlockPrimaryStorageFactory {
    String getType();

    BlockPrimaryStorageBackend getBlockPrimaryStorageBackend(PrimaryStorageVO vo);
}
