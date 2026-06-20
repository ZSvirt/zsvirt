package org.zstack.crypto.volumebackup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ExpungeImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageManager;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;

public class BackupImageKeyCleanupExtension implements ExpungeImageExtensionPoint {
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private ImageStoreBackupStorageManager imageStoreBackupStorageManager;

    @Override
    public void preExpungeImage(ImageInventory img) {
    }

    @Override
    public void beforeExpungeImage(ImageInventory img) {
    }

    @Override
    public void afterExpungeImage(ImageInventory img, String imageBackupStorageUuid) {
        if (img == null || img.getUuid() == null) {
            return;
        }

        long refCount = imageStoreBackupStorageManager.countImageBackupStorageRefs(img.getUuid());
        if (refCount == 0) {
            volumeEncryptedResourceKeyBackend.detachKeyProviderFromTemporarySnapshotImage(img.getUuid());
        }
    }

    @Override
    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {
    }
}
