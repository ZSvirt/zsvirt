package org.zstack.crypto.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.Message;
import org.zstack.header.storage.snapshot.CommitVolumeSnapshotAsImageOnPrimaryStorageMsg;
import org.zstack.storage.encrypt.VolumeSnapshotEncryptionHelper;

import java.io.Serializable;
import java.util.Map;

public class ImageStoreEncryptionCreateImageExtension implements CreateImageExtensionPoint {
    private static final String IMAGE_STORE_ENCRYPTION_ADDON = "ImageStoreEncryption";

    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;

    public static class ImageStoreEncryptionAddon implements Serializable {
        @NoLogging
        private String encryptedDek;

        public ImageStoreEncryptionAddon() {
        }

        public ImageStoreEncryptionAddon(String encryptedDek) {
            this.encryptedDek = encryptedDek;
        }

        public String getEncryptedDek() {
            return encryptedDek;
        }

        public void setEncryptedDek(String encryptedDek) {
            this.encryptedDek = encryptedDek;
        }
    }

    @Override
    public void beforeCreateImage(ImageInventory img, String bsUuid, String psUuid) {
    }

    @Override
    public void beforeSyncImage(ImageInventory img, String bsUuid) {
    }

    @Override
    public String getImageDescription(ImageInventory img) {
        return null;
    }

    @Override
    public void addAddon(Message message, String hostUuid, Map<String, Object> addons) {
        if (!(message instanceof CommitVolumeSnapshotAsImageOnPrimaryStorageMsg)) {
            return;
        }

        CommitVolumeSnapshotAsImageOnPrimaryStorageMsg msg =
                (CommitVolumeSnapshotAsImageOnPrimaryStorageMsg) message;
        if (!Boolean.TRUE.equals(msg.getEncrypted())) {
            return;
        }

        String encryptedDek = snapshotEncryptionHelper.prepareTemporarySnapshotImageEncryptedDek(
                hostUuid, msg.getSnapshotUuid(), msg.getImageUuid(), true);
        addons.put(IMAGE_STORE_ENCRYPTION_ADDON, new ImageStoreEncryptionAddon(encryptedDek));
    }
}
