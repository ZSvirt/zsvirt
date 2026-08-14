package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageInventory;

public interface BackupStorageSharedBlockKvmFactory {
    String getBackupStorageType();

    BackupStorageSharedBlockKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid);

    BackupStorageSharedBlockKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid);
}
