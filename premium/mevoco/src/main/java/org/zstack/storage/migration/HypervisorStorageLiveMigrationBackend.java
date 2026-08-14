package org.zstack.storage.migration;

import org.zstack.storage.migration.primary.PrimaryStorageCancelLiveMigrateVmMsg;
import org.zstack.storage.migration.primary.PrimaryStorageLiveMigrateVmMsg;

public interface HypervisorStorageLiveMigrationBackend {
    String getHypervisorType();

    void handle(PrimaryStorageLiveMigrateVmMsg msg);

    void handle(PrimaryStorageCancelLiveMigrateVmMsg msg);
}
