package org.zstack.header.storage;

import org.zstack.header.storage.backup.BackupStorageInventory;

public interface AddBackupStorageReply {
    BackupStorageInventory backupStorageInventory();
}
