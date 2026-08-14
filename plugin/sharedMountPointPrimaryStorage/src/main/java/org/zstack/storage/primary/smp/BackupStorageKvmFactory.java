package org.zstack.storage.primary.smp;

import org.zstack.header.storage.primary.PrimaryStorageInventory;

/**
 * Created by xing5 on 2016/3/27.
 */
public interface BackupStorageKvmFactory {
    String getBackupStorageType();

    BackupStorageKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid);

    BackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid);
}
