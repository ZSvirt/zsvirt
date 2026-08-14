package org.zstack.storage.primary.imagestore.ceph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.CommitImageBackupStorageSelector;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.BackupStorageGlobalConfig;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.function.Function;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by david on 8/9/16.
 */
public class CephCommitImageBackupStorageSelector implements CommitImageBackupStorageSelector {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    @Transactional(readOnly = true)
    private List<BackupStorageInventory> findCephBackupStorageInZone(String zoneUuid) {
        String sql = "select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref where bs.type = :type and bs.status = :status and bs.state = :state and ref.zoneUuid = :zoneUuid and ref.backupStorageUuid = bs.uuid";
        TypedQuery<BackupStorageVO> q = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
        q.setParameter("type", CephConstants.CEPH_BACKUP_STORAGE_TYPE);
        q.setParameter("status", BackupStorageStatus.Connected);
        q.setParameter("state", BackupStorageState.Enabled);
        q.setParameter("zoneUuid", zoneUuid);
        return transform(q.getResultList(), BackupStorageInventory::valueOf);
    }

    @Override
    public BackupStorageInventory selectWithVolume(VolumeInventory vol, long requiredSize) {
        return doSelect(vol, requiredSize);
    }
    private BackupStorageInventory doSelect(VolumeInventory vol, long size) {
        final long totalSize = SizeUtils.sizeStringToBytes(BackupStorageGlobalConfig.RESERVED_CAPACITY.value()) + size;

        Tuple t = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type, PrimaryStorageVO_.zoneUuid)
                .eq(PrimaryStorageVO_.uuid, vol.getPrimaryStorageUuid())
                .findTuple();

        if (t == null || !CephConstants.CEPH_PRIMARY_STORAGE_TYPE.equals(t.get(0, String.class))) {
            return null;
        }

        List<BackupStorageInventory> bsList = findCephBackupStorageInZone(t.get(1, String.class));
        if (bsList.isEmpty()) {
            return null;
        }

        PrimaryStorageType psType = PrimaryStorageType.valueOf(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        List<String> bsUuidListForVolumePrimaryStorage = psType.findBackupStorage(vol.getPrimaryStorageUuid());

        List<String> backupStorageUuids = bsList.stream().map(BackupStorageInventory::getUuid).collect(Collectors.toList());
        Map<String, String> resourceConfigValueMap = rcf.getResourceConfigValues(
                BackupStorageGlobalConfig.RESERVED_CAPACITY, backupStorageUuids, String.class);

        bsList.removeIf(bs -> !bsUuidListForVolumePrimaryStorage.contains(bs.getUuid()) ||
                bs.getAvailableCapacity() < SizeUtils.sizeStringToBytes(resourceConfigValueMap.get(bs.getUuid())) + size);
        if (bsList.isEmpty()) {
            return null;
        }

        return bsList.get(new Random().nextInt(bsList.size()));
    }
}
