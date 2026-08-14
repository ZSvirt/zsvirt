package org.zstack.header.storage.primary;

import java.util.List;

public interface PrimaryToBackupStorageMediator {
    String getSupportedPrimaryStorageType();

    String getSupportedBackupStorageType();

    List<String> getSupportedHypervisorTypes();
}
