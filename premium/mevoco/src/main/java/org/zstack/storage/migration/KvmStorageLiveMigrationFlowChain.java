package org.zstack.storage.migration;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.VolumeTO;
import org.zstack.longjob.LongJobFlowContextHandler;
import org.zstack.storage.migration.primary.DiscardVolumeReferenceFlow;
import org.zstack.storage.migration.primary.PrimaryStorageLiveMigrateVmMsg;
import org.zstack.storage.primary.PrimaryStorageProvisioningStrategyGetter;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.zstack.storage.migration.PrimaryStorageMigrationRecoverHelper.getDstVolumeFromJobContext;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KvmStorageLiveMigrationFlowChain {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMngr;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private StorageTrash trash;

    private static final CLogger logger = Utils.getLogger(KvmStorageLiveMigrationFlowChain.class);

    private final PrimaryStorageLiveMigrateVmMsg msg;
    private final KvmLiveMigrationWorkFlowTemplate.ParamIn paramIn;

    private KvmStorageLiveMigrationFlowChain(final KvmLiveMigrationWorkFlowTemplate.ParamIn paramIn) {
        this.msg = paramIn.getMsg();
        this.paramIn = paramIn;
    }

    static KvmStorageLiveMigrationFlowChain getFlow(final KvmLiveMigrationWorkFlowTemplate.ParamIn paramIn) {
        return new KvmStorageLiveMigrationFlowChain(paramIn);
    }

    private boolean psIsLocal(String psUuid) {
        return Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .isExists();
    }

    public static Map<String, VolumeTO> getMigrateDiskTOList(KVMHostInventory hostInventory,
                                                      Map<String, String> volumeMappingDict,
                                                      List<VolumeVO> oldVolumes,
                                                      List<VolumeInventory> newInitializedVolumes) {
        Map<String, VolumeTO> disks = new HashMap<>();
        Map<String, String> oldInstallPathDict = oldVolumes.stream()
                .collect(Collectors.toMap(ResourceVO::getUuid, VolumeAO::getInstallPath));

        for (VolumeInventory v: newInitializedVolumes) {
            VolumeTO to = VolumeTO.valueOf(v, hostInventory);
            String oldInstallPath = oldInstallPathDict.get(volumeMappingDict.get(v.getUuid()));
            disks.put(oldInstallPath, to);
        }

        return disks;
    }

    public static Map<String, String> getMigrateDisks(Map<String, String> volumeMappingDict,
                                                         List<VolumeVO> oldVolumes,
                                                         List<VolumeInventory> newInitializedVolumes) {
        Map<String, String> disks = new HashMap<>();
        Map<String, String> oldInstallPathDict = oldVolumes.stream()
                .collect(Collectors.toMap(ResourceVO::getUuid, VolumeAO::getInstallPath));

        for (VolumeInventory v : newInitializedVolumes) {
            String oldInstallPath = oldInstallPathDict.get(volumeMappingDict.get(v.getUuid()));
            disks.put(oldInstallPath, v.getUuid());
        }

        return disks;
    }

    private void initMigrationContext(PrimaryStorageMigrateVmContext ctx) {
        ctx.setCreatedVolumes(new ArrayList<>());
        ctx.setInitializedVolumes(new ArrayList<>());
        ctx.setVolumeMappingDict(new HashMap<>());
        ctx.setDstHostInventory(null);
        ctx.setMigrateStarted(false);
        String flowChainName = String.format("kvm-live-storage-migrate-vm-%s-to-ps-%s",
                msg.getVmInstanceUuid(),
                msg.getDstPrimaryStorageUuid());
        if (ctx.getStartedFlowNames() == null) {
            ctx.setStartedFlowNames(new HashMap<>());
        }
        ctx.getStartedFlowNames().put(flowChainName, new ArrayList<>());
        LongJobContextUtil.saveContext(msg.getLongJobUuid(), ctx);
    }

    // 1. Allocate host connected to destination primary storage P if need;
    // 2. Allocate and initialize an empty volume on P for each disks not on P;
    // 3. Perform live block migration.
    // 4. SyncVolumeSize.
    public void run(Completion completion) {
        PrimaryStorageMigrateVmContext ctx = paramIn.getJobContext();
        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        final List<VolumeVO> finalVolumes = paramIn.getVolumesToMigrate();
        if (!ctx.isMigrateStarted()) {
            initMigrationContext(ctx);
        } else {
            // check volume records from PrimaryStorageMigrateVmContext
            DebugUtils.Assert(finalVolumes.stream().allMatch(srcVol -> getDstVolumeFromJobContext(ctx, srcVol.getUuid()) != null), "missing dst volume");
        }

        // A mapping from old & new volume uuid pairs.
        //  - feed old/new volume uuid, returns new/old volume uuid.
        final Map<String, String> volumeMappingDict = ctx.getVolumeMappingDict();
        final Map<String, String> volumeAllocatedUrlDict = new HashMap<>();
        final List<VolumeInventory> createdVolumes = ctx.getCreatedVolumes();
        final List<VolumeInventory> initializedVolumes = ctx.getInitializedVolumes();

        paramIn.setCreatedVolumes(createdVolumes);
        paramIn.setInitializedVolumes(initializedVolumes);
        paramIn.setVolumeMappingDict(volumeMappingDict);
        paramIn.setDstHostInventory(ctx.getDstHostInventory() != null ? ctx.getDstHostInventory() : paramIn.getDstHostInventory());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        String flowChainName = String.format("kvm-live-storage-migrate-vm-%s-to-ps-%s",
                msg.getVmInstanceUuid(),
                msg.getDstPrimaryStorageUuid());
        chain.setName(flowChainName);

        paramIn.getTemplate().getPrepareFlows(paramIn).forEach(chain::then);

        chain.then(new Flow() {
            String __name__ = "allocate-volume-on-ps-" + msg.getDstPrimaryStorageUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // create volume
                final ErrorCode[] err = new ErrorCode[1];
                new While<>(finalVolumes).each((volumeVO, whileCompletion) -> {
                    CreateVolumeMsg cmsg = new CreateVolumeMsg();
                    cmsg.setAccountUuid(acntMngr.getOwnerAccountUuidOfResource(volumeVO.getUuid()));
                    cmsg.setSize(volumeVO.getSize());
                    cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                    cmsg.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    cmsg.setName(volumeVO.getName());
                    cmsg.setVolumeType(volumeVO.getType().toString());
                    String volumeProvisioningStrategy = PrimaryStorageProvisioningStrategyGetter.get(msg.getDstPrimaryStorageUuid());
                    cmsg.setSystemTags(Arrays.asList(
                            VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.instantiateTag(
                                    map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, volumeProvisioningStrategy))
                            )
                    ));

                    bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                    bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                err[0] = reply.getError();
                                whileCompletion.allDone();
                                return;
                            }

                            CreateVolumeReply r = reply.castReply();
                            volumeMappingDict.put(r.getInventory().getUuid(), volumeVO.getUuid());
                            volumeMappingDict.put(volumeVO.getUuid(), r.getInventory().getUuid());
                            createdVolumes.add(r.getInventory());
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (err[0] == null) {
                            trigger.next();
                        } else {
                            trigger.fail(err[0]);
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<DeleteVolumeMsg> dmsgs = createdVolumes.stream()
                        .map(v -> {
                            DeleteVolumeMsg dmsg = new DeleteVolumeMsg();
                            dmsg.setDetachBeforeDeleting(false);
                            dmsg.setUuid(v.getUuid());
                            dmsg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, v.getUuid());
                            return dmsg;
                        }).collect(Collectors.toList());
                if (!dmsgs.isEmpty()) {
                    bus.send(dmsgs);
                }

                trigger.rollback();
            }
        });

        chain.then(new Flow() {
            String __name__ = "allocate-space-for-volume-on-ps-" + msg.getDstPrimaryStorageUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                final ErrorCode[] err = new ErrorCode[1];
                new While<>(createdVolumes).each((volumeInventory, whileCompletion) -> {
                    AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                    amsg.setRequiredPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    amsg.setSize(volumeInventory.getSize());
                    amsg.setRequiredHostUuid(paramIn.getDstHostInventory().getUuid());
                    amsg.setSystemTags(msg.getSystemTags());
                    if (volumeInventory.getType().equals(VolumeType.Root.toString())) {
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateRootVolume.toString());
                    } else {
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
                    }

                    bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                err[0] = reply.getError();
                                whileCompletion.allDone();
                                return;
                            }

                            AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                            volumeAllocatedUrlDict.put(volumeInventory.getUuid(), ar.getAllocatedInstallUrl());
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (err[0] == null) {
                            trigger.next();
                        } else {
                            trigger.fail(err[0]);
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<ReleasePrimaryStorageSpaceMsg> rmsgs = Lists.newArrayList();
                for (Map.Entry<String, String> e : volumeAllocatedUrlDict.entrySet()) {
                    long size = Q.New(VolumeVO.class).select(VolumeVO_.size).eq(VolumeVO_.uuid, e.getKey()).findValue();
                    ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                    rmsg.setAllocatedInstallUrl(e.getValue());
                    rmsg.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    rmsg.setDiskSize(size);
                    bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, msg.getDstPrimaryStorageUuid());
                    rmsgs.add(rmsg);
                }

                if (!rmsgs.isEmpty()) {
                    bus.send(rmsgs);
                }

                trigger.rollback();
            }
        });

        chain.then(new Flow() {
            String __name__ = "instantiate-volume-on-ps-" + msg.getDstPrimaryStorageUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // instantiate volume
                final ErrorCode[] err = new ErrorCode[1];
                new While<>(createdVolumes).each((volumeInventory, whileCompletion) -> {
                    InstantiateTemporaryVolumeOnPrimaryStorageMsg imsg = new InstantiateTemporaryVolumeOnPrimaryStorageMsg();
                    String volTempUuid = volumeInventory.getUuid();
                    imsg.setOriginVolumeUuid(volumeMappingDict.get(volTempUuid));
                    imsg.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    imsg.setVolume(VolumeInventory.valueOf(dbf.findByUuid(volTempUuid, VolumeVO.class)));
                    imsg.setDestHost(paramIn.getDstHostInventory());
                    imsg.setSystemTags(msg.getSystemTags());
                    imsg.setAllocatedInstallUrl(volumeAllocatedUrlDict.get(volTempUuid));

                    bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getDstPrimaryStorageUuid());
                    bus.send(imsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                err[0] = reply.getError();
                                whileCompletion.allDone();
                                return;
                            }

                            InstantiateVolumeOnPrimaryStorageReply r = reply.castReply();
                            if (r.getVolume().getActualSize() == null) {
                                r.getVolume().setActualSize(volumeInventory.getActualSize());
                            }
                            initializedVolumes.add(r.getVolume());
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (err[0] == null) {
                            for (VolumeInventory v : initializedVolumes) {
                                SQL.New(VolumeVO.class)
                                        .eq(VolumeVO_.uuid, v.getUuid())
                                        .set(VolumeVO_.installPath, v.getInstallPath())
                                        .set(VolumeVO_.protocol, v.getProtocol())
                                        .set(VolumeVO_.vmInstanceUuid, null)
                                        .update();
                            }

                            trigger.next();
                        } else {
                            trigger.fail(err[0]);
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<DeleteVolumeOnPrimaryStorageMsg> dmsgs = initializedVolumes.stream()
                        .map(v -> {
                            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                            dmsg.setUuid(msg.getDstPrimaryStorageUuid());
                            dmsg.setVolume(v);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getDstPrimaryStorageUuid());
                            return dmsg;
                        }).collect(Collectors.toList());
                if (!dmsgs.isEmpty()) {
                    bus.send(dmsgs);
                }

                trigger.rollback();
            }
        });

        paramIn.getTemplate().getPreMigrateFlows(paramIn).forEach(chain::then);

        chain.then(new NoRollbackFlow() {
            String __name__ = "prepare-block-migration-for-vm-" + msg.getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(VolumeMigrateExtensionPoint.class)).each((ext, comp) -> {
                    ext.preCopyVolumes(VolumeInventory.valueOf(finalVolumes), initializedVolumes, volumeMappingDict, paramIn.getDstHostInventory().getUuid(), new Completion(comp) {
                        @Override
                        public void success() {
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            comp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        });

        paramIn.getTemplate().getMigrateFlows(paramIn).forEach(chain::then);

        chain.then(new NoRollbackFlow() {
            String __name__ = "after-block-migration-for-vm-" + msg.getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (VolumeMigrateExtensionPoint ext : pluginRgty.getExtensionList(VolumeMigrateExtensionPoint.class)) {
                    ext.afterCopyVolumes(VolumeInventory.valueOf(finalVolumes), vmInstanceVO.getHostUuid());
                }

                trigger.next();
            }
        });

        paramIn.getTemplate().getAfterMigrateFlows(paramIn).forEach(chain::then);
        List<Object> objsToTrash = new ArrayList<>();
        chain.then(new NoRollbackFlow() {
            final String __name__ = "discard-volume-reference-on-vm-" + msg.getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VolumeInventory> volumesToDiscardSrc = VolumeInventory.valueOf(finalVolumes);
                List<VolumeSnapshotVO> volumeSnapshotVOs = Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.volumeUuid,
                        volumesToDiscardSrc.stream().map(VolumeInventory::getUuid).collect(Collectors.toList()))
                        .list();
                Map<String, List<VolumeSnapshotInventory>> volumeSnapshotsToDiscardSrc = volumeSnapshotVOs.stream()
                        .map(VolumeSnapshotInventory::valueOf)
                        .collect(Collectors.groupingBy(VolumeSnapshotInventory::getVolumeUuid));

                for (VolumeInventory vol : volumesToDiscardSrc) {
                    List<VolumeSnapshotInventory> snapshotsToDiscard = volumeSnapshotsToDiscardSrc.get(vol.getUuid());

                    List<Object> objs = DiscardVolumeReferenceFlow.getVolumeResourcesToTrash(vol, snapshotsToDiscard);
                    objsToTrash.addAll(objs);
                    VolumeSnapshotReferenceUtils.handleVolumeDeletion(vol);
                }

                trigger.next();
            }
        });
        chain.then(new Flow() {
            final String __name__ = String.format("trash-old-volume-for-vm-%s", msg.getVmInstanceUuid());
            final List<Long> trashIds = new ArrayList<>();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (Object toTrash : objsToTrash) {
                    if (toTrash instanceof VolumeInventory) {
                        VolumeInventory vol = (VolumeInventory) toTrash;
                        trashIds.add(trash.createTrash(TrashType.MigrateVolume, false, vol).getTrashId());
                        logger.debug(String.format("trash old volume[uuid:%s] for live vm migration", vol.getUuid()));
                    } else if (toTrash instanceof VolumeSnapshotInventory) {
                        VolumeSnapshotInventory snapshot = (VolumeSnapshotInventory) toTrash;
                        trashIds.add(trash.createTrash(TrashType.MigrateVolumeSnapshot, false, snapshot).getTrashId());
                        logger.debug(String.format("trash old volume snapshot[uuid:%s, volumeUuid:%s] for live vm migration",
                                snapshot.getUuid(), snapshot.getVolumeUuid()));
                    }
                }

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trashIds.forEach(trashId -> trash.removeFromDb(trashId));
                trigger.rollback();
            }
        });


        chain.then(new NoRollbackFlow() {
            String __name__ = "update-volume-records-for-vm-" + msg.getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                assert finalVolumes.size() == initializedVolumes.size();

                List<Pair> zipped = IntStream
                        .range(0, finalVolumes.size())
                        .mapToObj(idx -> {
                            VolumeVO srcVol = finalVolumes.get(idx);
                            VolumeInventory destVol = getDstVolumeFromJobContext(ctx, srcVol.getUuid());
                            return new Pair<>(VolumeInventory.valueOf(srcVol), destVol);
                        })
                        .collect(Collectors.toList());

                ErrorCodeList errList = new ErrorCodeList();
                new While<>(zipped).each((pair, whileComp) -> {
                    VolumeInventory oldv = (VolumeInventory) pair.first();
                    VolumeInventory newv = (VolumeInventory) pair.second();

                    if (oldv.getPrimaryStorageUuid().equals(msg.getDstPrimaryStorageUuid()) &&
                            oldv.getInstallPath().equals(newv.getInstallPath())) {
                        logger.info(String.format("volume[%s] has been overwrite, skip it", oldv.getUuid()));
                        trigger.next();
                        return;
                    }
                    OverwriteVolumeMsg amsg = new OverwriteVolumeMsg();
                    amsg.setOriginVolume(oldv);
                    amsg.setTransientVolume(newv);
                    newv.setRootImageUuid(oldv.getRootImageUuid());
                    bus.makeTargetServiceIdByResourceUuid(amsg, VolumeConstant.SERVICE_ID, amsg.getVolumeUuid());
                    bus.send(amsg, new CloudBusCallBack(whileComp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                whileComp.done();
                            } else {
                                errList.getCauses().add(reply.getError());
                                whileComp.allDone();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errList.getCauses().get(0));
                        }
                    }
                });
            }
        });

        chain.then(new NoRollbackFlow() {
            String __name__ = "update-related-db-records-for-vm-" + msg.getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Set<String> oldUuids = finalVolumes.stream().map(VolumeVO::getUuid).collect(Collectors.toSet());
                boolean dstIsLocal = psIsLocal(msg.getDstPrimaryStorageUuid());

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        for (VolumeVO v : finalVolumes) {
                            sql(LocalStorageResourceRefVO.class)
                                    .eq(LocalStorageResourceRefVO_.resourceUuid, v.getUuid())
                                    .eq(LocalStorageResourceRefVO_.primaryStorageUuid, v.getPrimaryStorageUuid())
                                    .hardDelete();

                            if (dstIsLocal) {
                                String tmpVolumeUuid = volumeMappingDict.get(v.getUuid());
                                sql(LocalStorageResourceRefVO.class)
                                        .eq(LocalStorageResourceRefVO_.resourceUuid, tmpVolumeUuid)
                                        .eq(LocalStorageResourceRefVO_.primaryStorageUuid, msg.getDstPrimaryStorageUuid())
                                        .set(LocalStorageResourceRefVO_.resourceUuid, v.getUuid())
                                        .update();
                            }
                        }

                        sql(VmInstanceVO.class)
                                .eq(VmInstanceVO_.uuid, vmInstanceVO.getUuid())
                                .set(VmInstanceVO_.lastHostUuid, vmInstanceVO.getHostUuid())
                                .set(VmInstanceVO_.hostUuid, paramIn.getDstHostInventory().getUuid())
                                .set(VmInstanceVO_.clusterUuid, paramIn.getDstHostInventory().getClusterUuid())
                                .update();

                        // Delete VolumeSnapshot, then VolumeSnapshotTree records.
                        // We can not use DeleteVolumeSnapshotMsg.
                        List<String> snapshotUuids = q(VolumeSnapshotVO.class)
                                .in(VolumeSnapshotVO_.volumeUuid, oldUuids)
                                .select(VolumeSnapshotVO_.uuid)
                                .listValues();

                        if (!snapshotUuids.isEmpty()) {
                            sql(AccountResourceRefVO.class)
                                    .in(AccountResourceRefVO_.resourceUuid, snapshotUuids)
                                    .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                                    .hardDelete();

                            sql(VolumeSnapshotVO.class)
                                    .in(VolumeSnapshotVO_.uuid, snapshotUuids)
                                    .hardDelete();

                            sql(VolumeSnapshotTreeVO.class)
                                    .in(VolumeSnapshotTreeVO_.volumeUuid, oldUuids)
                                    .hardDelete();

                            sql(VolumeSnapshotGroupRefVO.class)
                                    .in(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, snapshotUuids)
                                    .hardDelete();

                            sql(VolumeSnapshotGroupVO.class)
                                    .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                                    .hardDelete();

                            flush();
                        }
                    }
                }.execute();

                trigger.next();
            }
        });

        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-volume-sizes-for-vm-" + msg.getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<SyncVolumeSizeMsg> syncVolumeSizeMsgs = finalVolumes.stream().map(v -> {
                    SyncVolumeSizeMsg syncVolumeSizeMsg = new SyncVolumeSizeMsg();
                    syncVolumeSizeMsg.setVolumeUuid(v.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(syncVolumeSizeMsg, VolumeConstant.SERVICE_ID, v.getUuid());
                    return syncVolumeSizeMsg;
                }).collect(Collectors.toList());
                bus.send(syncVolumeSizeMsgs);

                trigger.next();
            }
        });

        paramIn.getTemplate().getCompleteFlows(paramIn).forEach(chain::then);

        chain.ctxHandler(new LongJobFlowContextHandler(msg.getLongJobUuid()) {
            @Override
            public void saveContext(Flow toRun) {
                // TODO it is necessary to consider the inconsistency between the original flow and the existing flow after the upgrade
                String flowName = toRun.name();
                DebugUtils.Assert(StringUtils.isNotBlank(flowName), "flow must has attr __name__");
                if (!ctx.getStartedFlowNames().get(flowChainName).contains(flowName)) {
                    ctx.getStartedFlowNames().get(flowChainName).add(flowName);
                }
                LongJobContextUtil.saveContext(getJobUuid(), ctx);
            }

            @Override
            public boolean skipFlow(Flow toRun) {
                String flowName = toRun.name();
                List<String> flowNames = ctx.getStartedFlowNames().get(flowChainName);
                boolean inProgress = !flowNames.isEmpty() && flowNames.get(flowNames.size() - 1).equals(flowName);
                return !inProgress && flowNames.contains(flowName);
            }

            @Override
            public boolean cancelled() {
                return !paramIn.isMigrateSucceeded() && super.cancelled();
            }
        });
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                initMigrationContext(ctx);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}
