package org.zstack.storage.primary.block;

import org.zstack.header.storage.primary.PrimaryStorageInventory;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/9 12:12
 */
public interface BackupStorageBlockKvmFactory {
    String getBackupStorageType();

    BackupStorageBlockKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid);

    BackupStorageBlockKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid);
}
