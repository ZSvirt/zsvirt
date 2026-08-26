package org.zstack.crypto.volumebackup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.GLock;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.vm.DiskAO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageManager;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

public class BackupImageKeyManager {
    private static final String IMAGE_RESOURCE_TYPE = "ImageVO";
    private static final long TEMPORARY_IMAGE_KEY_LOCK_TIMEOUT = TimeUnit.SECONDS.toSeconds(30);

    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private EncryptedResourceKeyManager encryptedResourceKeyManager;
    @Autowired
    private ImageStoreBackupStorageManager imageStoreBackupStorageManager;

    public boolean shouldHandleDownload(String backupUuid, String imageUuid) {
        return isBackupEncrypted(backupUuid) || hasTemporaryImageKey(imageUuid);
    }

    public void ensureDiskTemporaryImageKey(DiskAO disk) {
        if (disk == null || !disk.isVolumeBackupTemplate() || StringUtils.isBlank(disk.getTemplateUuid())) {
            return;
        }

        String backupUuid = findBackupUuidByImageUuidForBuildSpec(disk.getTemplateUuid());
        if (StringUtils.isBlank(backupUuid)) {
            return;
        }

        boolean sourceEncrypted = isBackupEncrypted(backupUuid);
        boolean targetEncrypted = Boolean.TRUE.equals(disk.getEncrypted());
        if (!sourceEncrypted && !targetEncrypted) {
            return;
        }

        disk.setEncrypted(true);
        prepareTemporaryImageKeyForTarget(backupUuid, disk.getTemplateUuid(), targetEncrypted);
    }

    public boolean isBackupEncrypted(String backupUuid) {
        if (StringUtils.isBlank(backupUuid)) {
            return false;
        }
        return volumeEncryptedResourceKeyBackend.hasBackupKeyRef(backupUuid);
    }

    public EncryptedResourceKeyManager.ResourceKeyResult prepareTemporaryImageKey(String backupUuid, String imageUuid) {
        if (isBackupEncrypted(backupUuid)) {
            copyBackupKeyRefToTemporaryImageIfNeeded(backupUuid, imageUuid);
            return getTemporaryImageKey(imageUuid);
        }

        if (hasTemporaryImageKey(imageUuid)) {
            return getTemporaryImageKey(imageUuid);
        }
        return null;
    }

    @ExceptionSafe
    public ErrorableValue<EncryptedResourceKeyManager.ResourceKeyResult> prepareTemporaryImageKeyValue(String backupUuid,
                                                                                                       String imageUuid) {
        return ErrorableValue.of(prepareTemporaryImageKey(backupUuid, imageUuid));
    }

    private EncryptedResourceKeyManager.ResourceKeyResult prepareTemporaryImageKeyForTarget(String backupUuid,
                                                                                           String imageUuid,
                                                                                           boolean targetEncrypted) {
        if (isBackupEncrypted(backupUuid)) {
            copyBackupKeyRefToTemporaryImageIfNeeded(backupUuid, imageUuid);
            return getTemporaryImageKey(imageUuid);
        }
        if (!targetEncrypted) {
            return hasTemporaryImageKey(imageUuid) ? getTemporaryImageKey(imageUuid) : null;
        }
        return getOrCreateTemporaryImageKey(imageUuid);
    }

    private boolean hasTemporaryImageKey(String imageUuid) {
        return StringUtils.isNotBlank(imageUuid) &&
                volumeEncryptedResourceKeyBackend.checkTemporarySnapshotImageKeyProviderAttached(imageUuid);
    }

    public EncryptedResourceKeyManager.ResourceKeyResult getTemporaryImageKey(String imageUuid) {
        String keyProviderUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByTemporarySnapshotImage(imageUuid);
        if (StringUtils.isBlank(keyProviderUuid)) {
            throw new OperationFailureException(operr(
                    "temporary image[uuid:%s] requires encrypted ImageStore download but has no key provider binding",
                    imageUuid));
        }

        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(imageUuid);
        ctx.setResourceType(IMAGE_RESOURCE_TYPE);
        ctx.setKeyProviderUuid(keyProviderUuid);
        ctx.setPurpose("prepare-encrypted-imagestore-download");
        EncryptedResourceKeyManager.ResourceKeyResult keyResult = encryptedResourceKeyManager.getExistingKeySync(ctx);
        if (keyResult == null || StringUtils.isBlank(keyResult.getDekBase64())) {
            throw new OperationFailureException(operr(
                    "key manager returned empty DEK for temporary image[uuid:%s]", imageUuid));
        }
        return keyResult;
    }

    private void copyBackupKeyRefToTemporaryImageIfNeeded(String backupUuid, String imageUuid) {
        GLock lock = new GLock(temporaryImageKeyLockName(imageUuid), TEMPORARY_IMAGE_KEY_LOCK_TIMEOUT);
        lock.lock();
        try {
            if (!volumeEncryptedResourceKeyBackend.checkTemporarySnapshotImageKeyProviderAttached(imageUuid)) {
                volumeEncryptedResourceKeyBackend.copyBackupKeyRefToTemporarySnapshotImage(backupUuid, imageUuid);
            }
        } finally {
            lock.unlock();
        }
    }

    private EncryptedResourceKeyManager.ResourceKeyResult getOrCreateTemporaryImageKey(String imageUuid) {
        GLock lock = new GLock(temporaryImageKeyLockName(imageUuid), TEMPORARY_IMAGE_KEY_LOCK_TIMEOUT);
        lock.lock();
        try {
            if (volumeEncryptedResourceKeyBackend.checkTemporarySnapshotImageKeyProviderAttached(imageUuid)) {
                return getTemporaryImageKey(imageUuid);
            }

            String kpUuid = volumeEncryptedResourceKeyBackend.defaultKeyProviderUuid();
            if (StringUtils.isBlank(kpUuid)) {
                throw new OperationFailureException(operr(
                        "encrypted temporary image[uuid:%s] has no default key provider configured", imageUuid));
            }

            EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                    new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
            ctx.setResourceUuid(imageUuid);
            ctx.setResourceType(IMAGE_RESOURCE_TYPE);
            ctx.setKeyProviderUuid(kpUuid);
            ctx.setPurpose("prepare-temporary-volume-backup-image-secret-material");

            EncryptedResourceKeyManager.ResourceKeyResult result = encryptedResourceKeyManager.getOrCreateKeySync(ctx);
            if (result == null || StringUtils.isBlank(result.getDekBase64())) {
                throw new OperationFailureException(operr(
                        "key manager returned empty DEK for encrypted temporary image[uuid:%s]", imageUuid));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    String findBackupUuidByImageUuid(String imageUuid) {
        return imageStoreBackupStorageManager.findVolumeBackupUuidByImageUuidWithMetadataFallback(imageUuid);
    }

    private String findBackupUuidByImageUuidForBuildSpec(String imageUuid) {
        return imageStoreBackupStorageManager.findVolumeBackupUuidByImageUuid(imageUuid);
    }

    private String temporaryImageKeyLockName(String imageUuid) {
        return String.format("vbk-image-key-%s", imageUuid);
    }
}
