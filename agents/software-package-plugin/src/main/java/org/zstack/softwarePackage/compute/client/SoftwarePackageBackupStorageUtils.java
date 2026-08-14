package org.zstack.softwarePackage.compute.client;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.softwarePackage.compute.SoftwarePackageSystemTags;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.storage.backup.imagestore.ImageStoreSystemTags;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO_;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.GENERAL_ERROR;

public final class SoftwarePackageBackupStorageUtils {
    private SoftwarePackageBackupStorageUtils() {
    }

    private static List<String> getOnlyForBackupImageStoreBsUuids() {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, ImageStoreBackupStorageVO.class.getSimpleName())
                .eq(SystemTagVO_.tag, ImageStoreSystemTags.ONLY_FOR_BACKUP_TOKEN)
                .select(SystemTagVO_.resourceUuid)
                .listValues();
    }

    public static boolean isOnlyForBackupTagged(String bsUuid) {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, ImageStoreBackupStorageVO.class.getSimpleName())
                .eq(SystemTagVO_.resourceUuid, bsUuid)
                .eq(SystemTagVO_.tag, ImageStoreSystemTags.ONLY_FOR_BACKUP_TOKEN)
                .isExists();
    }

    private static String getOriginalBackupStorageUuid(String softwarePackageUuid) {
        return SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID
                .getTokenByResourceUuid(softwarePackageUuid,
                        SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
    }

    public static String requireOriginalBackupStorageUuid(String softwarePackageUuid) {
        String uuid = getOriginalBackupStorageUuid(softwarePackageUuid);
        if (StringUtils.isEmpty(uuid)) {
            throw err(GENERAL_ERROR,
                    "cannot find the backup storage hosting software package [uuid:%s]; the system tag is missing",
                    softwarePackageUuid).toException();
        }
        return uuid;
    }

    public static List<String> getZoneUuidsOfBackupStorage(String bsUuid) {
        return Q.New(BackupStorageZoneRefVO.class)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, bsUuid)
                .select(BackupStorageZoneRefVO_.zoneUuid)
                .listValues();
    }

    private static List<String> getBackupStorageUuidsInZones(Collection<String> zoneUuids) {
        if (zoneUuids == null || zoneUuids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> uuids = Q.New(BackupStorageZoneRefVO.class)
                .in(BackupStorageZoneRefVO_.zoneUuid, zoneUuids)
                .select(BackupStorageZoneRefVO_.backupStorageUuid)
                .listValues();
        return uuids.stream().distinct().collect(Collectors.toList());
    }

    private static List<Tuple> listConnectedImageStoreBs() {
        return listConnectedImageStoreBs(null);
    }

    private static List<Tuple> listConnectedImageStoreBs(Collection<String> restrictUuids) {
        if (restrictUuids != null && restrictUuids.isEmpty()) {
            return Collections.emptyList();
        }
        Q q = Q.New(ImageStoreBackupStorageVO.class)
                .eq(ImageStoreBackupStorageVO_.state, BackupStorageState.Enabled)
                .eq(ImageStoreBackupStorageVO_.status, BackupStorageStatus.Connected);
        if (restrictUuids != null) {
            q.in(ImageStoreBackupStorageVO_.uuid, restrictUuids);
        }
        return q.orderByDesc(ImageStoreBackupStorageVO_.availableCapacity)
                .select(ImageStoreBackupStorageVO_.uuid, ImageStoreBackupStorageVO_.availableCapacity)
                .listTuple();
    }

    private static List<Tuple> listConnectedCephBs() {
        return listConnectedCephBs(null);
    }

    private static List<Tuple> listConnectedCephBs(Collection<String> restrictUuids) {
        if (restrictUuids != null && restrictUuids.isEmpty()) {
            return Collections.emptyList();
        }
        Q q = Q.New(CephBackupStorageVO.class)
                .eq(CephBackupStorageVO_.state, BackupStorageState.Enabled)
                .eq(CephBackupStorageVO_.status, BackupStorageStatus.Connected);
        if (restrictUuids != null) {
            q.in(CephBackupStorageVO_.uuid, restrictUuids);
        }
        return q.orderByDesc(CephBackupStorageVO_.availableCapacity)
                .select(CephBackupStorageVO_.uuid, CephBackupStorageVO_.availableCapacity)
                .listTuple();
    }

    private static Tuple pickFirstFitting(List<Tuple> candidates, long estimatedImageSize, Collection<String> excludeUuids) {
        Set<String> excludeSet = (excludeUuids == null || excludeUuids.isEmpty())
                ? Collections.emptySet()
                : new HashSet<>(excludeUuids);
        for (Tuple t : candidates) {
            String uuid = t.get(0, String.class);
            if (excludeSet.contains(uuid)) {
                continue;
            }
            Long capacity = t.get(1, Long.class);
            if (capacity != null && (estimatedImageSize <= 0 || capacity >= estimatedImageSize)) {
                return t;
            }
        }
        return null;
    }

    public static Tuple getBackupStorageUuidAndAvailableCapacity(long estimatedImageSize) {
        List<String> onlyForBackupBsUuids = getOnlyForBackupImageStoreBsUuids();

        Tuple t = pickFirstFitting(listConnectedImageStoreBs(), estimatedImageSize, onlyForBackupBsUuids);
        if (t != null) {
            return t;
        }
        return pickFirstFitting(listConnectedCephBs(), estimatedImageSize, null);
    }

    public static Tuple getUpgradeBackupStorageUuidAndAvailableCapacity(String softwarePackageUuid, long estimatedImageSize) {
        String originalBsUuid = requireOriginalBackupStorageUuid(softwarePackageUuid);

        List<String> originalZoneUuids = getZoneUuidsOfBackupStorage(originalBsUuid);
        if (originalZoneUuids.isEmpty()) {
            throw err(GENERAL_ERROR,
                    "the backup storage [uuid:%s] hosting software package [uuid:%s] is not attached to any zone",
                    originalBsUuid, softwarePackageUuid)
                    .toException();
        }

        List<String> candidateBsUuids = getBackupStorageUuidsInZones(originalZoneUuids);
        if (candidateBsUuids.isEmpty()) {
            return null;
        }

        List<String> onlyForBackupBsUuids = getOnlyForBackupImageStoreBsUuids();
        Tuple t = pickFirstFitting(listConnectedImageStoreBs(candidateBsUuids), estimatedImageSize, onlyForBackupBsUuids);
        if (t != null) {
            return t;
        }
        return pickFirstFitting(listConnectedCephBs(candidateBsUuids), estimatedImageSize, null);
    }
}
