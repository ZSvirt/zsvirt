package org.zstack.storage.migration.primary.nfs;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.*;
import org.zstack.longjob.LongJobUtils;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.primary.nfs.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 11/26/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsToNfsMigrateVolumeFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(NfsToNfsMigrateVolumeFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected JobQueueFacade jobf;
    @Autowired
    private NfsPrimaryStorageFactory nfsFactory;

    private static final String MIGRATED = "migrated";

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        String srcPsUuid = (String) data.get(StorageMigrationConstant.SRC_PS_UUID);
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        String independentPath = (String) data.get(StorageMigrationConstant.INDEPENDENT_PATH);

        String srcPsMountPath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, srcPsUuid)
                .findValue();
        String dstPsMountPath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, dstPsUuid)
                .findValue();

        // select host
        PrimaryStorageInventory srcPsInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(srcPsUuid, PrimaryStorageVO.class));
        PrimaryStorageInventory dstPsInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(dstPsUuid, PrimaryStorageVO.class));
        List<HostInventory> hosts = nfsFactory.getConnectedHostForOperation(srcPsInv);
        if (hosts == null || hosts.size() == 0) {
            throw new OperationFailureException(operr("cannot find any connected host to perform the storage migration operation"));
        }
        HostInventory host = hosts.get(0);
        String hostUuid = host.getUuid();
        data.put(StorageMigrationConstant.NFS_TO_NFS_MIGRATE_VOLUME_CHOOSEN_HOST, hostUuid);

        // update volume status before migration
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        volume.setStatus(VolumeStatus.Migrating);
        dbf.update(volume);
        String imageUuid = volume.getRootImageUuid();

        // get folder path of volume and snapshots
        String srcVolumeInstallPath = volume.getInstallPath();
        String srcVolumeFolderPath = getVolumeFolderPath(srcVolumeInstallPath, srcPsMountPath, srcPsMountPath);
        String dstVolumeFolderPath = getVolumeFolderPath(srcVolumeInstallPath, srcPsMountPath, dstPsMountPath);

        // record install path of dstVolume and snapshots
        String dstVolumeInstallPath = getDestInstallPath(srcVolumeInstallPath, srcPsMountPath, dstPsMountPath);

        List<VolumeSnapshotVO> snapshots = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, volumeUuid)
                .list();
        Map<String, String> dstVolumeSnapshotPaths = new HashMap<>();
        Map<String, Long> srcVolumeSnapshotsSize = new HashMap<>();
        for (VolumeSnapshotVO snapshot : snapshots) {
            dstVolumeSnapshotPaths.put(snapshot.getUuid(),
                    getDestInstallPath(snapshot.getPrimaryStorageInstallPath(), srcPsMountPath, dstPsMountPath)
            );
            srcVolumeSnapshotsSize.put(snapshot.getUuid(), snapshot.getSize());
        }
        data.put(StorageMigrationConstant.DST_VOLUME_INSTALL_PATH, dstVolumeInstallPath);
        data.put(StorageMigrationConstant.DST_VOLUME_SNAPSHOT_PATHS, dstVolumeSnapshotPaths);
        data.put(StorageMigrationConstant.SRC_VOLUME_SNAPSHOTS_SIZE, srcVolumeSnapshotsSize);

        Map<String, Long> dstFilesActualSize = new HashMap<>();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("migrate-volume-%s-from-ps-%s-to-ps-%s", volumeUuid, srcPsUuid, dstPsUuid));
        chain.preCheck(d -> LongJobUtils.buildErrIfCanceled());
        chain.enableProgressReport();
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-if-install-path-in-trash";
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
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
                                cTrigger.fail(Platform.operr("found trashId(%s) in PrimaryStorage [%s] for the migrate installPath[%s]. Please clean it first by 'APICleanUpTrashOnPrimaryStorageMsg' if you insist to migrate the volume[%s]",
                                        reply1.getTrashId(), dstPsUuid, dstVolumeFolderPath, reply1.getResourceUuid()));
                            } else if (!CollectionUtils.isEmpty(reply1.getRelatedTrashPaths())) {
                                cTrigger.fail(Platform.operr("found related trash paths(%s) in PrimaryStorage [%s] for the migrate installPath[%s]. Please clean them first by 'APICleanUpTrashOnPrimaryStorageMsg' if you insist to migrate the volume[%s]",
                                        reply1.getRelatedTrashPaths(), dstPsUuid, dstVolumeFolderPath, reply1.getResourceUuid()));
                            } else {
                                cTrigger.next();
                            }
                        } else {
                            cTrigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "download-image-to-cache-if-necessary";

            @Override
            public boolean skip(Map data) {
                // if data volume, pass
                if (volume.getType() == VolumeType.Data) {
                    logger.info("no need to download image for data volume");
                    return true;
                }

                // if image type is ISO, pass
                boolean isISO = Q.New(ImageVO.class)
                        .eq(ImageVO_.uuid, imageUuid)
                        .eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.ISO)
                        .isExists();
                if (isISO) {
                    return true;
                }

                // check whether needs to download image to cache
                boolean alreadyDownloaded = Q.New(ImageCacheVO.class)
                        .eq(ImageCacheVO_.imageUuid, imageUuid)
                        .eq(ImageCacheVO_.primaryStorageUuid, dstPsUuid)
                        .isExists();
                if (alreadyDownloaded) {
                    logger.info("imageCache is already there for root volume " + volumeUuid);
                    return true;
                }

                if (independentPath != null) {
                    logger.info(String.format("migrate volume[uuid: %s] will independent backing resource.", volumeUuid));
                    return true;
                }

                return false;
            }

            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                ImageCacheVO cache = Q.New(ImageCacheVO.class)
                        .eq(ImageCacheVO_.imageUuid, imageUuid)
                        .eq(ImageCacheVO_.primaryStorageUuid, srcPsUuid)
                        .find();
                if (cache == null) {
                    logger.debug(String.format("image[uuid:%s] has no cache on primary storage[uuid:%s], " +
                            "check if volume has image dependency", imageUuid, srcPsUuid));
                    checkNoImageDependency(cTrigger);
                } else {
                    copyImage(cache, cTrigger, data);
                }
            }

            private void copyImage(ImageCacheVO cache, FlowTrigger cTrigger, Map data) {
                ImageCacheInventory srcImageCache = ImageCacheInventory.valueOf(cache);

                NfsCopyImageCacheJob job = new NfsCopyImageCacheJob();
                job.setHost(host);
                job.setSrcImageCache(srcImageCache);
                job.setSrcPrimaryStorage(srcPsInv);
                job.setDstPrimaryStorage(dstPsInv);

                jobf.execute(NfsPrimaryStorageKvmHelper.makeCopyImageCacheJobName(srcImageCache, srcPsInv, dstPsInv),
                        NfsPrimaryStorageKvmHelper.makeJobOwnerName(dstPsInv), job,
                        new ReturnValueCompletion<ImageCacheInventory>(cTrigger) {
                            @Override
                            public void success(ImageCacheInventory returnValue) {
                                logger.info(String.format("successfully copyed cache of image[uuid:%s] to ImageCache of ps[uuid:%s]", imageUuid, dstPsUuid));
                                data.put(StorageMigrationConstant.DST_IMAGE_CACHE_TEMPLATE_INSTALL_PATH, returnValue.getInstallUrl());
                                cTrigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.error(String.format("failed to copy cache of image[uuid:%s] to ImageCache of ps[uuid:%s]", imageUuid, dstPsUuid));
                                cTrigger.fail(errorCode);
                            }
                        },
                        ImageCacheInventory.class
                );
            }

            private void checkNoImageDependency(FlowTrigger cTrigger) {
                GetVolumeRootImageUuidFromPrimaryStorageMsg msg = new GetVolumeRootImageUuidFromPrimaryStorageMsg();
                msg.setVolume(VolumeInventory.valueOf(volume));
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, srcPsUuid);
                bus.send(msg, new CloudBusCallBack(cTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            GetVolumeRootImageUuidFromPrimaryStorageReply r = reply.castReply();
                            if (r.getImageUuid() == null && CollectionUtils.isEmpty(r.getOtherImageUuids())) {
                                cTrigger.next();
                            } else {
                                cTrigger.fail(operr("volume[uuid:%s] has image[uuid:%s] dependency, " +
                                        "other dependency image[%s]", volumeUuid, r.getImageUuid(), r.getOtherImageUuids()));
                            }
                        } else {
                            cTrigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "migrate-volume-data-from-nfs-to-nfs";
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                NfsToNfsMigrateBitsMsg msg = new NfsToNfsMigrateBitsMsg();
                msg.setHostUuid(hostUuid);
                msg.setSrcPrimaryStorageUuid(srcPsUuid);
                msg.setSrcFolderPath(srcVolumeFolderPath);
                msg.setDstFolderPath(dstVolumeFolderPath);
                msg.setPrimaryStorageUuid(dstPsUuid);
                msg.setIndependentPath(independentPath);
                msg.setVolumeInstallPath(volume.getInstallPath());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                bus.send(msg, new CloudBusCallBack(cTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            NfsToNfsMigrateBitsReply r = (NfsToNfsMigrateBitsReply) reply;
                            if (r.getDstFilesActualSize() != null) {
                                dstFilesActualSize.putAll(r.getDstFilesActualSize());
                            }
                            logger.info(String.format("Migrated Volume %s from PS %s to PS %s.", volumeUuid, srcPsUuid, dstPsUuid));
                            data.put(MIGRATED, true);
                            cTrigger.next();
                        } else {
                            logger.error(String.format("Failed to migrate Volume %s from PS %s to PS %s.", volumeUuid, srcPsUuid, dstPsUuid));
                            cTrigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "rebase-backing-file-of-qcow2-file";
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                NfsRebaseVolumeBackingFileMsg msg = new NfsRebaseVolumeBackingFileMsg();
                msg.setHostUuid(hostUuid);
                msg.setSrcPsUuid(srcPsUuid);
                msg.setDstPsUuid(dstPsUuid);
                msg.setDstVolumeFolderPath(dstVolumeFolderPath);

                if (data.get(StorageMigrationConstant.DST_IMAGE_CACHE_TEMPLATE_INSTALL_PATH) != null) {
                    String path = (String) data.get(StorageMigrationConstant.DST_IMAGE_CACHE_TEMPLATE_INSTALL_PATH);

                    // image cache install path is xxxx/imagecache/template/09a67204df704b48a64c0626895128c1/{image-uuid}/xxxx.qcow2
                    msg.setDstImageCacheTemplateFolderPath(PathUtil.parentFolder(path));
                }

                msg.setPrimaryStorageUuid(dstPsUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                bus.send(msg, new CloudBusCallBack(cTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info("Rebased backing file for volume " + volumeUuid);
                            cTrigger.next();
                        } else {
                            logger.error("Failed to rebase backing file for volume " + volumeUuid);
                            cTrigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-snapshot-size-after-migration";

            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                Map<String, Long> dstVolumeSnapshotsSize = dstVolumeSnapshotPaths.keySet().stream().collect(Collectors.toMap(uuid -> uuid, uuid -> {
                    String dstSpPath = dstVolumeSnapshotPaths.get(uuid);
                    return dstFilesActualSize.containsKey(dstSpPath) ? dstFilesActualSize.get(dstSpPath) : srcVolumeSnapshotsSize.get(uuid);
                }));
                updateVolumeSnapshotsSize(dstVolumeSnapshotsSize);
                cTrigger.next();
            }
        });

        chain.done(new FlowDoneHandler(trigger) {
            @Override
            public void handle(Map data) {
                trigger.next();
            }
        }).error(new FlowErrorHandler(trigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                trigger.fail(errCode);
            }
        }).start();
    }

    @Transactional
    private void updateVolumeSnapshotsSize(Map<String, Long> snapshotsSize) {
        for (String spUuid : snapshotsSize.keySet()) {
            SQL.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, spUuid)
                    .set(VolumeSnapshotVO_.size, snapshotsSize.get(spUuid))
                    .update();
        }
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        Map<String, Long> srcVolumeSnapshotsSize = (Map<String, Long>) data.get(StorageMigrationConstant.SRC_VOLUME_SNAPSHOTS_SIZE);

        // recover volume status
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        volume.setStatus(VolumeStatus.Ready);
        dbf.update(volume);
        updateVolumeSnapshotsSize(srcVolumeSnapshotsSize);

        if (!data.containsKey(MIGRATED) || !(boolean) data.get(MIGRATED)) {
            trigger.rollback();
            return;
        }

        // delete the migrated snapshots and volume
        String dstVolumeInstallPath = (String) data.get(StorageMigrationConstant.DST_VOLUME_INSTALL_PATH);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-migrated-volume-%s-and-snapshots", dstVolumeInstallPath));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-migrated-volume-and-snapshots";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(dstPsUuid);
                msg.setInstallPath(dstVolumeInstallPath);
                msg.setFolder(true);
                msg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(volume.getFormat()).toString());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info(String.format("Deleted the migrated volume %s", dstVolumeInstallPath));
                            trigger.next();
                        } else {
                            logger.warn(String.format("Failed to delete the unusable volume %s", dstVolumeInstallPath));
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(trigger) {
            @Override
            public void handle(Map data) {
                trigger.rollback();
            }
        }).error(new FlowErrorHandler(trigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                trigger.rollback();
            }
        }).start();
    }

    // be careful with snapshots folder
    private static String getVolumeFolderPath(String srcInstallPath, String srcPsMountPath, String dstPsMountPath) {
        File vol = new File(srcInstallPath);
        if (vol.getParentFile().getName().equals("snapshots")) {
            return vol.getParentFile().getParent().replace(srcPsMountPath, dstPsMountPath);
        } else {
            return vol.getParent().replaceFirst(srcPsMountPath, dstPsMountPath);
        }
    }

    // src and dst volume install path diff only in prim-PSUUID
    private static String getDestInstallPath(String srcInstallPath, String srcPsMountPath, String dstPsMountPath) {
        return srcInstallPath.replace(srcPsMountPath, dstPsMountPath);
    }
}
