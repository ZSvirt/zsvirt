package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.CommitImageBackupStorageSelector;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.BackupStorageGlobalConfig;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by david on 8/1/16.
 */
public class ImageStoreBackupStorageSelector implements CommitImageBackupStorageSelector {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageSelector.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    private String findPrimaryStorageZoneUuid(final String primaryStorageUuid) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.uuid, SimpleQuery.Op.EQ, primaryStorageUuid);
        q.select(PrimaryStorageVO_.zoneUuid);
        return q.findValue();
    }

    @Transactional(readOnly = true)
    private List<BackupStorageInventory> findImageStoreBackupStorageInZone(String zoneUuid) {
        String sql = "select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref where bs.type = :type " +
                "and bs.status = :status and bs.state = :state and ref.zoneUuid = :zoneUuid and ref.backupStorageUuid = bs.uuid";
        List<BackupStorageVO> qs = SQL.New(sql).
                param("type", ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE).
                param("status", BackupStorageStatus.Connected).
                param("state", BackupStorageState.Enabled).
                param("zoneUuid", zoneUuid).list();
        // filter the image-store which is backup remote
        return BackupStorageInventory.valueOf(qs.stream()
                .filter(q -> !ImageStoreBackupStorageSelector.isRemote(q.getUuid()))
                .filter(q -> !ImageStoreBackupStorageSelector.backupOnlyBackupStorage(q.getUuid()))
                .collect(Collectors.toList()));
    }

    private List<String> findImageLocatedBs(final String imageUuid) {
        return Q.New(ImageBackupStorageRefVO.class).select(ImageBackupStorageRefVO_.backupStorageUuid)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid).listValues();
    }

    @Override
    public BackupStorageInventory selectWithVolume(VolumeInventory vol, long requiredSize) {
        return doSelect(vol, requiredSize);
    }

    private BackupStorageInventory doSelect(VolumeInventory vol, long size) {
        final String imgUuid = vol.getRootImageUuid();
        final String zoneUuid = findPrimaryStorageZoneUuid(vol.getPrimaryStorageUuid());

        List<BackupStorageInventory> bsList = findImageStoreBackupStorageInZone(zoneUuid);

        List<String> backupStorageUuids = bsList.stream().map(BackupStorageInventory::getUuid).collect(Collectors.toList());
        Map<String, String> resourceConfigValueMap = rcf.getResourceConfigValues(
                BackupStorageGlobalConfig.RESERVED_CAPACITY, backupStorageUuids, String.class);

        bsList.removeIf(bs -> bs.getAvailableCapacity() < SizeUtils.sizeStringToBytes(resourceConfigValueMap.get(bs.getUuid())) + size);
        if (bsList.isEmpty()) {
            logger.info(String.format("after subtracting reserved capacity, " +
                    "no image store backup storage found for volume %s with at least %s bytes of available capacity.", vol.getUuid(), size));
            return null;
        }

        // We prefer the ImageStore server where the root image comes from.
        if (imgUuid != null) {
            List<String> locatedBsUuids = findImageLocatedBs(imgUuid);
            Collections.shuffle(bsList);
            bsList.sort(Comparator.comparing(bs -> locatedBsUuids.contains(bs.getUuid()) ? 0 : 1));
        }

        return bsList.get(0);
    }

    public static List<String> getRemoteBsUuids() {
        return Q.New(SystemTagVO.class).select(SystemTagVO_.resourceUuid)
                .in(SystemTagVO_.tag, Arrays.asList(
                        ImageStoreSystemTags.IS_REMOTE_BACKUP.getTagFormat(),
                        ImageStoreSystemTags.IS_FROM_ALIYUN.getTagFormat()))
                .listValues();
    }

    public static boolean isFromAliyun(String bsUuid) {
        return ImageStoreSystemTags.IS_FROM_ALIYUN.hasTag(bsUuid);
    }

    public static boolean isRemote(String bsUuid) {
        return ImageStoreSystemTags.IS_REMOTE_BACKUP.hasTag(bsUuid) ||
                ImageStoreSystemTags.IS_FROM_ALIYUN.hasTag(bsUuid);
    }

    public static boolean backupOnlyBackupStorage(String uuid) {
        return ImageStoreSystemTags.ONLY_FOR_BACKUP.hasTag(uuid);
    }
}
