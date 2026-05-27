package org.zstack.storage.migration;

import org.zstack.storage.migration.primary.VolumeMigrationAO;

import java.util.Collections;
import java.util.List;

public interface PrimaryStorageMigrateVmMessage extends StorageMigrationMessage {
    String getVmInstanceUuid();
    String getDstPrimaryStorageUuid();
    String getDstHostUuid();
    boolean isWithDataVolumes();
    boolean isWithSnapshots();
    List<String> getSystemTags();

    default List<VolumeMigrationAO> getVolumeMigrationAOs() {
        return Collections.emptyList();
    }
}
