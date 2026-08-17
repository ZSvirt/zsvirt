package org.zstack.crypto.keyprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Objects;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.keyprovider.KeyProviderErrors.VOLUME_ALREADY_ATTACHED_KEY_PROVIDER;

/**
 * Volume key-provider binding on {@link EncryptedResourceKeyRefVO} (LUKS / volume crypto).
 */
public class KeyProviderResourceKeyBackendVolume implements VolumeEncryptedResourceKeyBackend {
    private static final CLogger logger = Utils.getLogger(KeyProviderResourceKeyBackendVolume.class);

    @Autowired
    private DatabaseFacade databaseFacade;

    @Override
    public void attachKeyProviderToVolume(String volumeUuid, String keyProviderUuid) {
        final String existKeyProviderUuid = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, volumeUuid)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .findValue();
        if (Objects.equals(existKeyProviderUuid, keyProviderUuid)) {
            logger.debug(String.format(
                    "The key provider[uuid:%s] is already attached to volume[uuid:%s], skip attach",
                    keyProviderUuid, volumeUuid));
            return;
        } else if (existKeyProviderUuid != null) {
            throw err(VOLUME_ALREADY_ATTACHED_KEY_PROVIDER,
                    "The key provider[uuid:%s] is already attached to volume[uuid:%s]",
                    existKeyProviderUuid, volumeUuid)
                    .withOpaque("exists.key.provider.uuid", existKeyProviderUuid)
                    .withOpaque("volume.uuid", volumeUuid)
                    .toException();
        }

        final KeyProviderVO provider = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.uuid, keyProviderUuid)
                .find();
        if (provider == null) {
            throw operr("KeyProvider[uuid:%s] not found", keyProviderUuid).toException();
        }

        EncryptedResourceKeyRefVO ref = new EncryptedResourceKeyRefVO();
        ref.setResourceType(VolumeVO.class.getSimpleName());
        ref.setResourceUuid(volumeUuid);
        ref.setProviderUuid(keyProviderUuid);
        ref.setProviderName(provider.getName());
        ref.setWrappedDek("");
        databaseFacade.persist(ref);
    }

    @Override
    public void detachKeyProviderFromVolume(String volumeUuid) {
        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, volumeUuid)
                .delete();
    }

    @Override
    public void detachKeyProviderFromSnapshot(String snapshotUuid) {
        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, snapshotUuid)
                .delete();
    }

    @Override
    public void detachKeyProviderFromTemporarySnapshotImage(String imageUuid) {
        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, ImageVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, imageUuid)
                .delete();
    }

    @Override
    public boolean checkVolumeKeyProviderAttached(String volumeUuid) {
        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, volumeUuid)
                .count();
        return count != null && count.longValue() > 0;
    }

    @Override
    public boolean checkSnapshotKeyProviderAttached(String snapshotUuid) {
        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, snapshotUuid)
                .count();
        return count != null && count.longValue() > 0;
    }

    @Override
    public boolean checkTemporarySnapshotImageKeyProviderAttached(String imageUuid) {
        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, ImageVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, imageUuid)
                .count();
        return count != null && count.longValue() > 0;
    }

    @Override
    public void copyVolumeKeyToSnapshot(String volumeUuid, String snapshotUuid) {
        copyKeyRef(VolumeVO.class.getSimpleName(), volumeUuid,
                VolumeSnapshotVO.class.getSimpleName(), snapshotUuid);
    }

    @Override
    public void copySnapshotKeyToVolume(String snapshotUuid, String volumeUuid) {
        copyKeyRef(VolumeSnapshotVO.class.getSimpleName(), snapshotUuid,
                VolumeVO.class.getSimpleName(), volumeUuid);
    }

    @Override
    public void copyVolumeKeyToVolume(String srcVolumeUuid, String dstVolumeUuid) {
        copyKeyRef(VolumeVO.class.getSimpleName(), srcVolumeUuid,
                VolumeVO.class.getSimpleName(), dstVolumeUuid);
    }

    @Override
    public void copySnapshotKeyToTemporarySnapshotImage(String snapshotUuid, String imageUuid) {
        copyKeyRef(VolumeSnapshotVO.class.getSimpleName(), snapshotUuid,
                ImageVO.class.getSimpleName(), imageUuid);
    }

    @Override
    public void copyTemporarySnapshotImageKeyToVolume(String imageUuid, String volumeUuid) {
        copyKeyRef(ImageVO.class.getSimpleName(), imageUuid,
                VolumeVO.class.getSimpleName(), volumeUuid);
    }

    @Override
    public String defaultKeyProviderUuid() {
        String currentDefault = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value();
        if (KeyProviderGlobalConfig.NONE.equals(currentDefault)) {
            return null;
        }
        return currentDefault;
    }

    @Override
    public String findKeyProviderUuidByVolume(String volumeUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, volumeUuid)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .findValue();
    }

    @Override
    public String findKeyProviderUuidBySnapshot(String snapshotUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, snapshotUuid)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .findValue();
    }

    @Override
    public String findKeyProviderUuidByTemporarySnapshotImage(String imageUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, ImageVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, imageUuid)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .findValue();
    }

    @Override
    public Integer findKeyVersionByVolume(String volumeUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, volumeUuid)
                .select(EncryptedResourceKeyRefVO_.keyVersion)
                .findValue();
    }

    @Override
    public Integer findKeyVersionBySnapshot(String snapshotUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, snapshotUuid)
                .select(EncryptedResourceKeyRefVO_.keyVersion)
                .findValue();
    }

    private void copyKeyRef(String srcType, String srcUuid, String dstType, String dstUuid) {
        EncryptedResourceKeyRefVO src = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, srcType)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, srcUuid)
                .find();
        if (src == null) {
            throw operr("encrypted key ref[type:%s, uuid:%s] not found", srcType, srcUuid).toException();
        }

        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, dstType)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, dstUuid)
                .delete();

        EncryptedResourceKeyRefVO dst = new EncryptedResourceKeyRefVO();
        dst.setResourceType(dstType);
        dst.setResourceUuid(dstUuid);
        dst.setProviderUuid(src.getProviderUuid());
        dst.setProviderName(src.getProviderName());
        dst.setKeyVersion(src.getKeyVersion());
        dst.setKekRef(src.getKekRef());
        dst.setWrappedDek(src.getWrappedDek());
        dst.setAlgorithm(src.getAlgorithm());
        databaseFacade.persist(dst);
    }
}
