package org.zstack.storage.migration.primary.ceph;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.*;
import org.zstack.longjob.LongJobUtils;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.SnapInfo;
import org.zstack.storage.ceph.primary.*;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 9/8/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephToCephMigrateVolumeFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(CephToCephMigrateVolumeFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ApiTimeoutManager timeoutMgr;

    private static final String CREATED_VOLUME_PATHS = "created_volume_paths";

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Map<String, String> sysTags = parseSysTags((List<String>) data.get(StorageMigrationConstant.SYSTEM_TAGS));
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        String srcPsUuid = (String) data.get(StorageMigrationConstant.SRC_PS_UUID);
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        List<String> createdVolumePaths = Collections.synchronizedList(new ArrayList<>());
        data.put(CREATED_VOLUME_PATHS, createdVolumePaths);
        CephPrimaryStorageVO dstPsVO = dbf.findByUuid(dstPsUuid, CephPrimaryStorageVO.class);
        CephPrimaryStorageVO srcPsVO = dbf.findByUuid(srcPsUuid, CephPrimaryStorageVO.class);
        boolean isSameCephPrimaryStorage = srcPsUuid.equals(dstPsUuid);

        // update volume status before migration
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        volume.setStatus(VolumeStatus.Migrating);
        dbf.update(volume);

        // get volume size and install path
        long volumeSize = volume.getSize();
        String srcVolumeInstallPath = volume.getInstallPath();

        // get install path of dstVolume and snapshots
        VolumeInventory volumeInv = VolumeInventory.valueOf(volume);
        String srcVolumePoolName = getVolumePoolName(srcPsVO, volumeInv);
        String dstVolumePoolName = getPoolNameFromInstallUrl((String) data.get(StorageMigrationConstant.ALLOCATED_INSTALL_URL));
        String dstVolumeInstallPath = makeDstPrimaryStorageInstallPath(srcVolumeInstallPath, srcVolumePoolName, dstVolumePoolName);
        logger.debug("install path of storage migration destination volume is " + dstVolumeInstallPath);

        List<VolumeSnapshotVO> snapshots = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).list();
        Map<String, String> dstVolumeSnapshotPaths = new HashMap<>();
        for (VolumeSnapshotVO snapshot : snapshots) {
            dstVolumeSnapshotPaths.put(snapshot.getUuid(), makeDstPrimaryStorageInstallPath(snapshot.getPrimaryStorageInstallPath(), srcVolumePoolName, dstVolumePoolName));
        }
        data.put(StorageMigrationConstant.DST_VOLUME_CEPH_POOL_NAME, dstVolumePoolName);
        data.put(StorageMigrationConstant.DST_VOLUME_INSTALL_PATH, dstVolumeInstallPath);
        data.put(StorageMigrationConstant.DST_VOLUME_SNAPSHOT_PATHS, dstVolumeSnapshotPaths);

        // Fixes ZSTAC-20943: new snapshot tree will be created when ReimageVmInstance is called
        Set<String> srcVolumeInstallPaths = new HashSet<>(Collections.singletonList(srcVolumeInstallPath));
        List<String> snapInstallPaths = snapshots.stream().map(s -> s.getPrimaryStorageInstallPath()).collect(Collectors.toList());
        srcVolumeInstallPaths.addAll(snapInstallPaths.stream()
                .filter(v -> !(getPoolNameFromInstallUrl(v).equals(dstVolumePoolName) && isSameCephPrimaryStorage))
                .map(CephToCephMigrateVolumeFlow.this::getVolumeInstallPathOfSnapshot)
                .collect(Collectors.toList()));
        logger.debug(String.format("volume[uuid:%s] install paths: %s", volumeUuid, srcVolumeInstallPaths));
        data.put(StorageMigrationConstant.SRC_VOLUME_INSTALL_PATHS, srcVolumeInstallPaths);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("migrate-volume-%s-from-ps-%s-to-ps-%s", volumeUuid, srcPsUuid, dstPsUuid));
        chain.enableProgressReport();
        chain.preCheck(d -> LongJobUtils.buildErrIfCanceled());
        chain.then(new ShareFlow() {
            List<VolumeSegment> segments = new ArrayList<>();
            boolean dstXsky = false;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-if-install-path-in-trash";
                    @Override
                    public void run(FlowTrigger cTrigger, Map data) {
                        new While<>(srcVolumeInstallPaths).each((srcInstallPath, completion) -> {
                            CheckInstallPathInTrashMsg cmsg = new CheckInstallPathInTrashMsg();
                            cmsg.setPrimaryStorageUuid(dstPsUuid);
                            String dstInstallPath = makeDstPrimaryStorageInstallPath(srcInstallPath, srcVolumePoolName, dstVolumePoolName);
                            cmsg.setInstallPath(dstInstallPath);
                            bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                            bus.send(cmsg, new CloudBusCallBack(cTrigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.addError(Platform.operr(String.format("Failed to check install path of volume %s in ceph ps[uuid:%s].", dstInstallPath, dstPsUuid)));
                                        completion.allDone();
                                        return;
                                    }

                                    CheckInstallPathInTrashReply reply1 = reply.castReply();
                                    if (reply1.getTrashId() == null) {
                                        completion.done();
                                    } else {
                                        completion.addError(Platform.operr(String.format("found trashId(%s) in PrimaryStorage [%s] for the migrate installPath[%s]. Please clean it first by 'APICleanUpTrashOnPrimaryStorageMsg' if you insist to migrate the volume[%s]",
                                                reply1.getTrashId(), dstPsUuid, dstInstallPath, reply1.getResourceUuid())));
                                        completion.allDone();
                                    }
                                }
                            });
                        }).run(new WhileDoneCompletion(cTrigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    cTrigger.fail(errorCodeList.getCauses().get(0));
                                } else {
                                    cTrigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-volume-segment-infos";

                    @Override
                    public void run(FlowTrigger cTrigger, Map data) {
                        new While<>(srcVolumeInstallPaths).each((srcInstallPath, completion) -> {
                            String dstInstallPath = makeDstPrimaryStorageInstallPath(srcInstallPath, srcVolumePoolName, dstVolumePoolName);
                            GetVolumeSnapshotInfoMsg msg = new GetVolumeSnapshotInfoMsg();
                            msg.setVolumePath(srcInstallPath);
                            msg.setPrimaryStorageUuid(srcPsUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, volumeUuid);
                            bus.send(msg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.addError(Platform.operr(String.format("Failed to get snapshot IDs of volume %s in ceph ps[uuid:%s].",
                                                srcInstallPath, srcPsUuid)));
                                        completion.allDone();
                                        return;
                                    }
                                    logger.info(String.format("Got snapshot IDs of volume %s.", srcInstallPath));
                                    GetVolumeSnapshotInfoReply rly = reply.castReply();

                                    List<SnapInfo> snapInfos = rly.getSnapInfos();
                                    if (snapInfos == null || snapInfos.isEmpty()) {
                                        // volume without snapshots
                                        VolumeSegment seg = new VolumeSegment();
                                        seg.parentUuid = "";
                                        seg.resourceUuid = volumeUuid;
                                        seg.exportPath = srcInstallPath;
                                        seg.importPath = dstInstallPath;
                                        seg.noSnaps = true;
                                        segments.add(seg);
                                    } else {
                                        // create segment for every snapshots
                                        Collections.sort(snapInfos);
                                        for (SnapInfo snap : snapInfos) {
                                            VolumeSegment seg = new VolumeSegment();
                                            if (snapInfos.indexOf(snap) == 0) {
                                                seg.parentUuid = "";
                                            } else {
                                                seg.parentUuid = snapInfos.get(snapInfos.indexOf(snap) - 1).getName();
                                            }
                                            seg.resourceUuid = snap.getName();
                                            seg.exportPath = String.format("%s@%s", srcInstallPath, snap.getName());
                                            seg.importPath = dstInstallPath;
                                            seg.size = snap.getSize();
                                            segments.add(seg);
                                        }

                                        // CREATE SEGMENT AFTER LATEST SNAPSHOT
                                        VolumeSegment seg = new VolumeSegment();
                                        seg.parentUuid = snapInfos.get(snapInfos.size() - 1).getName();
                                        seg.resourceUuid = volumeUuid;
                                        seg.exportPath = srcInstallPath;
                                        seg.importPath = dstInstallPath;
                                        segments.add(seg);
                                    }
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(cTrigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    cTrigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }
                                cTrigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-current-size-of-ceph-volume";

                    @Override
                    public void run(FlowTrigger cTrigger, Map data) {
                        Optional<VolumeSegment> op = segments.stream().filter(s -> s.noSnaps).findFirst();
                        if (!op.isPresent()) {
                            cTrigger.next();
                            return;
                        }

                        // if volume has no snapshots, then there must be only one segment to migrate
                        SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                        smsg.setVolumeUuid(volumeUuid);
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, volumeUuid);
                        bus.send(smsg, new CloudBusCallBack(cTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    cTrigger.fail(reply.getError());
                                    return;
                                }

                                op.get().size = ((SyncVolumeSizeReply) reply).getSize();
                                cTrigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "prepare-empty-image-for-importing-diff";

                    @Override
                    public void run(FlowTrigger cTrigger, Map data) {
                        List<String> errors = Collections.synchronizedList(new LinkedList<>());

                        // get size of the first segment of each tree, then create empty volume
                        new While<>(srcVolumeInstallPaths).each((srcInstallPath, completion) -> {
                            String dstInstallPath = makeDstPrimaryStorageInstallPath(srcInstallPath, srcVolumePoolName, dstVolumePoolName);
                            long emptyVolumeSize = 0;
                            Optional<VolumeSegment> op = segments.stream()
                                    .filter(s -> s.parentUuid.equals(""))
                                    .filter(s -> s.exportPath.contains(srcInstallPath))
                                    .findFirst();
                            if (op.isPresent()) {
                                emptyVolumeSize = op.get().size;
                            }

                            CreateEmptyVolumeMsg msg = new CreateEmptyVolumeMsg();
                            msg.setInstallPath(dstInstallPath);
                            msg.setSize(emptyVolumeSize);
                            msg.setShareable(false);
                            // THIS IS VERY IMPORTANT!!!
                            // if volume already exists, and we set skip to true, then old data will LOST when import-diff
                            msg.setSkipIfExisting(false);
                            msg.setPrimaryStorageUuid(dstPsUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                            bus.send(msg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        logger.info("Successfully created empty volume on " + dstInstallPath);
                                        createdVolumePaths.add(dstInstallPath);
                                        CreateEmptyVolumeReply reply1 = reply.castReply();
                                        if (reply1.getSize() > 0) {
                                            dstXsky = reply1.isXsky();
                                        }
                                        completion.done();
                                    } else {
                                        errors.add(String.format("Failed to create empty volume on %s, maybe it already exists.", dstInstallPath));
                                        completion.allDone();
                                    }
                                }
                            });
                        }).run(new WhileDoneCompletion(cTrigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errors.isEmpty()) {
                                    cTrigger.next();
                                } else {
                                    cTrigger.fail(Platform.operr(errors.get(0)));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "migrate-volume-segments-from-ceph-ps-to-ceph-ps";

                    @Override
                    public void run(FlowTrigger cTrigger, Map data) {
                        if (segments.isEmpty()) {
                            cTrigger.next();
                            return;
                        }

                        List<ErrorCode> errors = Collections.synchronizedList(new ArrayList<>());
                        new While<>(segments)
                                .enableProgressReport("migrate-volume-segments-from-ceph-ps-to-ceph-ps")
                                .each((segment, completion) -> {
                            CephToCephMigrateVolumeSegmentMsg msg = new CephToCephMigrateVolumeSegmentMsg();
                            msg.setParentUuid(segment.getParentUuid());
                            msg.setResourceUuid(segment.getResourceUuid());
                            msg.setSrcInstallPath(segment.getExportPath());
                            msg.setDstInstallPath(segment.getImportPath());
                            msg.setPrimaryStorageUuid(srcPsUuid);
                            msg.setDstPrimaryStorageUuid(dstPsUuid);
                            if (segment.getParentUuid().equals("")) {
                                msg.setXsky(dstXsky);
                            }
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, volumeUuid);
                            bus.send(msg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        logger.info(String.format("Migrated volume bits from %s to %s.",
                                                msg.getSrcInstallPath(), msg.getDstInstallPath()));
                                        completion.done();
                                    } else {
                                        errors.add(reply.getError());
                                        logger.error(String.format("Failed to migrate volume bits from %s to %s.",
                                                msg.getSrcInstallPath(), msg.getDstInstallPath()));
                                        completion.allDone();
                                    }
                                }
                            });
                        }).run(new WhileDoneCompletion(cTrigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errors.isEmpty()) {
                                    cTrigger.fail(errors.get(0));
                                    return;
                                }
                                cTrigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "update-volume-ceph-pool-name-tag";

                    @Override
                    public void run(FlowTrigger cTrigger, Map data) {
                        String volumeType = volume.getType().toString();
                        if (volumeType.equals(VolumeType.Root.toString()) &&
                                CephSystemTags.USE_CEPH_ROOT_POOL.hasTag(volumeUuid)) {
                            String srcPoolName = CephSystemTags.USE_CEPH_ROOT_POOL
                                    .getTokenByResourceUuid(volumeUuid, CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN);
                            data.put(StorageMigrationConstant.SRC_VOLUME_CEPH_POOL_NAME, srcPoolName);
                            CephSystemTags.USE_CEPH_ROOT_POOL
                                    .updateTagByToken(volumeUuid, CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN, dstVolumePoolName);
                            logger.debug(String.format("update rootPoolName tag of volume[uuid:%s] to %s", volumeUuid, dstVolumePoolName));
                        }

                        if (volumeType.equals(VolumeType.Data.toString()) &&
                                CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.hasTag(volumeUuid)) {
                            String srcPoolName = CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL
                                    .getTokenByResourceUuid(volumeUuid, CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN);
                            data.put(StorageMigrationConstant.SRC_VOLUME_CEPH_POOL_NAME, srcPoolName);
                            CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL
                                    .updateTagByToken(volumeUuid, CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN, dstVolumePoolName);
                            logger.debug(String.format("update poolName tag of volume[uuid:%s] to %s", volumeUuid, dstVolumePoolName));
                        }

                        cTrigger.next();
                    }
                });
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

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);
        List<String> createdVolumePaths = (List<String>) data.get(CREATED_VOLUME_PATHS);

        // recover volume status
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        volume.setStatus(VolumeStatus.Ready);
        dbf.update(volume);

        // delete the migrated snapshots and volume
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-migrated-volume-%s-during-rollback", volume.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                new While<>(createdVolumePaths).each((dstInstallPath, completion) -> {
                    PurgeSnapshotOnPrimaryStorageMsg msg = new PurgeSnapshotOnPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(dstPsUuid);
                    msg.setVolumePath(dstInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                    bus.send(msg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.info(String.format("Purged the migrated snapshots of volume %s", dstInstallPath));
                            } else {
                                logger.error(String.format("Failed to purge the migrated snapshots of volume %s", dstInstallPath));
                            }
                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(cTrigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        cTrigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                new While<>(createdVolumePaths).each((dstInstallPath, completion) -> {
                    DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(dstPsUuid);
                    msg.setInstallPath(dstInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPsUuid);
                    bus.send(msg, new CloudBusCallBack(cTrigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.info(String.format("Deleted the migrated volume %s", dstInstallPath));
                            } else {
                                logger.warn(String.format("Failed to delete the unusable volume %s", dstInstallPath));
                            }
                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(cTrigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        cTrigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "recover-volume-ceph-pool-name-tag";

            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                String srcPoolName = (String) data.get(StorageMigrationConstant.SRC_VOLUME_CEPH_POOL_NAME);
                if (srcPoolName != null) {
                    CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL
                            .updateTagByToken(volumeUuid, CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN, srcPoolName);
                }
                cTrigger.next();
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

    private Map<String, String> parseSysTags(List<String> systemTags) {
        if (CollectionUtils.isEmpty(systemTags)) {
            return null;
        }
        Map<String, String> sysTags = new HashMap<>();
        systemTags.forEach(tag -> {
            Map<String, String> m = TagUtils.parseIfMatch(CephSystemTags.USE_CEPH_ROOT_POOL.getTagFormat(), tag);
            if (m != null) {
                sysTags.put(VolumeType.Root.toString(), m.get(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN));
                return;
            }
            m = TagUtils.parseIfMatch(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.getTagFormat(), tag);
            if (m != null) {
                sysTags.put(VolumeType.Data.toString(), m.get(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN));
            }
        });
        return sysTags;
    }

    private boolean isVolumeInPoolCheck (VolumeInventory volume, String poolName) {
        String path = String.format("ceph://%s/", poolName);
        return volume.getInstallPath().contains(path);
    }

    private String getPoolNameFromInstallUrl(String alloccatedInstallUrl) {
        String path = alloccatedInstallUrl.replaceFirst("ceph://", "");
        return path.substring(0, path.lastIndexOf("/"));
    }

    // get ps pool name for volume which is in this ps
    private String getVolumePoolName(CephPrimaryStorageVO ps, VolumeInventory volume) {
        List<String> poolNames;
        if (volume.getType().equals(VolumeType.Root.toString())) {
            poolNames = ps.getPools().stream()
                    .filter(pool -> pool.getType().equals(CephPrimaryStoragePoolType.Root.toString()))
                    .filter(pool -> isVolumeInPoolCheck(volume, pool.getPoolName()))
                    .map(CephPrimaryStoragePoolVO::getPoolName).collect(Collectors.toList());
        } else if (volume.getType().equals(VolumeType.Data.toString())) {
            poolNames = ps.getPools().stream()
                    .filter(pool -> pool.getType().equals(CephPrimaryStoragePoolType.Data.toString()))
                    .filter(pool -> isVolumeInPoolCheck(volume, pool.getPoolName()))
                    .map(CephPrimaryStoragePoolVO::getPoolName).collect(Collectors.toList());
        } else {
            throw new OperationFailureException(Platform.operr("The type [%s] of volume is invalid.", volume.getType()));
        }

        DebugUtils.Assert(!poolNames.isEmpty(), String.format("cannot find correct pool from ps: %s", ps.getUuid()));

        // pool name in volume tag first
        if (volume.getType().equals(VolumeType.Root.toString()) && CephSystemTags.USE_CEPH_ROOT_POOL.hasTag(volume.getUuid())) {
            String poolName = CephSystemTags.USE_CEPH_ROOT_POOL.getTokenByResourceUuid(volume.getUuid(), CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN);
            if (!poolName.equals(poolNames.get(0))) {
                logger.warn("the pool name should be the same with volume's USER_CEPH_ROOT_POOL tag");
            };
        }

        if (volume.getType().equals(VolumeType.Data.toString()) && CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.hasTag(volume.getUuid())) {
            String poolName = CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.getTokenByResourceUuid(volume.getUuid(), CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN);
            if (!poolName.equals(poolNames.get(0))) {
                logger.warn("the pool name should be the same with volume's USE_CEPH_PRIMARY_STORAGE_POOL tag");
            };
        }

        return poolNames.get(0);
    }

    private String makeDstPrimaryStorageInstallPath(String installPath, String srcVolumePoolName, String dstVolumePoolName) {
        return installPath.replace(srcVolumePoolName, dstVolumePoolName);
    }

    private String getVolumeInstallPathOfSnapshot(String snapshotInstallPath) {
        if (!snapshotInstallPath.contains("@")) {
            return snapshotInstallPath;
        }
        return snapshotInstallPath.split("@")[0];
    }

    class VolumeSegment {
        String parentUuid;
        String resourceUuid;
        String exportPath;
        String importPath;
        boolean noSnaps;
        Long size;

        public String getParentUuid() {
            return parentUuid;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public String getExportPath() {
            return exportPath;
        }

        public String getImportPath() {
            return importPath;
        }

        public boolean isNoSnaps() {
            return noSnaps;
        }

        public Long getSize() {
            return size;
        }
    }
}
