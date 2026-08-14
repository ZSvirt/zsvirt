package org.zstack.header.storage.backup;

import org.zstack.header.core.Completion;

public interface AfterUnpackBackupStorageExtensionPoint {
    void afterUnpackBackupStorage(BackupStorageInventory inventory, Completion completion);
}
