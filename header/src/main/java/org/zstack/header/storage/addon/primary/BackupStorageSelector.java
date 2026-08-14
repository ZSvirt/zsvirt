package org.zstack.header.storage.addon.primary;

import java.util.List;

public interface BackupStorageSelector {
    List<String> getPreferBackupStorageTypes();

    String getIdentity();
}
