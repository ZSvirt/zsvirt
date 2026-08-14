package org.zstack.storage.primary.sharedblock.migration;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.ImageBackupStorageSelector;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.*;
import org.zstack.longjob.LongJobUtils;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.primary.sharedblock.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.primary.sharedblock.SharedBlockConstants.SHARED_BLOCK_INSTALL_PATH_SCHEME;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Create by weiwang at 2018/6/28
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SblkToSblkMigrateVolumeFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(SblkToSblkMigrateVolumeFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected JobQueueFacade jobf;
    @Autowired
    protected ApiTimeoutManager timeoutMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger outerTrigger, Map outerData) {
        String volumeUuid = (String) outerData.get(StorageMigrationConstant.VOLUME_UUID);
        String srcPsUuid = (String) outerData.get(StorageMigrationConstant.SRC_PS_UUID);
        String dstPsUuid = (String) outerData.get(StorageMigrationConstant.DST_PS_UUID);
        boolean withSnapshots = (boolean) outerData.get(StorageMigrationConstant.WITH_SNAPSHOTS);
        String independentPath = (String) outerData.get(StorageMigrationConstant.INDEPENDENT_PATH);
        boolean independentImageCache = independentPath != null;
        List<SharedBlockMigrateVolumeStruct> structs = new ArrayList<>();

        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        volumeVO.setStatus(VolumeStatus.Migrating);
        dbf.update(volumeVO);
        outerData.put(StorageMigrationConstant.DST_VOLUME_INSTALL_PATH, volumeVO.getInstallPath().replace(
                srcPsUuid, dstPsUuid));

        ImageVO imageVO = volumeVO.getRootImageUuid() == null ? null :
                dbf.findByUuid(volumeVO.getRootImageUuid(), ImageVO.class);
        String zoneUuid = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.zoneUuid)
                .eq(PrimaryStorageVO_.uuid, dstPsUuid)
                .findValue();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.enableProgressReport();
        chain.setName(String.format("migrate-volume-%s-between-sblk-%s-and-sblk-%s", volumeUuid, srcPsUuid, dstPsUuid));
        chain.setData(map(e(StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_COPY_IMAGE_CACHE, false)));
        chain.preCheck(d -> outerData.containsKey(StorageMigrationConstant.VOLUME_MIGRATED) ?
                null : LongJobUtils.buildErrIfCanceled());
        String dstVolumeFolderPath = volumeVO.getInstallPath().replace(srcPsUuid, dstPsUuid);
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-if-install-path-in-trash";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                CheckInstallPathInTrashMsg cmsg = new CheckInstallPathInTrashMsg();
                cmsg.setPrimaryStorageUuid(dstPsUuid);
                cmsg.setInstallPath(dstVolumeFolderPath);
                bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            CheckInstallPathInTrashReply reply1 = reply.castReply();
                            if (reply1.getTrashId() != null) {
                                trigger.fail(Platform.operr("found trashId(%s) in PrimaryStorage [%s] for the migrate installPath[%s]. Please clean it first by 'APICleanUpTrashOnPrimaryStorageMsg' if you insist to migrate the volume[%s]",
                                        reply1.getTrashId(), dstPsUuid, dstVolumeFolderPath, reply1.getResourceUuid()));
                            } else {
                                trigger.next();
                            }
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "download image to cache if necessary";

            boolean allocatePrimaryStorageForImageCache = false;

            @Override
            public boolean skip(Map data) {
                if (!withSnapshots || independentImageCache) {
                    return true;
                }

                // if data volume, pass
                if (volumeVO.getType() == VolumeType.Data) {
                    logger.info("no need to download image for data volume");
                    return true;
                }

                // check whether needs to download image to cache
                boolean alreadyDownloaded = Q.New(ImageCacheVO.class)
                        .eq(ImageCacheVO_.imageUuid, volumeVO.getRootImageUuid())
                        .eq(ImageCacheVO_.primaryStorageUuid, dstPsUuid)
                        .isExists();
                if (alreadyDownloaded) {
                    logger.info("imageCache is already there for root volume " + volumeUuid);
                    return true;
                }

                if (imageVO == null) {
                    logger.debug(String.format("root image[uuid:%s] has been deleted, should get info from ps", volumeVO.getRootImageUuid()));
                    data.put(StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_COPY_IMAGE_CACHE, true);
                    return true;
                }

                if (imageVO.getMediaType() == ImageConstant.ImageMediaType.ISO) {
                    logger.debug(String.format("no need to download iso image[uuid:%s] to image cache of ps[uuid:%s] " +
                            "when storage migrate volume[uuid:%s]", imageVO.getUuid(), dstPsUuid, volumeUuid));
                    return true;
                }

                return false;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {


                String imageUuid = volumeVO.getRootImageUuid();
                PrimaryStorageInventory dstPsInv = PrimaryStorageInventory.valueOf(
                        dbf.findByUuid(dstPsUuid, PrimaryStorageVO.class)
                );

                // select backup storage
                ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
                selector.setImageUuid(imageUuid);
                selector.setZoneUuid(zoneUuid);
                final String bsUuid = selector.select();
                if (bsUuid == null) {
                    throw new OperationFailureException(operr("cannot find the image[uuid:%s] in any connected backup storage attached to the zone[uuid:%s]. check below:\n" +
                                    "1. whether the backup storage is attached to the zone[uuid:%s]\n" +
                                    "2. whether the backup storage is in connected status; try to reconnect it if not",
                            imageUuid, zoneUuid, zoneUuid)
                    );
                }
                logger.info("the selected backup storage to download image from is " + bsUuid);

                ImageInventory image = ImageInventory.valueOf(imageVO);
                VmInstanceSpec.ImageSpec imageSpec = new VmInstanceSpec.ImageSpec();
                imageSpec.setSelectedBackupStorage(CollectionUtils.findOneOrNull(image.getBackupStorageRefs(),
                        arg -> arg.getBackupStorageUuid().equals(bsUuid)
                                && ImageStatus.Ready.toString().equals(arg.getStatus())));
                imageSpec.setInventory(image);

                if (image.getActualSize() > dstPsInv.getAvailablePhysicalCapacity()) {
                    throw new OperationFailureException(operr("there are not enough capacity for image[uuid: %s] download while volume[uuid: %s] storage migration," +
                            " required capacity: %s, current available physical capacity: %s", image.getUuid(), volumeUuid, image.getActualSize(), dstPsInv.getAvailablePhysicalCapacity()));
                }

                ImageBackupStorageRefVO imageBackupStorageRefVO = Q.New(ImageBackupStorageRefVO.class)
                        .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, imageSpec.getSelectedBackupStorage().getBackupStorageUuid())
                        .find();

                SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
                q.select(BackupStorageVO_.type);
                q.add(BackupStorageVO_.uuid, SimpleQuery.Op.EQ, bsUuid);
                String bsType = q.findValue();

                BackupStorageSharedBlockKvmDownloader downloader = getBackupStorageKvmDownloader(dstPsInv, imageSpec.getSelectedBackupStorage().getBackupStorageUuid(), bsType);
                if (downloader == null) {
                    logger.debug(String.format("selected bs: %s, sharedblock not support %s backupstorage yet, skip download image cache", bsUuid, bsType));
                    data.put(StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_COPY_IMAGE_CACHE, true);
                    trigger.next();
                    return;
                }

                downloader.downloadBits(imageBackupStorageRefVO.getInstallPath(),
                        makeImageCachePrimaryStorageInstallPath(dstPsUuid, imageUuid),
                        LvmlockdLockingType.SHARE, new Completion(trigger) {
                            @Override
                            public void success() {
                                ImageCacheVO vo = new ImageCacheVO();
                                vo.setState(ImageCacheState.ready);
                                vo.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getMediaType()));
                                vo.setImageUuid(image.getUuid());
                                vo.setPrimaryStorageUuid(dstPsUuid);
                                vo.setSize(image.getActualSize());
                                vo.setMd5sum("not calculated");
                                vo.setInstallUrl(makeImageCachePrimaryStorageInstallPath(
                                        dstPsUuid, imageUuid));
                                dbf.persist(vo);

                                logger.debug(String.format("downloaded image[uuid:%s, name:%s] to the image cache of shared block storage[uuid: %s, installPath: %s]",
                                        image.getUuid(), image.getName(), dstPsUuid, vo.getInstallUrl()));

                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check capacity after image cache downloaded";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                PrimaryStorageInventory dstPsInv = PrimaryStorageInventory.valueOf(
                        dbf.findByUuid(dstPsUuid, PrimaryStorageVO.class)
                );

                if (volumeVO.getActualSize() > dstPsInv.getAvailablePhysicalCapacity()) {
                    throw new OperationFailureException(operr("there are not enough capacity for volume[uuid: %s] storage migration," +
                            " required capacity: %s, current available physical capacity: %s", volumeUuid, volumeVO.getActualSize(), dstPsInv.getAvailablePhysicalCapacity()));
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-backing-file-chain-not-in-db";

            long totalSize = 0;
            List<ImageCacheVO> toPersistCache = new ArrayList<>();

            @AfterDone
            List<Runnable> runnables = Collections.singletonList(() -> dbf.persistCollection(toPersistCache));

            @Override
            public boolean skip(Map data) {
                return independentImageCache;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                boolean containCurrentChain = (boolean) data.get(StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_COPY_IMAGE_CACHE)
                        || volumeVO.getRootImageUuid() != null && volumeVO.getType() == VolumeType.Data;

                GetVolumeBackingInstallPathMsg gmsg = new GetVolumeBackingInstallPathMsg();
                gmsg.setVolumeUuid(volumeUuid);
                bus.makeLocalServiceId(gmsg, VolumeConstant.SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        GetVolumeBackingInstallPathReply r = reply.castReply();
                        handleReply(r);
                        trigger.next();
                    }

                    @Transactional(readOnly = true)
                    private void handleReply(GetVolumeBackingInstallPathReply r) {
                        if (containCurrentChain) {
                            r.getCurrentBackingChain().forEach(this::addStruct);
                            totalSize += r.getCurrentBackingChainSize();
                            if (volumeVO.getType() == VolumeType.Root && !r.getCurrentBackingChain().isEmpty()) {
                                buildCurrentImageCache(r.getCurrentBackingChain().get(0), r.getCurrentBackingChainSize());
                            }
                        }

                        for (int i = 0; i < r.getPreviousBackingChains().size(); i++) {
                            List<String> chain = r.getPreviousBackingChains().get(i);
                            long size = r.getPreviousBackingChainSizes().get(i);
                            totalSize+=size;
                            chain.forEach(this::addStruct);
                            if (volumeVO.getType() == VolumeType.Root && !chain.isEmpty()) {
                                buildPreviousImageCache(chain.get(0), size);
                            }
                        }

                        data.put(StorageMigrationConstant.IMAGE_SIZE, totalSize);
                    }

                    private void addStruct(String path) {
                        SharedBlockMigrateVolumeStruct struct = new SharedBlockMigrateVolumeStruct();
                        struct.setCurrentInstallPath(path);
                        struct.setTargetInstallPath(struct.currentInstallPath.replace(srcPsUuid, dstPsUuid));
                        struct.safeMode = false;
                        struct.compareQcow2 = true;
                        struct.skipIfExisting = true;
                        struct.setVolumeUuid(volumeUuid);
                        structs.add(struct);
                    }

                    private void buildCurrentImageCache(String path, long size) {
                        String expectImageUuid = volumeVO.getRootImageUuid();
                        String imageUuid = getImageUuidFromCachePrimaryStorageInstallPath(path);
                        if (!expectImageUuid.equals(imageUuid)) {
                            logger.error(String.format("install path[%s] seems not be cache of image[uuid:%s], " +
                                    "but we decide to add it to db.", path, expectImageUuid));
                        }
                        buildImageCache(path, size, expectImageUuid);
                    }

                    private void buildPreviousImageCache(String path, long size) {
                        String imageUuid = getImageUuidFromCachePrimaryStorageInstallPath(path);
                        buildImageCache(path, size, imageUuid);
                    }

                    @Transactional(readOnly = true)
                    private void buildImageCache(String path, long size, String imageUuid) {
                        if (!StringDSL.isZStackUuid(imageUuid)) {
                            logger.warn(String.format("attempt to build cache has wrong image uuid[%s], skip it.", imageUuid));
                            return;
                        }

                        boolean cacheExists = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, imageUuid)
                                .eq(ImageCacheVO_.primaryStorageUuid, dstPsUuid)
                                .isExists();
                        if (cacheExists) {
                            return;
                        }

                        String existingResourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType)
                                .eq(ResourceVO_.uuid, imageUuid).findValue();
                        if (existingResourceType != null && !existingResourceType.equals(ImageVO.class.getSimpleName())) {
                            logger.warn(String.format("attempt to build cache has wrong image uuid[%s], actually it is %s, skip it.",
                                    imageUuid, existingResourceType));
                            return;
                        }

                        ImageCacheVO vo = new ImageCacheVO();
                        vo.setState(ImageCacheState.ready);
                        vo.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate);
                        vo.setImageUuid(imageUuid);
                        vo.setPrimaryStorageUuid(dstPsUuid);
                        vo.setSize(size);
                        vo.setMd5sum("not calculated");
                        vo.setInstallUrl(makeSblkInstallUrl(path.replace(srcPsUuid, dstPsUuid)));
                        toPersistCache.add(vo);
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "allocate space for backing chain";

            @Override
            public boolean skip(Map data) {
                return independentImageCache || (long) data.get(StorageMigrationConstant.IMAGE_SIZE) == 0;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                amsg.setRequiredPrimaryStorageUuid(dstPsUuid);
                amsg.setSize((long) data.get(StorageMigrationConstant.IMAGE_SIZE));
                amsg.setNoOverProvisioning(true);
                bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                imsg.setDiskSize((long)data.get(StorageMigrationConstant.IMAGE_SIZE));
                imsg.setNoOverProvisioning(true);
                imsg.setPrimaryStorageUuid(dstPsUuid);
                bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
                bus.send(imsg);

                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "migrate volume and snapshots while with snapshot";

            @Override
            public boolean skip(Map data) {
                return !withSnapshots;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg mmsg = new MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg();
                mmsg.setPrimaryStorageUuid(srcPsUuid);
                mmsg.setTargetPrimaryStorageUuid(dstPsUuid);
                mmsg.setMigrateVolumeStructs(structs);
                mmsg.setVolumePath(volumeVO.getInstallPath());
                List<VolumeSnapshotVO> snapshotVOS = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).list();
                if (snapshotVOS != null && !snapshotVOS.isEmpty()) {
                    for (VolumeSnapshotVO snapshotVO : snapshotVOS) {
                        SharedBlockMigrateVolumeStruct struct = new SharedBlockMigrateVolumeStruct();
                        struct.volumeUuid = volumeUuid;
                        struct.snapshotUuid = snapshotVO.getUuid();
                        struct.currentInstallPath = snapshotVO.getPrimaryStorageInstallPath();
                        struct.targetInstallPath = struct.currentInstallPath.replace(srcPsUuid, dstPsUuid);
                        struct.safeMode = false;
                        struct.compareQcow2 = true;
                        struct.independent = struct.currentInstallPath.equals(independentPath);
                        if (struct.currentInstallPath.equalsIgnoreCase(struct.targetInstallPath)) {
                            logger.warn(String.format("snapshot[uuid: %s] current install path equals to target[%s]",
                                    snapshotVO.getUuid(), snapshotVO.getPrimaryStorageInstallPath()));
                            continue;
                        }
                        mmsg.getMigrateVolumeStructs().add(struct);
                    }
                }

                SharedBlockMigrateVolumeStruct struct = new SharedBlockMigrateVolumeStruct();
                struct.volumeUuid = volumeUuid;
                struct.currentInstallPath = volumeVO.getInstallPath();
                struct.targetInstallPath = dstVolumeFolderPath;
                struct.safeMode = false;
                struct.compareQcow2 = true;
                struct.independent = struct.currentInstallPath.equals(independentPath);
                if (!struct.currentInstallPath.equalsIgnoreCase(struct.targetInstallPath)) {
                    mmsg.getMigrateVolumeStructs().add(struct);
                }

                if (mmsg.getMigrateVolumeStructs().isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.makeLocalServiceId(mmsg, PrimaryStorageConstant.SERVICE_ID);
                bus.send(mmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply reply1 = reply.castReply();
                        if (!reply1.isSuccess()) {
                            trigger.fail(reply1.getError());
                            return;
                        }

                        Map<String, String> migratedSnapshots = mmsg.getMigrateVolumeStructs().stream()
                                .filter(s -> s.snapshotUuid != null)
                                .collect(Collectors.toMap(SharedBlockMigrateVolumeStruct::getSnapshotUuid, SharedBlockMigrateVolumeStruct::getTargetInstallPath));
                        outerData.put(StorageMigrationConstant.DST_VOLUME_SNAPSHOT_PATHS, migratedSnapshots);
                        logger.debug(String.format("Migrated snapshots[uuids: %s, install paths: %s] for volume[%s]",
                                migratedSnapshots.keySet(), migratedSnapshots.values(), volumeUuid));
                        outerData.put(StorageMigrationConstant.VOLUME_MIGRATED, true);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "migrate volume while without snapshot";

            @Override
            public boolean skip(Map data) {
                return withSnapshots;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg tmsg = new TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg();
                tmsg.setPrimaryStorageUuid(srcPsUuid);
                tmsg.setInstallPath(dstVolumeFolderPath);
                tmsg.setFull(true);
                tmsg.setVolumeUuid(volumeUuid);
                tmsg.setVolumeInstallPath(volumeVO.getInstallPath());
                tmsg.setTargetPrimaryStorageUuid(dstPsUuid);
                tmsg.setCompareQocw2(true);
                bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, srcPsUuid);
                bus.send(tmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()){
                            trigger.fail(reply.getError());
                            return;
                        }

                        TakeSnapshotOnSharedBlockGroupPrimaryStorageReply treply = reply.castReply();
                        if (!treply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(outerTrigger) {
            @Override
            public void handle(Map data) {
                outerTrigger.next();
            }
        }).error(new FlowErrorHandler(outerTrigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                outerTrigger.fail(errCode);
            }
        }).start();
    }

    @Override
    public void rollback(FlowRollback flowRollback, Map data) {
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);

        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        volumeVO.setStatus(VolumeStatus.Ready);
        dbf.update(volumeVO);
        flowRollback.rollback();
    }

    private String makeImageCachePrimaryStorageInstallPath(String psUuid, String imageUuid) {
        return String.format("%s%s/%s",
                SHARED_BLOCK_INSTALL_PATH_SCHEME, psUuid, imageUuid);
    }

    private String getImageUuidFromCachePrimaryStorageInstallPath(String path) {
        String[] s = path.split("/");
        return s[s.length - 1];
    }

    private String makeSblkInstallUrl(String path) {
        return path.replace("/dev/", SHARED_BLOCK_INSTALL_PATH_SCHEME);
    }

    private BackupStorageSharedBlockKvmDownloader getBackupStorageKvmDownloader(PrimaryStorageInventory dstInv, String bsUuid, String bsType) {
        for (BackupStorageSharedBlockKvmFactory f : pluginRgty.getExtensionList(BackupStorageSharedBlockKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createDownloader(dstInv, bsUuid);
            }
        }

        return null;
    }
}
