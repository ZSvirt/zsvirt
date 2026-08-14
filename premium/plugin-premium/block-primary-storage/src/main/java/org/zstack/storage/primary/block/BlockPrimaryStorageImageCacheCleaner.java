package org.zstack.storage.primary.block;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/7/17 21:07
 */
public class BlockPrimaryStorageImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BlockPrimaryStorageImageCacheCleaner.class);

    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE;
    }
}
