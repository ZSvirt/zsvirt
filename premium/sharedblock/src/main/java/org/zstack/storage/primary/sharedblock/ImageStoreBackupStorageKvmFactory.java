package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;

/**
 * Created by david on 7/22/16.
 */
public class ImageStoreBackupStorageKvmFactory implements BackupStorageSharedBlockKvmFactory {
    @Override
    public String getBackupStorageType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }

    @Override
    public BackupStorageSharedBlockKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return ImageStoreBackupStorageKvmUploader.createUploader(ps, bsUuid);
    }

    @Override
    public BackupStorageSharedBlockKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return ImageStoreBackupStorageKvmDownloader.createDownloader(ps, bsUuid);
    }
}
