package org.zstack.storage.migration;

import java.util.List;

public interface PrimaryStorageMigrateVmMessage extends StorageMigrationMessage {
    String getVmInstanceUuid();
    String getDstPrimaryStorageUuid();
    String getDstHostUuid();
    boolean isWithDataVolumes();
    boolean isWithSnapshots();
    List<String> getSystemTags();
}
