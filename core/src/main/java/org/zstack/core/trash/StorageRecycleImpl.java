package org.zstack.core.trash;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.trash.InstallPathRecycleVO;
import org.zstack.header.core.trash.InstallPathRecycleVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotAfterDeleteExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.inerr;

/**
 * Created by mingjian.deng on 2019/9/19.
 */
public class StorageRecycleImpl implements StorageTrash, VolumeSnapshotAfterDeleteExtensionPoint, VolumeBeforeExpungeExtensionPoint,
        VolumeJustBeforeDeleteFromDbExtensionPoint,  Component {
    private final static CLogger logger = Utils.getLogger(StorageRecycleImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;

    private String getResourceType(String resourceUuid) {
        ResourceVO vo = dbf.findByUuid(resourceUuid, ResourceVO.class);
        if (vo == null) {
            throw new OperationFailureException(inerr("cannot find ResourceVO for resourceUuid: %s, maybe it has been deleted", resourceUuid));
        }
        return vo.getResourceType();
    }

    private InstallPathRecycleInventory createRecycleFromVolume(TrashType type, boolean isFolder, final VolumeInventory vol) {
        InstallPathRecycleVO vo = new InstallPathRecycleVO();
        vo.setFolder(isFolder);
        vo.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(vol.getFormat()).toString());
        vo.setInstallPath(vol.getInstallPath());
        vo.setResourceType(VolumeVO.class.getSimpleName());
        vo.setResourceUuid(vol.getUuid());
        vo.setStorageUuid(vol.getPrimaryStorageUuid());
        vo.setStorageType(PrimaryStorageVO.class.getSimpleName());
        vo.setTrashType(type.toString());
        vo.setSize(vol.getSize());

        for (CreateRecycleExtensionPoint ext : pluginRgty.getExtensionList(CreateRecycleExtensionPoint.class)) {
            ext.setHostUuid(vo, vol.getPrimaryStorageUuid());
        }

        vo = dbf.persistAndRefresh(vo);
        logger.debug(String.format("create trash for volume[uuid:%s, installPath:%s] on primary storage[uuid:%s], trashId: %s, type: %s",
                vol.getUuid(), vol.getInstallPath(), vol.getPrimaryStorageUuid(), vo.getTrashId(), type));
        return InstallPathRecycleInventory.valueOf(vo);
    }

    private InstallPathRecycleInventory createRecycleFromImage(TrashType type, boolean isFolder, final ImageInventory image) {
        InstallPathRecycleVO vo = new InstallPathRecycleVO();
        vo.setFolder(isFolder);
        vo.setInstallPath(image.getUrl());
        vo.setResourceType(ImageVO.class.getSimpleName());
        vo.setResourceUuid(image.getUuid());
        //the description field temporarily records the uuid value of image storage
        vo.setStorageUuid(image.getDescription());
        vo.setStorageType(BackupStorageVO.class.getSimpleName());
        vo.setTrashType(type.toString());
        vo.setSize(image.getSize());

        vo = dbf.persistAndRefresh(vo);
        logger.debug(String.format("create trash for image[uuid:%s, installPath:%s] on backup storage[uuid:%s], trashId: %s, type: %s",
                image.getUuid(), image.getUrl(), image.getDescription(), vo.getTrashId(), type));
        return InstallPathRecycleInventory.valueOf(vo);
    }

    private InstallPathRecycleInventory createRecycleFromVolumeSnapshot(TrashType type, boolean isFolder, final VolumeSnapshotInventory snapshot) {
        InstallPathRecycleVO vo = new InstallPathRecycleVO();
        vo.setFolder(isFolder);
        vo.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(snapshot.getFormat()).toString());
        vo.setInstallPath(snapshot.getPrimaryStorageInstallPath());
        vo.setResourceType(VolumeSnapshotVO.class.getSimpleName());
        vo.setResourceUuid(snapshot.getUuid());
        vo.setStorageUuid(snapshot.getPrimaryStorageUuid());
        vo.setStorageType(PrimaryStorageVO.class.getSimpleName());
        vo.setTrashType(type.toString());
        vo.setSize(snapshot.getSize());

        for (CreateRecycleExtensionPoint ext : pluginRgty.getExtensionList(CreateRecycleExtensionPoint.class)) {
            ext.setHostUuid(vo, snapshot.getPrimaryStorageUuid());
        }

        vo = dbf.persistAndRefresh(vo);
        logger.debug(String.format("create trash for volume snapshot[uuid:%s, installPath:%s] on primary storage[uuid:%s], trashId: %s, type: %s",
                snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getPrimaryStorageUuid(), vo.getTrashId(), type));
        return InstallPathRecycleInventory.valueOf(vo);
    }

    @Override
    public InstallPathRecycleInventory createTrash(TrashType type, boolean isFolder, Object o) {
        if (o instanceof VolumeInventory) {
            return createRecycleFromVolume(type, isFolder, (VolumeInventory)o);
        } else if (o instanceof ImageInventory) {
            return createRecycleFromImage(type, isFolder, (ImageInventory)o);
        } else if (o instanceof VolumeSnapshotInventory) {
            return createRecycleFromVolumeSnapshot(type, isFolder, (VolumeSnapshotInventory)o);
        } else {
            throw new OperationFailureException(inerr("non support resourceType to create trash"));
        }
    }

    @Override
    public List<InstallPathRecycleInventory> getTrashList(String storageUuid) {
        return getTrashList(storageUuid, CollectionDSL.list(TrashType.values()));
    }

    @Override
    public List<InstallPathRecycleInventory> getTrashList(String storageUuid, List<TrashType> types) {
        List<InstallPathRecycleInventory> invs = new ArrayList<>();
        for (TrashType type: types) {
            List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.storageUuid, storageUuid).eq(InstallPathRecycleVO_.trashType, type.toString()).list();
            invs.addAll(InstallPathRecycleInventory.valueOf(vos));
        }
        return invs;
    }

    @Override
    public InstallPathRecycleInventory getTrash(Long trashId) {
        InstallPathRecycleVO vo = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.trashId, trashId).find();
        if (vo == null) {
            return null;
        }
        return InstallPathRecycleInventory.valueOf(vo);
    }

    private String checkVolume(String installPath) {
        List<String> uuids = Q.New(VolumeVO.class).eq(VolumeVO_.installPath, installPath).select(VolumeVO_.uuid).listValues();
        if (uuids.size() > 0) {
            return String.format("%s is still in using by volume %s, cannot remove it from trash before delete them", installPath, uuids);
        }
        return null;
    }

    private String checkImageCache(String installPath) {
        List<String> ids = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.installUrl, installPath).select(ImageCacheVO_.id).listValues();
        if (ids.size() > 0) {
            return String.format("%s is still in using by imagecache %s, cannot remove it from trash before delete them", installPath, ids);
        }
        return null;
    }

    private String checkInnerVolumeSnapshot(String installPath) {
        List<String> uuids = Q.New(VolumeSnapshotVO.class).like(VolumeSnapshotVO_.primaryStorageInstallPath, installPath + "@%").select(VolumeSnapshotVO_.uuid).listValues();
        if (uuids.size() > 0) {
            return String.format("%s is still in using by volumesnapshot %s, cannot remove it from trash before delete them", installPath, uuids);
        }
        return null;
    }

    private String checkVolumeSnapshot(String installPath) {
        List<String> uuids = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageInstallPath, installPath).select(VolumeSnapshotVO_.uuid).listValues();
        if (uuids.size() > 0) {
            return String.format("%s is still in using by volumesnapshot %s, cannot remove it from trash before delete them", installPath, uuids);
        }
        return checkInnerVolumeSnapshot(installPath);
    }

    private String checkImage(String installPath) {
        List<String> uuids = Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.installPath, installPath).select(ImageBackupStorageRefVO_.imageUuid).listValues();
        if (uuids.size() > 0) {
            return String.format("%s is still in using by image %s, cannot remove it from trash before delete them", installPath, uuids);
        }
        return null;
    }

    @Transactional(readOnly = true)
    protected String makeSureInstallPathNotUsedByVolume(String installPath) {
        String details = checkVolume(installPath);
        if (details != null) {
            return details;
        }
        details = checkImageCache(installPath);
        if (details != null) {
            return details;
        }
        details = checkInnerVolumeSnapshot(installPath);
        if (details != null) {
            return details;
        }
        return null;
    }

    @Transactional(readOnly = true)
    protected String makeSureInstallPathNotUsedByImage(String installPath) {
        String details = checkImage(installPath);
        if (details != null) {
            return details;
        }
        return null;
    }

    @Transactional(readOnly = true)
    protected String makeSureInstallPathNotUsedBySnapshot(String installPath) {
        String details = checkVolumeSnapshot(installPath);
        if (details != null) {
            return details;
        }
        details = checkImageCache(installPath);
        if (details != null) {
            return details;
        }
        return null;
    }

    @Override
    public String makeSureInstallPathNotUsed(InstallPathRecycleInventory inv) {
        if (VolumeVO.class.getSimpleName().equals(inv.getResourceType())) {
            String psType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, inv.getStorageUuid()).select(PrimaryStorageVO_.type).findValue();
            DeleteRecycleExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, DeleteRecycleExtensionPoint.class);
            if (ext != null) {
                return ext.makeSureInstallPathNotUsed(inv);
            } else {
                return makeSureInstallPathNotUsedByVolume(inv.getInstallPath());
            }
        }
        return makeSureInstallPathNotUsed(inv.getInstallPath(), inv.getResourceType());
    }

    @Override
    @Transactional(readOnly = true)
    public String makeSureInstallPathNotUsed(String installPath, String resourceType) {
        if (VolumeVO.class.getSimpleName().equals(resourceType)) {
            return makeSureInstallPathNotUsedByVolume(installPath);
        } else if (ImageVO.class.getSimpleName().equals(resourceType)) {
            return makeSureInstallPathNotUsedByImage(installPath);
        } else if (VolumeSnapshotVO.class.getSimpleName().equals(resourceType)) {
            return makeSureInstallPathNotUsedBySnapshot(installPath);
        }
        return null;
    }

    @Override
    public String makeSurePrimaryStorageInstallPathNotUsed(String installPath) {
        String details = checkVolume(installPath);
        if (details != null) {
            return details;
        }
        details = checkImageCache(installPath);
        if (details != null) {
            return details;
        }
        details = checkVolumeSnapshot(installPath);
        if (details != null) {
            return details;
        }
        return null;
    }

    @Override
    public Long getTrashId(String storageUuid, String installPath) {
        DebugUtils.Assert(installPath != null, "installPath is not allowed null here");
        List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.storageUuid, storageUuid).like(InstallPathRecycleVO_.installPath, String.format("%%%s%%", installPath)).list();
        if (!vos.isEmpty()) {
            for (InstallPathRecycleVO vo: vos) {
                if (vo.getInstallPath().equals(installPath)) {
                    return vo.getTrashId();
                }
            }
        }
        return null;
    }

    @Override
    public void removeFromDb(Long trashId) {
        DebugUtils.Assert(trashId != null, "trashId is not allowed null here");
        UpdateQuery.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.trashId, trashId).delete();
        logger.debug(String.format("remove trash[trashId:%s] from db", trashId));
    }

    private void deleteTrashForVolume(String resourceUuid, String primaryStorageUuid, Completion completion) {
        List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.storageUuid, primaryStorageUuid).list();
        List<Long> trashIds = new ArrayList<>();
        for (InstallPathRecycleVO vo: vos) {
            if (vo.getResourceUuid().equals(resourceUuid)) {
                trashIds.add(vo.getTrashId());
            }
        }

        deleteTrashForVolume(trashIds.iterator(), primaryStorageUuid, completion);
    }

    private void deleteTrashForVolume(final Iterator<Long> trashIds, String primaryStorageUuid, Completion completion) {
        if (!trashIds.hasNext()) {
            completion.success();
            return;
        }
        Long trashId = trashIds.next();

        CleanUpTrashOnPrimaryStroageMsg pmsg = new CleanUpTrashOnPrimaryStroageMsg();
        pmsg.setPrimaryStorageUuid(primaryStorageUuid);
        pmsg.setTrashId(trashId);
        bus.makeTargetServiceIdByResourceUuid(pmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(pmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("clean up trash [%s] on primary storage [%s] failed, because: %s",
                            pmsg.getTrashId(), primaryStorageUuid, reply.getError().getDetails()));
                }
                deleteTrashForVolume(trashIds, primaryStorageUuid, completion);
            }
        });
    }

    @Override
    public void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion) {
        deleteTrashForVolume(snapshot.getUuid(), snapshot.getPrimaryStorageUuid(), completion);
    }

    @Override
    public void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot) {

    }

    @Override
    public void volumeSnapshotAfterCleanUpExtensionPoint(String volumeUuid, List<VolumeSnapshotInventory> snapshots) {
    }

    private void transfer(List<JsonLabelVO> trashs, String type) {
        List<String> recycles = new ArrayList<>();
        for (JsonLabelVO trash: trashs) {
            InstallPathRecycleVO vo = JSONObjectUtil.toObject(trash.getLabelValue(), InstallPathRecycleVO.class);
            try {
                vo = dbf.persistAndRefresh(vo);
                recycles.add(String.valueOf(vo.getTrashId()));
            } catch (Exception e) {
                logger.warn(e.getMessage());
                logger.warn(trash.getLabelValue());
            }
        }
        for (JsonLabelVO trash: trashs) {
            try {
                dbf.remove(trash);
            } catch (Exception e) {
                logger.warn(e.getMessage());
                logger.warn(trash.getLabelValue());
            }

        }
        if (!recycles.isEmpty()) {
            logger.debug(String.format("transfer %s trash to recycles from %s: %s", recycles.size(), type, recycles));
        }
    }

    private void transferTrashDataFromOldVersion() {
        for (TrashType type: TrashType.values()) {
            List<JsonLabelVO> vos = Q.New(JsonLabelVO.class).like(JsonLabelVO_.labelKey, type.toString() + "-%").list();
            transfer(vos, type.toString());
        }
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(DeleteRecycleExtensionPoint.class, new Function<Object, DeleteRecycleExtensionPoint>() {
            @Override
            public Object call(DeleteRecycleExtensionPoint arg) {
                return arg.getPrimaryStorageType().toString();
            }
        });

        transferTrashDataFromOldVersion();
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return stop();
        } else {
            // we insert test data in ut, so we start a timer for test case
            thdf.submitPeriodicTask(new PeriodicTask(){
                @Override
                public TimeUnit getTimeUnit() {
                    return TimeUnit.MILLISECONDS;
                }

                @Override
                public long getInterval() {
                    return 100;
                }

                @Override
                public String getName() {
                    return "transferTrashDataFromOldVersion";
                }

                @Override
                public void run() {
                    transferTrashDataFromOldVersion();
                }
            });
            return true;
        }
    }

    @Override
    public List<String> findTrashInstallPath(String installPath, String storageUuid) {
        List<String> absPaths = Q.New(InstallPathRecycleVO.class).select(InstallPathRecycleVO_.installPath)
                .eq(InstallPathRecycleVO_.storageUuid, storageUuid)
                .like(InstallPathRecycleVO_.installPath, String.format("%s%%", installPath))
                .listValues();
        return absPaths.stream().map(it -> it.substring(installPath.length())).collect(Collectors.toList());
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void volumePreExpunge(VolumeInventory volume) {}

    @Override
    public void volumeBeforeExpunge(VolumeInventory volume, Completion completion) {
        deleteTrashForVolume(volume.getUuid(), volume.getPrimaryStorageUuid(), completion);
    }

    @Override
    public void volumeJustBeforeDeleteFromDb(VolumeInventory inv) {
        deleteTrashInDb(inv);
    }
    
    private void deleteTrashInDb(VolumeInventory volume) {
        SQL.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.resourceUuid, volume.getUuid()).eq(InstallPathRecycleVO_.storageUuid, volume.getPrimaryStorageUuid()).delete();
    }
}
