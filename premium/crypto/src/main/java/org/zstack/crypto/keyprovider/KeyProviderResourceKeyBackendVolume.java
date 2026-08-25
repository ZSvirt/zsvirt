package org.zstack.crypto.keyprovider;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

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
    @Autowired
    private KeyProviderAvailabilityChecker keyProviderAvailabilityChecker;

    @Override
    public void attachKeyProviderToVolume(String volumeUuid, String keyProviderUuid) {
        EncryptedResourceKeyRefVO exist = findKeyRef(VolumeVO.class.getSimpleName(), volumeUuid);
        final String existKeyProviderUuid = exist == null ? null : exist.getProviderUuid();
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

        keyProviderAvailabilityChecker.ensureProviderAvailable(keyProviderUuid);

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
    public void detachKeyProviderFromBackup(String backupUuid) {
        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeBackupVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, backupUuid)
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
    public boolean hasBackupKeyRef(String backupUuid) {
        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeBackupVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, backupUuid)
                .count();
        return count != null && count.longValue() > 0;
    }

    @Override
    public void copyVolumeKeyRefToSnapshot(String volumeUuid, String snapshotUuid) {
        copyKeyRef(VolumeVO.class.getSimpleName(), volumeUuid,
                VolumeSnapshotVO.class.getSimpleName(), snapshotUuid);
    }

    @Override
    public void copyVolumeKeyRefToSnapshots(String volumeUuid, Set<String> snapshotUuids) {
        if (CollectionUtils.isEmpty(snapshotUuids)) {
            return;
        }

        EncryptedResourceKeyRefVO src = findKeyRef(VolumeVO.class.getSimpleName(), volumeUuid);
        if (src == null) {
            throw operr("encrypted key ref[type:%s, uuid:%s] not found",
                    VolumeVO.class.getSimpleName(), volumeUuid).toException();
        }

        Set<String> dstUuids = new LinkedHashSet<>(snapshotUuids);
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(EncryptedResourceKeyRefVO.class)
                        .eq(EncryptedResourceKeyRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                        .in(EncryptedResourceKeyRefVO_.resourceUuid, dstUuids)
                        .delete();

                StringBuilder sql = new StringBuilder("insert into EncryptedResourceKeyRefVO " +
                        "(resourceType, resourceUuid, providerUuid, providerName, keyVersion, kekRef, wrappedDek, algorithm) " +
                        "select :resourceType, dst.resourceUuid, :providerUuid, :providerName, :keyVersion, :kekRef, :wrappedDek, :algorithm from (");
                int i = 0;
                for (String ignored : dstUuids) {
                    if (i > 0) {
                        sql.append(" union all ");
                    }
                    sql.append(String.format("select :resourceUuid%s as resourceUuid", i));
                    i++;
                }
                sql.append(") dst");

                Query query = databaseFacade.getEntityManager().createNativeQuery(sql.toString());
                query.setParameter("resourceType", VolumeSnapshotVO.class.getSimpleName());
                query.setParameter("providerUuid", src.getProviderUuid());
                query.setParameter("providerName", src.getProviderName());
                query.setParameter("keyVersion", src.getKeyVersion());
                query.setParameter("kekRef", src.getKekRef());
                query.setParameter("wrappedDek", src.getWrappedDek());
                query.setParameter("algorithm", src.getAlgorithm());

                int idx = 0;
                for (String snapshotUuid : dstUuids) {
                    query.setParameter(String.format("resourceUuid%s", idx), snapshotUuid);
                    idx++;
                }
                query.executeUpdate();
            }
        }.execute();
    }

    @Override
    public void copySnapshotKeyRefToVolume(String snapshotUuid, String volumeUuid) {
        copyKeyRef(VolumeSnapshotVO.class.getSimpleName(), snapshotUuid,
                VolumeVO.class.getSimpleName(), volumeUuid);
    }

    @Override
    public void copyVolumeKeyRefToVolume(String srcVolumeUuid, String dstVolumeUuid) {
        copyKeyRef(VolumeVO.class.getSimpleName(), srcVolumeUuid,
                VolumeVO.class.getSimpleName(), dstVolumeUuid);
    }

    @Override
    public void copySnapshotKeyRefToTemporarySnapshotImage(String snapshotUuid, String imageUuid) {
        copyKeyRef(VolumeSnapshotVO.class.getSimpleName(), snapshotUuid,
                ImageVO.class.getSimpleName(), imageUuid);
    }

    @Override
    public void copyTemporarySnapshotImageKeyRefToVolume(String imageUuid, String volumeUuid) {
        copyKeyRef(ImageVO.class.getSimpleName(), imageUuid,
                VolumeVO.class.getSimpleName(), volumeUuid);
    }

    @Override
    public void copyVolumeKeyRefToBackup(String volumeUuid, String backupUuid) {
        copyKeyRef(VolumeVO.class.getSimpleName(), volumeUuid,
                VolumeBackupVO.class.getSimpleName(), backupUuid);
    }

    @Override
    public void copyBackupKeyRefToTemporarySnapshotImage(String backupUuid, String imageUuid) {
        copyKeyRef(VolumeBackupVO.class.getSimpleName(), backupUuid,
                ImageVO.class.getSimpleName(), imageUuid);
    }

    @Override
    public void copyBackupKeyRefToVolume(String backupUuid, String volumeUuid) {
        copyKeyRef(VolumeBackupVO.class.getSimpleName(), backupUuid,
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
        EncryptedResourceKeyRefVO ref = findKeyRef(VolumeVO.class.getSimpleName(), volumeUuid);
        return ref == null ? null : ref.getProviderUuid();
    }

    @Override
    public String findKeyProviderUuidBySnapshot(String snapshotUuid) {
        EncryptedResourceKeyRefVO ref = findKeyRef(VolumeSnapshotVO.class.getSimpleName(), snapshotUuid);
        return ref == null ? null : ref.getProviderUuid();
    }

    @Override
    public String findKeyProviderUuidByTemporarySnapshotImage(String imageUuid) {
        EncryptedResourceKeyRefVO ref = findKeyRef(ImageVO.class.getSimpleName(), imageUuid);
        return ref == null ? null : ref.getProviderUuid();
    }

    @Override
    public Integer findKeyVersionByVolume(String volumeUuid) {
        EncryptedResourceKeyRefVO ref = findKeyRef(VolumeVO.class.getSimpleName(), volumeUuid);
        return ref == null ? null : ref.getKeyVersion();
    }

    @Override
    public Integer findKeyVersionBySnapshot(String snapshotUuid) {
        EncryptedResourceKeyRefVO ref = findKeyRef(VolumeSnapshotVO.class.getSimpleName(), snapshotUuid);
        return ref == null ? null : ref.getKeyVersion();
    }

    private void copyKeyRef(String srcType, String srcUuid, String dstType, String dstUuid) {
        EncryptedResourceKeyRefVO src = findKeyRef(srcType, srcUuid);
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

    private EncryptedResourceKeyRefVO findKeyRef(String resourceType, String resourceUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, resourceType)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, resourceUuid)
                .find();
    }
}
