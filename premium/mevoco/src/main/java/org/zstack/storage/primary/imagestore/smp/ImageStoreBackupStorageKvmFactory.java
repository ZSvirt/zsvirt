package org.zstack.storage.primary.imagestore.smp;

import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.primary.smp.BackupStorageKvmDownloader;
import org.zstack.storage.primary.smp.BackupStorageKvmFactory;
import org.zstack.storage.primary.smp.BackupStorageKvmUploader;

/**
 * Created by david on 7/22/16.
 */
public class ImageStoreBackupStorageKvmFactory implements BackupStorageKvmFactory {
    @Override
    public String getBackupStorageType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }

    @Override
    public BackupStorageKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return ImageStoreBackupStorageKvmUploader.createUploader(ps, bsUuid);
    }

    @Override
    public BackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return ImageStoreBackupStorageKvmDownloader.createDownloader(ps, bsUuid);
    }
}
