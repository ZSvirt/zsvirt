package org.zstack.storage.migration;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.storage.migration.primary.PrimaryStorageMigrateVmMsg;

public interface StorageMigrationExtensionPoint {
    void afterStorageMigration(PrimaryStorageMigrateVmMsg msg, VmInstanceVO vm);
}
