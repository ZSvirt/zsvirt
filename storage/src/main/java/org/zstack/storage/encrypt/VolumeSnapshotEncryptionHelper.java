package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeVO;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotEncryptionHelper {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VolumeEncryptedResourceKeyBackend keyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper secretHelper;
    @Autowired
    private EncryptedResourceKeyManager encryptedResourceKeyManager;

    public void completeTakeSnapshot(VolumeVO volume, VolumeSnapshotInventory snapshot) {
        if (volume == null || snapshot == null || !volume.isEncrypted()) {
            return;
        }

        snapshot.setEncrypted(true);
        Boolean encrypted = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshot.getUuid())
                .select(VolumeSnapshotVO_.encrypted)
                .findValue();
        if (!Boolean.TRUE.equals(encrypted)) {
            VolumeSnapshotVO vo = dbf.findByUuid(snapshot.getUuid(), VolumeSnapshotVO.class);
            if (vo != null) {
                vo.setEncrypted(true);
                dbf.update(vo);
            }
        }
    }

    public void inheritVolumeKeyToSnapshot(VolumeVO volume, VolumeSnapshotInventory snapshot) {
        if (volume == null || snapshot == null || !volume.isEncrypted()) {
            return;
        }
        keyBackend.copyVolumeKeyRefToSnapshot(volume.getUuid(), snapshot.getUuid());
    }

    public void inheritFromRelatedSnapshotKeyIfPossible(VolumeVO volume, String snapshotUuid) {
        if (volume == null || StringUtils.isBlank(snapshotUuid)
                || keyBackend.checkVolumeKeyProviderAttached(volume.getUuid())) {
            return;
        }

        Boolean snapshotEncrypted = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .select(VolumeSnapshotVO_.encrypted)
                .findValue();
        if (!Boolean.TRUE.equals(snapshotEncrypted)) {
            if (volume.isEncrypted()) {
                String kpUuid = keyBackend.defaultKeyProviderUuid();
                if (StringUtils.isBlank(kpUuid)) {
                    throw new OperationFailureException(operr(
                            "encrypted volume[uuid:%s] has no key provider binding and no default key provider configured",
                            volume.getUuid()));
                }
                keyBackend.attachKeyProviderToVolume(volume.getUuid(), kpUuid);
                materializeVolumeKey(volume.getUuid(), kpUuid, "create-encrypted-volume-from-plain-snapshot");
            }
            return;
        }
        if (!keyBackend.checkSnapshotKeyProviderAttached(snapshotUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted snapshot[uuid:%s] has no key provider binding; cannot inherit key for volume[uuid:%s]",
                    snapshotUuid, volume.getUuid()));
        }

        keyBackend.copySnapshotKeyRefToVolume(snapshotUuid, volume.getUuid());
        if (!volume.isEncrypted()) {
            volume.setEncrypted(true);
            dbf.update(volume);
        }
    }

    public void inheritFromTemporarySnapshotImageKeyIfPossible(VolumeVO volume) {
        if (volume == null || StringUtils.isBlank(volume.getRootImageUuid())) {
            return;
        }

        String imageUrl = Q.New(ImageVO.class)
                .eq(ImageVO_.uuid, volume.getRootImageUuid())
                .select(ImageVO_.url)
                .findValue();
        if (StringUtils.isBlank(imageUrl)) {
            return;
        }

        if (keyBackend.checkTemporarySnapshotImageKeyProviderAttached(volume.getRootImageUuid())) {
            if (!keyBackend.checkVolumeKeyProviderAttached(volume.getUuid())) {
                keyBackend.copyTemporarySnapshotImageKeyRefToVolume(volume.getRootImageUuid(), volume.getUuid());
            }
            return;
        }

        if (imageUrl.startsWith("volume://")) {
            String srcVolumeUuid = imageUrl.substring("volume://".length());
            if (!keyBackend.checkVolumeKeyProviderAttached(volume.getUuid())) {
                if (keyBackend.checkVolumeKeyProviderAttached(srcVolumeUuid)) {
                    keyBackend.copyVolumeKeyRefToVolume(srcVolumeUuid, volume.getUuid());
                }
            }
            return;
        }

        if (!imageUrl.startsWith(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA)
                && !imageUrl.startsWith(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            return;
        }

        String snapshotUuid = getSnapshotUuidFromImageUrl(imageUrl);
        inheritFromRelatedSnapshotKeyIfPossible(volume, snapshotUuid);
    }

    public boolean hasTemporarySnapshotImageKey(String imageUuid) {
        return StringUtils.isNotBlank(imageUuid) && keyBackend.checkTemporarySnapshotImageKeyProviderAttached(imageUuid);
    }

    private String getSnapshotUuidFromImageUrl(String imageUrl) {
        String snapshotUuid;
        if (imageUrl.startsWith(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA.length());
        } else if (imageUrl.startsWith(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA.length());
        } else {
            return null;
        }
        return snapshotUuid.length() >= 32 ? snapshotUuid.substring(0, 32) : snapshotUuid;
    }

    private EncryptedResourceKeyManager.ResourceKeyResult materializeVolumeKey(String volumeUuid,
                                                                               String keyProviderUuid,
                                                                               String purpose) {
        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(volumeUuid);
        ctx.setResourceType(VolumeVO.class.getSimpleName());
        ctx.setKeyProviderUuid(keyProviderUuid);
        ctx.setPurpose(purpose);

        final EncryptedResourceKeyManager.ResourceKeyResult[] resultRef =
                new EncryptedResourceKeyManager.ResourceKeyResult[1];
        final org.zstack.header.errorcode.ErrorCode[] errorRef =
                new org.zstack.header.errorcode.ErrorCode[1];
        encryptedResourceKeyManager.getOrCreateKey(ctx,
                new org.zstack.header.core.ReturnValueCompletion<EncryptedResourceKeyManager.ResourceKeyResult>(null) {
                    @Override
                    public void success(EncryptedResourceKeyManager.ResourceKeyResult returnValue) {
                        resultRef[0] = returnValue;
                    }

                    @Override
                    public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                        errorRef[0] = errorCode;
                    }
                });

        if (errorRef[0] != null) {
            if (VolumeEncryptedSecretHelper.isKeyProviderUnavailable(errorRef[0])) {
                throw new OperationFailureException(errorRef[0]);
            }

            throw new OperationFailureException(operr(
                    "failed to materialize encryption key for volume[uuid:%s]", volumeUuid).withCause(errorRef[0]));
        }
        if (resultRef[0] == null || StringUtils.isBlank(resultRef[0].getDekBase64())) {
            throw new OperationFailureException(operr(
                    "key manager returned empty DEK for encrypted volume[uuid:%s]", volumeUuid));
        }
        return resultRef[0];
    }

    public String prepareTemporarySnapshotImageEncryptedDek(String hostUuid,
                                                            String snapshotUuid,
                                                            String imageUuid,
                                                            Boolean encrypted) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(snapshotUuid) || StringUtils.isBlank(imageUuid)
                || !Boolean.TRUE.equals(encrypted)) {
            return null;
        }

        EncryptedResourceKeyManager.ResourceKeyResult keyResult;
        Boolean snapshotEncrypted = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .select(VolumeSnapshotVO_.encrypted)
                .findValue();
        if (Boolean.TRUE.equals(snapshotEncrypted)) {
            keyBackend.copySnapshotKeyRefToTemporarySnapshotImage(snapshotUuid, imageUuid);
            keyResult = getTemporarySnapshotImageKey(imageUuid);
        } else {
            keyResult = createTemporarySnapshotImageKey(imageUuid);
        }

        return secretHelper.verifyHostKeyAndHpkeSealDek(
                hostUuid, imageUuid, keyResult.getDekBase64());
    }

    private EncryptedResourceKeyManager.ResourceKeyResult getTemporarySnapshotImageKey(String imageUuid) {
        String kpUuid = keyBackend.findKeyProviderUuidByTemporarySnapshotImage(imageUuid);
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted temporary snapshot image[uuid:%s] has no key provider binding", imageUuid));
        }

        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(imageUuid);
        ctx.setResourceType(ImageVO.class.getSimpleName());
        ctx.setKeyProviderUuid(kpUuid);
        ctx.setPurpose("prepare-temporary-snapshot-image-secret-material");

        EncryptedResourceKeyManager.ResourceKeyResult keyResult = encryptedResourceKeyManager.getExistingKeySync(ctx);
        if (keyResult == null || StringUtils.isBlank(keyResult.getDekBase64())) {
            throw new OperationFailureException(operr(
                    "key manager returned empty DEK for encrypted temporary snapshot image[uuid:%s]", imageUuid));
        }
        return keyResult;
    }

    private EncryptedResourceKeyManager.ResourceKeyResult createTemporarySnapshotImageKey(String imageUuid) {
        String kpUuid = keyBackend.defaultKeyProviderUuid();
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted temporary snapshot image[uuid:%s] has no default key provider configured", imageUuid));
        }

        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(imageUuid);
        ctx.setResourceType(ImageVO.class.getSimpleName());
        ctx.setKeyProviderUuid(kpUuid);
        ctx.setPurpose("prepare-temporary-snapshot-image-secret-material");

        final EncryptedResourceKeyManager.ResourceKeyResult[] resultRef =
                new EncryptedResourceKeyManager.ResourceKeyResult[1];
        final org.zstack.header.errorcode.ErrorCode[] errorRef =
                new org.zstack.header.errorcode.ErrorCode[1];
        encryptedResourceKeyManager.getOrCreateKey(ctx,
                new org.zstack.header.core.ReturnValueCompletion<EncryptedResourceKeyManager.ResourceKeyResult>(null) {
                    @Override
                    public void success(EncryptedResourceKeyManager.ResourceKeyResult returnValue) {
                        resultRef[0] = returnValue;
                    }

                    @Override
                    public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                        errorRef[0] = errorCode;
                    }
                });

        if (errorRef[0] != null) {
            if (VolumeEncryptedSecretHelper.isKeyProviderUnavailable(errorRef[0])) {
                throw new OperationFailureException(errorRef[0]);
            }

            throw new OperationFailureException(operr(
                    "failed to materialize encryption key for temporary snapshot image[uuid:%s]",
                    imageUuid).withCause(errorRef[0]));
        }
        if (resultRef[0] == null || StringUtils.isBlank(resultRef[0].getDekBase64())) {
            throw new OperationFailureException(operr(
                    "key manager returned empty DEK for encrypted temporary snapshot image[uuid:%s]", imageUuid));
        }
        return resultRef[0];
    }

}
