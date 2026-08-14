package org.zstack.header.storage.backup;

import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public interface BackupStoragePrimaryStorageExtensionPoint {
    String getPrimaryStoragePriorityMap(final BackupStorageInventory bs, final ImageInventory image);
    List<String> getBackupStorageSupportedPS(final String psUuid);

    void cleanupPrimaryCacheForBS(final PrimaryStorageInventory ps, String hostUuid, final Completion completion);
}
