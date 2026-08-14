package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.VolumeBackupStatus;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSelector;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Qi Le on 2020/7/3
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBackupQuotaUtil {

    @Autowired
    public DatabaseFacade dbf;

    @Transactional(readOnly = true)
    public VolumeBackupQuota getUsed(String accountUuid) {
        VolumeBackupQuota quota = new VolumeBackupQuota();
        long[] usedBackupNumAndSize = getUsedBackupNumAndCapacity(accountUuid);
        quota.backupNum = usedBackupNumAndSize[0];
        quota.backupSize = usedBackupNumAndSize[1];
        return quota;
    }

    @Transactional(readOnly = true)
    public long getUsedNum(String accountUuid) {
        Set<String> remoteBackupStorageUuids = new HashSet<>(ImageStoreBackupStorageSelector.getRemoteBsUuids());
        return getUsedNum(getVolumeBackups(accountUuid), remoteBackupStorageUuids);
    }

    @Transactional(readOnly = true)
    public long getUsedCapacity(String accountUuid) {
        Set<String> remoteBackupStorageUuids = new HashSet<>(ImageStoreBackupStorageSelector.getRemoteBsUuids());
        return getUsedCapacity(getVolumeBackups(accountUuid), remoteBackupStorageUuids);
    }

    public long[] getUsedBackupNumAndCapacity(String accountUuid) {
        List<VolumeBackupVO> backups = getVolumeBackups(accountUuid);
        Set<String> remoteBackupStorageUuids = new HashSet<>(ImageStoreBackupStorageSelector.getRemoteBsUuids());
        long[] tuple = {getUsedNum(backups, remoteBackupStorageUuids), getUsedCapacity(backups, remoteBackupStorageUuids)};
        return tuple;
    }

    @Transactional(readOnly = true)
    public List<VolumeBackupVO> getVolumeBackups(String accountUuid) {
        return ResourceHelper.findOwnResources(VolumeBackupVO.class, accountUuid);
    }

    @Transactional(readOnly = true)
    public class VolumeBackupQuota {
        public long backupNum;
        public long backupSize;
    }

    @Transactional(readOnly = true)
    private long getUsedNum(List<VolumeBackupVO> backups, Set<String> remoteBackupStorageUuids) {
        if (backups.isEmpty()) {
            return 0L;
        }

        if (remoteBackupStorageUuids.isEmpty()) {
            return backups.stream()
                    .filter(backup -> Objects.equals(backup.getStatus(), VolumeBackupStatus.Ready))
                    .count();
        }

        return backups.stream()
                .filter(backup -> Objects.equals(backup.getStatus(), VolumeBackupStatus.Ready))
                .filter(backup -> !localBackupIsDeleted(backup, remoteBackupStorageUuids)).count();
    }

    @Transactional(readOnly = true)
    private long getUsedCapacity(List<VolumeBackupVO> backups, Set<String> remoteBackupStorageUuids) {
        if (backups.isEmpty()) {
            return 0L;
        }

        if (remoteBackupStorageUuids.isEmpty()) {
            return backups.stream()
                    .filter(backup -> Objects.equals(backup.getStatus(), VolumeBackupStatus.Ready))
                    .map(VolumeBackupVO::getSize).reduce(0L, Long::sum);
        }

        return backups.stream()
                .filter(backup -> Objects.equals(backup.getStatus(), VolumeBackupStatus.Ready))
                .filter(backup -> !localBackupIsDeleted(backup, remoteBackupStorageUuids))
                .map(VolumeBackupVO::getSize).reduce(0L, Long::sum);
    }

    private boolean localBackupIsDeleted(VolumeBackupVO backup, Set<String> remoteBackupStorageUuids) {
        return backup.getBackupStorageRefs().stream()
                .filter(ref -> !remoteBackupStorageUuids.contains(ref.getBackupStorageUuid()))
                .allMatch(ref -> ref.getStatus().equals(VolumeBackupStatus.Deleted));
    }
}
