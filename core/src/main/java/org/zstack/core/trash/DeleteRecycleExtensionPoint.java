package org.zstack.core.trash;

import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.storage.primary.PrimaryStorageType;

public interface DeleteRecycleExtensionPoint {
    String makeSureInstallPathNotUsed(InstallPathRecycleInventory inv);
    String buildAllocatedInstallUrl(InstallPathRecycleInventory inv);

    PrimaryStorageType getPrimaryStorageType();
}
