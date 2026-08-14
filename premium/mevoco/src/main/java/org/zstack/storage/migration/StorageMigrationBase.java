package org.zstack.storage.migration;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.sriov.VfPciDeviceUtils;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.Constants;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageEO;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.longjob.LongJobFlowContextHandler;
import org.zstack.longjob.LongJobManagerImpl;
import org.zstack.longjob.LongJobUtils;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.migration.backup.*;
import org.zstack.storage.migration.primary.*;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 8/31/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StorageMigrationBase implements Component {
    private static final CLogger logger = Utils.getLogger(StorageMigrationBase.class);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected LongJobManagerImpl longJobMgr;

    private Map<String, StorageMigrationChainFactory> factories =
            Collections.synchronizedMap(new HashMap<String, StorageMigrationChainFactory>());

    private Map<String, HypervisorStorageLiveMigrationBackend> slmBackends =
            Collections.synchronizedMap(new HashMap<String, HypervisorStorageLiveMigrationBackend>());

    private Map<String, List<String>> backupStorageBackupStorageMetrics;
    private Map<String, List<String>> primaryStoragePrimaryStorageMetrics;
    private Map<String, List<String>> primaryStorageNotSupportCrossClusterMigrationMetrics;
    private static final List<VmInstanceState> liveStorageVmState = Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused);
    private static final List<VmInstanceState> offlineStorageVmState = Collections.singletonList(VmInstanceState.Stopped);
    private static final List<VmInstanceState> liveMigratingVmStates = Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused, VmInstanceState.Migrating);
    private static final List<VmInstanceState> offlineMigratingVmStates = Arrays.asList(VmInstanceState.Stopped, VmInstanceState.VolumeMigrating);

    public Map<String, List<String>> getBackupStorageBackupStorageMetrics() {
        return backupStorageBackupStorageMetrics;
    }

    public void setBackupStorageBackupStorageMetrics(Map<String, List<String>> backupStorageBackupStorageMetrics) {
        this.backupStorageBackupStorageMetrics = backupStorageBackupStorageMetrics;
    }

    public Map<String, List<String>> getPrimaryStoragePrimaryStorageMetrics() {
        return primaryStoragePrimaryStorageMetrics;
    }

    public void setPrimaryStoragePrimaryStorageMetrics(Map<String, List<String>> primaryStoragePrimaryStorageMetrics) {
        this.primaryStoragePrimaryStorageMetrics = primaryStoragePrimaryStorageMetrics;
    }

    public Map<String, List<String>> getPrimaryStorageNotSupportCrossClusterMigrationMetrics() {
        return primaryStorageNotSupportCrossClusterMigrationMetrics;
    }

    public void setPrimaryStorageNotSupportCrossClusterMigrationMetrics(Map<String, List<String>> primaryStorageNotSupportCrossClusterMigrationMetrics) {
        this.primaryStorageNotSupportCrossClusterMigrationMetrics = primaryStorageNotSupportCrossClusterMigrationMetrics;
    }

    public void handleMessage(StorageMigrationMessage msg) {
        if (msg instanceof APIBackupStorageMigrateImageMsg) {
            handle((APIBackupStorageMigrateImageMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVolumeMsg) {
            handle((APIPrimaryStorageMigrateVolumeMsg) msg);
        } else if ((msg instanceof APIGetBackupStorageCandidatesForImageMigrationMsg)) {
            handle((APIGetBackupStorageCandidatesForImageMigrationMsg) msg);
        } else if ((msg instanceof APIGetPrimaryStorageCandidatesForVolumeMigrationMsg)) {
            handle((APIGetPrimaryStorageCandidatesForVolumeMigrationMsg) msg);
        } else if ((msg instanceof APIPrimaryStorageMigrateVmMsg)) {
            handle((APIPrimaryStorageMigrateVmMsg) msg);
        } else if ((msg instanceof APIGetPrimaryStorageCandidatesForVmMigrationMsg)) {
            handle((APIGetPrimaryStorageCandidatesForVmMigrationMsg) msg);
        } else if ((msg instanceof APIGetHostCandidatesForVmMigrationMsg)) {
            handle((APIGetHostCandidatesForVmMigrationMsg) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    public void handle(APIPrimaryStorageMigrateVmMsg msg) {
        APIPrimaryStorageMigrateVmEvent event = new APIPrimaryStorageMigrateVmEvent(msg.getId());

        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        smsg.setName(msg.getMessageName());
        smsg.setJobName(APIPrimaryStorageMigrateVmMsg.class.getSimpleName());
        smsg.setJobData(JSONObjectUtil.toJsonString(new PrimaryStorageMigrateVmMsg(msg)));
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        longJobMgr.submitLongJob(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }
                SubmitLongJobReply rly = reply.castReply();
                if (!rly.isSuccess()) {
                    event.setError(rly.getError());
                    bus.publish(event);
                }
            }
        }, apiEvent -> {
            if (apiEvent.isSuccess()) {
                event.setInventory(VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class)));
            } else {
                event.setError(apiEvent.getError());
            }
            bus.publish(event);
        });
    }

    private void handle(APIBackupStorageMigrateImageMsg msg) {
        APIBackupStorageMigrateImageEvent evt = new APIBackupStorageMigrateImageEvent(msg.getId());

        MigrateImageOnBackupStorageMsg mmsg = new MigrateImageOnBackupStorageMsg();
        mmsg.setImageUuid(msg.getImageUuid());
        mmsg.setSrcBackupStorageUuid(msg.getSrcBackupStorageUuid());
        mmsg.setDstBackupStorageUuid(msg.getDstBackupStorageUuid());
        mmsg.setType(msg.getType());
        bus.makeLocalServiceId(mmsg, MevocoConstants.SERVICE_ID);

        // queue on source image
        MigrateImageOverlayMsg imsg = new MigrateImageOverlayMsg();
        imsg.setMessage(mmsg);
        imsg.setImageUuid(msg.getImageUuid());
        bus.makeTargetServiceIdByResourceUuid(imsg, ImageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    MigrateImageOnBackupStorageReply rly = reply.castReply();
                    evt.setInventory(rly.getInventory());
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(APIPrimaryStorageMigrateVolumeMsg msg) {
        APIPrimaryStorageMigrateVolumeEvent evt = new APIPrimaryStorageMigrateVolumeEvent(msg.getId());

        PrimaryStorageMigrateVolumeMsg mmsg = new PrimaryStorageMigrateVolumeMsg();
        mmsg.setVolumeUuid(msg.getVolumeUuid());
        mmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        mmsg.setSrcPrimaryStorageUuid(msg.getSrcPrimaryStorageUuid());
        mmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
        mmsg.setType(msg.getType());
        mmsg.setSystemTags(msg.getSystemTags());
        bus.makeLocalServiceId(mmsg, MevocoConstants.SERVICE_ID);
        bus.send(mmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
                PrimaryStorageMigrateVolumeReply rly = (PrimaryStorageMigrateVolumeReply) reply;
                evt.setInventory(rly.getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handleLocalMessage(StorageMigrationMessage msg) {
        if (msg instanceof MigrateVolumeOnPrimaryStorageMsg) {
            handle((MigrateVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MigrateImageOnBackupStorageMsg) {
            handle((MigrateImageOnBackupStorageMsg) msg);
        } else if (msg instanceof PrimaryStorageMigrateVolumeMsg) {
            handle((PrimaryStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof PrimaryStorageCancelMigrateVolumeMsg) {
            handle((PrimaryStorageCancelMigrateVolumeMsg) msg);
        } else if (msg instanceof PrimaryStorageCancelMigrateVmMsg) {
            handle((PrimaryStorageCancelMigrateVmMsg) msg);
        } else if (msg instanceof PrimaryStorageCancelLiveMigrateVmMsg) {
            handle((PrimaryStorageCancelLiveMigrateVmMsg) msg);
        } else if (msg instanceof BackupStorageMigrateImageMsg) {
            handle((BackupStorageMigrateImageMsg) msg);
        } else if (msg instanceof PrimaryStorageOfflineMigrateVmMsg) {
            handle((PrimaryStorageOfflineMigrateVmMsg) msg);
        } else if (msg instanceof PrimaryStorageLiveMigrateVmMsg) {
            handle((PrimaryStorageLiveMigrateVmMsg) msg);
        } else if (msg instanceof RollbackVolumeOnPrimaryStorageMsg) {
            handle((RollbackVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DiscardVolumeDataOnPrimaryStorageMsg) {
            handle((DiscardVolumeDataOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PrimaryStorageMigrateVmMsg) {
            handle((PrimaryStorageMigrateVmMsg) msg);
        }
    }

    private List<VolumeVO> getVolumesToMigrate(VmInstanceVO vmInstanceVO, PrimaryStorageMigrateVmMessage msg) {
        List<VolumeVO> volumes = new ArrayList<>();
        if (!msg.isWithDataVolumes()) {
            volumes.add(vmInstanceVO.getRootVolume());
        } else {
            volumes.addAll(vmInstanceVO.getAllDiskVolumes());
        }

        return volumes.stream().filter(vol -> requireMigration(vol, msg.getDstPrimaryStorageUuid(), msg.getDstHostUuid(), msg.getSystemTags()))
                .collect(Collectors.toList());
    }

    private void handle(PrimaryStorageMigrateVmMsg msg) {
        PrimaryStorageMigrateVmReply oreply = new PrimaryStorageMigrateVmReply();
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        List<VolumeVO> volumesToMigrate = getVolumesToMigrate(vmInstanceVO, msg);
        if (volumesToMigrate.isEmpty()) {
            oreply.setInventory(VmInstanceInventory.valueOf(vmInstanceVO));
            bus.reply(msg, oreply);
            return;
        }

        VmInstanceState vmInstanceState = PrimaryStorageMigrationRecoverHelper.saveAndGetVmStatus(msg);
        PrimaryStorageMigrateVmContext ctx = msg.getJobContext();
        String flowChainName = String.format("migrate-vm-%s-to-ps-%s", msg.getVmInstanceUuid(), msg.getDstPrimaryStorageUuid());
        if (!ctx.isMigrateStarted()) {
            ctx.setStartedFlowNames(new HashMap<>());
            ctx.getStartedFlowNames().put(flowChainName, new ArrayList<>());
        }
        AtomicBoolean migrateSucceeded = new AtomicBoolean(false);
        final String changeVmStateFlow = "change vm state to volumeMigrating";

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(flowChainName);
        chain.enableProgressReport();
        chain.then(new NoRollbackFlow() {
            String __name__ = "call-pre-vm-migration-extension";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(VmPreMigrationExtensionPoint.class))
                    .each((extension, whileCompletion) -> extension.preVmMigration(VmInstanceInventory.valueOf(vmInstanceVO),
                            VmMigrationType.PrimaryStorageMigration, msg.getDstHostUuid(), new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    })).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (errorCodeList.hasError()) {
                                trigger.fail(multiErr(errorCodeList));
                                return;
                            }
                            trigger.next();
                        }
                    });
            }
        }).then(new Flow() {
            String __name__ = changeVmStateFlow;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (vmInstanceVO.getState() == VmInstanceState.Migrating) {
                    // for recovery
                    trigger.next();
                    return;
                }
                // change vm status before storage migration
                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setStateEvent(VmInstanceStateEvent.volumeMigrating.toString());
                cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setStateEvent(vmInstanceState.getDrivenEvent().toString());
                cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh volumes actual size";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ErrorCode> errors = Collections.synchronizedList(new ArrayList<>());
                new While<>(vmInstanceVO.getAllDiskVolumes()).all((volumeVO, whileCompletion) -> {
                    SyncVolumeSizeMsg syncVolumeSizeMsg = new SyncVolumeSizeMsg();
                    syncVolumeSizeMsg.setVolumeUuid(volumeVO.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(syncVolumeSizeMsg, VolumeConstant.SERVICE_ID, syncVolumeSizeMsg.getVolumeUuid());
                    bus.send(syncVolumeSizeMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errors.add(reply.getError());
                                whileCompletion.done();
                                return;
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errors.isEmpty()) {
                            trigger.fail(errors.get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh dst primary storage capacity";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                SyncPrimaryStorageCapacityMsg smsg = new SyncPrimaryStorageCapacityMsg();
                smsg.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, smsg.getPrimaryStorageUuid());
                bus.send(smsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        SyncPrimaryStorageCapacityReply reply1 = reply.castReply();
                        if (!reply1.isSuccess()) {
                            trigger.fail(reply1.getError());
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check capacity after refresh actual size";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                long size = 0;
                for (VolumeVO volumeVO : volumesToMigrate) {
                    size += volumeVO.getActualSize();
                    if (volumeVO.getType() == VolumeType.Data) {
                        continue;
                    }

                    boolean alreadyDownloaded = Q.New(ImageCacheVO.class)
                            .eq(ImageCacheVO_.imageUuid, volumeVO.getRootImageUuid())
                            .eq(ImageCacheVO_.primaryStorageUuid, msg.getDstPrimaryStorageUuid())
                            .isExists();
                    if (!alreadyDownloaded && volumeVO.getRootImageUuid() != null) {
                        ImageEO eo = dbf.findByUuid(volumeVO.getRootImageUuid(), ImageEO.class);
                        size += eo != null ? eo.getActualSize() : 0;
                    }
                }
                PrimaryStorageVO dstPrimaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getDstPrimaryStorageUuid()).find();

                if (size > dstPrimaryStorageVO.getCapacity().getAvailablePhysicalCapacity()) {
                    trigger.fail(operr("there are not enough capacity for vm[uuid: %s] storage migration," +
                                    " required capacity(include image cache): %s, current available physical capacity: %s",
                            msg.getVmInstanceUuid(), size, dstPrimaryStorageVO.getCapacity().getAvailablePhysicalCapacity()));
                    return;
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check vm state and get lock and do migrate";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (liveStorageVmState.contains(vmInstanceState)) {
                    PrimaryStorageLiveMigrateVmMsg pmsg = new PrimaryStorageLiveMigrateVmMsg(msg);
                    bus.makeTargetServiceIdByResourceUuid(pmsg, MevocoConstants.SERVICE_ID, pmsg.getVmInstanceUuid());

                    PrimaryStorageLiveMigrateVmOverlayMsg overlayMsg = new PrimaryStorageLiveMigrateVmOverlayMsg();
                    overlayMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                    overlayMsg.setMessage(pmsg);
                    bus.makeTargetServiceIdByResourceUuid(overlayMsg, VmInstanceConstant.SERVICE_ID, pmsg.getVmInstanceUuid());
                    bus.send(overlayMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                trigger.fail(reply.getError());
                                return;
                            }

                            PrimaryStorageLiveMigrateVmReply reply1 = reply.castReply();
                            oreply.setInventory(reply1.getInventory());
                            migrateSucceeded.set(true);
                            trigger.next();
                        }
                    });
                } else if (offlineStorageVmState.contains(vmInstanceState)) {
                    PrimaryStorageOfflineMigrateVmMsg pmsg = new PrimaryStorageOfflineMigrateVmMsg(msg);
                    bus.makeTargetServiceIdByResourceUuid(pmsg, MevocoConstants.SERVICE_ID, pmsg.getVmInstanceUuid());

                    PrimaryStorageOfflineMigrateVmOverlayMsg overlayMsg = new PrimaryStorageOfflineMigrateVmOverlayMsg();
                    overlayMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                    overlayMsg.setMessage(pmsg);
                    bus.makeTargetServiceIdByResourceUuid(overlayMsg, VmInstanceConstant.SERVICE_ID, pmsg.getVmInstanceUuid());
                    bus.send(overlayMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn("block live migration failed for VM:" + pmsg.getVmInstanceUuid());
                                trigger.fail(reply.getError());
                                return;
                            }

                            PrimaryStorageOfflineMigrateVmReply reply1 = reply.castReply();
                            migrateSucceeded.set(true);
                            oreply.setInventory(reply1.getInventory());
                            trigger.next();
                        }
                    });
                } else {
                    throw new OperationFailureException(Platform.operr(
                            "not support vm state[%s] to do storage migration", vmInstanceVO.getState()));
                }
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change vm state to volumeMigrated";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // change vm status back after storage migration
                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setStateEvent(VmInstanceStateEvent.volumeMigrated.toString());
                cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                // if fail, host ping task will sync it state
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "set-dryrun-cluster-lasthost-for-vm";

            @Override
            public boolean skip(Map data) {
                if (!offlineStorageVmState.contains(vmInstanceVO.getState())) {
                    return true;
                }
                boolean vmClusterAttached = Q.New(PrimaryStorageClusterRefVO.class)
                        .eq(PrimaryStorageClusterRefVO_.clusterUuid, vmInstanceVO.getClusterUuid())
                        .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getDstPrimaryStorageUuid())
                        .isExists();

                return vmClusterAttached;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                setClusterAndLastHost(trigger, vmInstanceVO.getUuid());
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "after-storage-migration-extension-points";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(StorageMigrationExtensionPoint.class), new ForEachFunction<StorageMigrationExtensionPoint>() {
                    @Override
                    public void run(StorageMigrationExtensionPoint arg) {
                        arg.afterStorageMigration(msg, vmInstanceVO);
                    }
                });
                trigger.next();
            }
        });
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
                if (changeVmStateFlow.equals(flowName)) {
                    return false;
                }
                List<String> flowNames = ctx.getStartedFlowNames().get(flowChainName);
                boolean inProgress = !flowNames.isEmpty() && flowNames.get(flowNames.size() - 1).equals(flowName);
                return !inProgress && flowNames.contains(flowName);
            }

            @Override
            public boolean cancelled() {
                return !migrateSucceeded.get() && super.cancelled();
            }
        });
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                VmInstanceVO currentVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                oreply.setInventory(VmInstanceInventory.valueOf(currentVm));
                bus.reply(msg, oreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                oreply.setSuccess(false);
                oreply.setError(errCode);
                bus.reply(msg, oreply);
            }
        }).start();
    }

    private void handle(PrimaryStorageLiveMigrateVmMsg msg) {
        String hvType = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
        if (hvType == null) {
            PrimaryStorageLiveMigrateVmReply reply = new PrimaryStorageLiveMigrateVmReply();
            reply.setError(operr("VM[uuid: %s] not found", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        HypervisorStorageLiveMigrationBackend bkd = getHypervisorLiveStorageMigrationFactory(hvType);
        bkd.handle(msg);
    }

    private boolean requireMigration(VolumeVO volumeVO, String dstPsUuid, String dstHostUuid, List<String> sysTags) {
        String srcPsType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, volumeVO.getPrimaryStorageUuid())
                .select(PrimaryStorageVO_.type).findValue();
        String dstPsType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, dstPsUuid)
                .select(PrimaryStorageVO_.type).findValue();

        return pluginRgty.getExtensionList(VmStorageMigrationMetric.class).stream().anyMatch(ext ->
                ext.isCapable(srcPsType, dstPsType) && ext.needMigration(volumeVO, dstPsUuid, dstHostUuid, sysTags));
    }

    private void handle(PrimaryStorageOfflineMigrateVmMsg msg) {
        PrimaryStorageOfflineMigrateVmReply reply = new PrimaryStorageOfflineMigrateVmReply();
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();

        if (!msg.isWithDataVolumes()) {
            if (!requireMigration(vmInstanceVO.getRootVolume(), msg.getDstPrimaryStorageUuid(), null, msg.getSystemTags())) {
                reply.setInventory(VmInstanceInventory.valueOf(vmInstanceVO));
                bus.reply(msg, reply);
                return;
            }
            MigrateVolumeOnPrimaryStorageMsg mmsg = new MigrateVolumeOnPrimaryStorageMsg();
            mmsg.setSrcPrimaryStorageUuid(vmInstanceVO.getRootVolume().getPrimaryStorageUuid());
            mmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
            mmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
            mmsg.setVolumeUuid(vmInstanceVO.getRootVolume().getUuid());
            mmsg.setType(judgeOfflineStorageMigrationType(vmInstanceVO.getRootVolume().getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
            mmsg.setMigrateWithSnapshots(msg.isWithSnapshots());
            mmsg.setDiscardSourceVolume(true);
            mmsg.setSystemTags(msg.getSystemTags());

            bus.makeTargetServiceIdByResourceUuid(mmsg, MevocoConstants.SERVICE_ID, vmInstanceVO.getRootVolume().getUuid());
            bus.send(mmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply1) {
                    if (!reply1.isSuccess()) {
                        reply.setSuccess(false);
                        reply.setError(reply1.getError());
                        bus.reply(msg, reply);
                        return;
                    }

                    MigrateVolumeOnPrimaryStorageReply mreply = reply1.castReply();
                    if (!mreply.isSuccess()) {
                        reply.setSuccess(false);
                        reply.setError(mreply.getError());
                    } else {
                        VmInstanceVO currentVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                        reply.setInventory(VmInstanceInventory.valueOf(currentVm));
                    }
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        final List<VolumeInventory> toMigrateVols = vmInstanceVO.getAllDiskVolumes().stream()
                .filter(it -> requireMigration(it, msg.getDstPrimaryStorageUuid(), null, msg.getSystemTags()))
                .map(VolumeInventory::valueOf)
                .collect(Collectors.toList());

        final Map<String, Set<String>> volumePathsToTrash = Collections.synchronizedMap(new HashMap<>());
        final Map<String, List<VolumeSnapshotInventory>> srcSnapshots = Collections.synchronizedMap(new HashMap<>());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("offline-migrate-vm-%s-to-ps-%s", msg.getVmInstanceUuid(), msg.getDstPrimaryStorageUuid()));
        chain.preCheck(data -> LongJobUtils.buildErrIfCanceled());
        chain.then(new Flow() {
            String __name__ = "migrate volumes related to vm";
            final List<VolumeInventory> succeededVolumes = new ArrayList<>();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                List<Long> sizes = toMigrateVols.stream().map(VolumeInventory::getActualSize).collect(Collectors.toList());

                new While<>(toMigrateVols)
                        .enableProgressReport("migrate-volumes-on-primary-storage")
                        .all((volumeVO, whileCompletion) -> {
                    srcSnapshots.put(volumeVO.getUuid(), VolumeSnapshotInventory.valueOf(
                            Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeVO.getUuid()).list()
                    ));
                    MigrateVolumeOnPrimaryStorageMsg mmsg = new MigrateVolumeOnPrimaryStorageMsg();
                    mmsg.setSrcPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                    mmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    mmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                    mmsg.setVolumeUuid(volumeVO.getUuid());
                    mmsg.setType(judgeOfflineStorageMigrationType(volumeVO.getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
                    mmsg.setDiscardSourceVolume(false);
                    mmsg.setMigrateWithSnapshots(msg.isWithSnapshots());
                    mmsg.setSystemTags(msg.getSystemTags());
                    bus.makeTargetServiceIdByResourceUuid(mmsg, MevocoConstants.SERVICE_ID, volumeVO.getUuid());

                    bus.send(mmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errList.getCauses().add(reply.getError());
                                whileCompletion.done();
                                return;
                            }

                            MigrateVolumeOnPrimaryStorageReply mreply = reply.castReply();
                            if (!mreply.isSuccess()) {
                                errList.getCauses().add(mreply.getError());
                            } else {
                                volumePathsToTrash.put(volumeVO.getUuid(), mreply.getVolumePathsToTrash());
                                succeededVolumes.add(mreply.getInventory());
                            }
                            whileCompletion.done();
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (succeededVolumes.isEmpty()) {
                    trigger.rollback();
                    return;
                }

                new While<>(succeededVolumes).all((volume, whileCompletion) -> {
                    RollbackVolumeOnPrimaryStorageMsg msg1 = new RollbackVolumeOnPrimaryStorageMsg();
                    msg1.setSrcPrimaryStorageUuid(vmInstanceVO.getAllDiskVolumes().stream()
                            .filter(v -> v.getUuid().equalsIgnoreCase(volume.getUuid()))
                            .findFirst().get().getPrimaryStorageUuid());
                    msg1.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    msg1.setVmInstanceUuid(msg.getVmInstanceUuid());
                    msg1.setVolumeUuid(volume.getUuid());
                    msg1.setType(judgeOfflineStorageMigrationType(volume.getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
                    msg1.setSourceVolumeDiscarded(false);
                    msg1.setDiscardDestinationVolume(true);
                    msg1.setMigrateWithSnapshots(msg.isWithSnapshots());
                    bus.makeTargetServiceIdByResourceUuid(msg1, MevocoConstants.SERVICE_ID, volume.getUuid());
                    bus.send(msg1, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "discard data on source ps";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!msg.isDiscardSourceVolume()) {
                    trigger.next();
                    return;
                }

                new While<>(toMigrateVols).all((vol, whileCompletion) -> {
                    DiscardVolumeDataOnPrimaryStorageMsg dmsg = new DiscardVolumeDataOnPrimaryStorageMsg();
                    dmsg.setSrcVolume(vol);
                    dmsg.setSrcVolumeSnapshots(srcSnapshots.get(vol.getUuid()));
                    dmsg.setSrcVolumeInstallPaths(volumePathsToTrash.get(vol.getUuid()));
                    dmsg.setType(judgeOfflineStorageMigrationType(vol.getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
                    dmsg.setMigrateWithSnapshots(msg.isWithSnapshots());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, MevocoConstants.SERVICE_ID, vol.getUuid());
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("discard volume[%s] on primary storage[%s] failed",
                                        vol.getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
                                whileCompletion.done();
                                return;
                            }

                            DiscardVolumeDataOnPrimaryStorageReply dreply = reply.castReply();
                            if (!dreply.isSuccess()) {
                                logger.warn(String.format("discard volume[%s] on primary storage[%s] failed",
                                        vol.getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                VmInstanceVO currentVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                reply.setInventory(VmInstanceInventory.valueOf(currentVm));
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setSuccess(false);
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private String judgeOfflineStorageMigrationType(String srcPsUuid, String dstPsUuid) {
        PrimaryStorageVO srcPs = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, srcPsUuid).find();
        PrimaryStorageVO dstPs = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, dstPsUuid).find();

        if (srcPs.getType().equalsIgnoreCase(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)) {
            return StorageMigrationConstant.CEPH_TO_CEPH_MIGRATE_VOLUME_TYPE;
        }

        if (srcPs.getType().equalsIgnoreCase(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            return StorageMigrationConstant.NFS_TO_NFS_MIGRATE_VOLUME_TYPE;
        }

        if (srcPs.getType().equalsIgnoreCase(StorageMigrationConstant.SHAREDBLOCK_STORAGE_TYPE)) {
            return StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_MIGRATE_VOLUME_TYPE;
        }

        if (srcPs.getType().equalsIgnoreCase(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
            return StorageMigrationConstant.LOCAL_TO_LOCAL_MIGRATE_VOLUME_TYPE;
        }

        throw new OperationFailureException(Platform.operr(
                "unsupported storage migration type: from %s to %s", srcPs.getType(), dstPs.getType()));
    }

    // local message counterparts of APIBackupStorageMigrateImageMsg
    private void handle(BackupStorageMigrateImageMsg msg) {
        BackupStorageMigrateImageReply mreply = new BackupStorageMigrateImageReply();

        MigrateImageOnBackupStorageMsg mmsg = new MigrateImageOnBackupStorageMsg();
        mmsg.setImageUuid(msg.getImageUuid());
        mmsg.setSrcBackupStorageUuid(msg.getSrcBackupStorageUuid());
        mmsg.setDstBackupStorageUuid(msg.getDstBackupStorageUuid());
        mmsg.setType(msg.getType());
        bus.makeLocalServiceId(mmsg, MevocoConstants.SERVICE_ID);

        // queue on source image
        MigrateImageOverlayMsg imsg = new MigrateImageOverlayMsg();
        imsg.setMessage(mmsg);
        imsg.setImageUuid(msg.getImageUuid());
        bus.makeTargetServiceIdByResourceUuid(imsg, ImageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    MigrateImageOnBackupStorageReply rly = reply.castReply();
                    mreply.setInventory(rly.getInventory());
                    bus.reply(msg, mreply);
                } else {
                    mreply.setError(reply.getError());
                    bus.reply(msg, mreply);
                }
            }
        });
    }

    // local message counterparts of APIPrimaryStorageMigrateVolumeMsg
    private void handle(PrimaryStorageMigrateVolumeMsg msg) {
        PrimaryStorageMigrateVolumeReply mreply = new PrimaryStorageMigrateVolumeReply();
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();

        if (!requireMigration(volumeVO, msg.getDstPrimaryStorageUuid(), null, msg.getSystemTags())) {
            mreply.setInventory(VolumeInventory.valueOf(volumeVO));
            bus.reply(msg, mreply);
            return;
        }

        MigrateVolumeOnPrimaryStorageMsg mmsg = new MigrateVolumeOnPrimaryStorageMsg();
        mmsg.setVolumeUuid(msg.getVolumeUuid());
        mmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        mmsg.setSrcPrimaryStorageUuid(msg.getSrcPrimaryStorageUuid());
        mmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
        mmsg.setType(msg.getType());
        mmsg.setSystemTags(msg.getSystemTags());
        bus.makeTargetServiceIdByResourceUuid(mmsg, MevocoConstants.SERVICE_ID, msg.getVolumeUuid());

        // queue on source vm instance if migrate root volume
        VolumeType volType = Q.New(VolumeVO.class)
                .select(VolumeVO_.type)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .findValue();
        if (volType == VolumeType.Root) {
            String srcVmUuuid = Q.New(VolumeVO.class)
                    .select(VolumeVO_.vmInstanceUuid)
                    .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                    .findValue();
            VmInstanceVO vmvo = dbf.findByUuid(srcVmUuuid, VmInstanceVO.class);

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName(String.format("migrate-root-volume-%s-from-ps-%s-to-ps-%s",
                    msg.getVolumeUuid(), msg.getSrcPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
            chain.then(new Flow() {
                String __name__ = "change-vm-status-before-storage-migration";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    // change vm status before storage migration
                    ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                    cmsg.setStateEvent(VmInstanceStateEvent.volumeMigrating.toString());
                    cmsg.setVmInstanceUuid(srcVmUuuid);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, srcVmUuuid);
                    bus.send(cmsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    // change vm status back to stopped
                    ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                    cmsg.setStateEvent(VmInstanceStateEvent.stopped.toString());
                    cmsg.setVmInstanceUuid(srcVmUuuid);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, srcVmUuuid);
                    bus.send(cmsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            trigger.rollback();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "send-storage-migrate-message";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    MigrateRootVolumeOverlayMsg rmsg = new MigrateRootVolumeOverlayMsg();
                    rmsg.setMessage(mmsg);
                    rmsg.setVmInstanceUuid(srcVmUuuid);
                    bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, srcVmUuuid);
                    bus.send(rmsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                MigrateVolumeOnPrimaryStorageReply rly = reply.castReply();
                                mreply.setInventory(rly.getInventory());
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "change-vm-status-after-storage-migration";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    // change vm status back after storage migration
                    ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                    cmsg.setStateEvent(VmInstanceStateEvent.volumeMigrated.toString());
                    cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                    // if fail, host ping task will sync it state
                    bus.send(cmsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            trigger.next();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "set-dryrun-cluster-lasthost-for-vm";

                @Override
                public boolean skip(Map data) {
                    boolean vmClusterAttached = Q.New(PrimaryStorageClusterRefVO.class)
                            .eq(PrimaryStorageClusterRefVO_.clusterUuid, vmvo.getClusterUuid())
                            .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getDstPrimaryStorageUuid())
                            .isExists();

                    return vmClusterAttached;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    setClusterAndLastHost(trigger, vmvo.getUuid());
                }
            });

            chain.done(new FlowDoneHandler(msg) {
                @Override
                public void handle(Map data) {
                    bus.reply(msg, mreply);
                }
            }).error(new FlowErrorHandler(msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    mreply.setError(errCode);
                    bus.reply(msg, mreply);
                }
            }).start();
        } else if (volType == VolumeType.Memory) {
            volumeVO.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
            volumeVO = dbf.updateAndRefresh(volumeVO);
            mreply.setInventory(VolumeInventory.valueOf(volumeVO));
            bus.reply(msg, mreply);
        } else {
            // queue on source data volume
            MigrateDataVolumeOverlayMsg dmsg = new MigrateDataVolumeOverlayMsg();
            dmsg.setMessage(mmsg);
            dmsg.setVolumeUuid(msg.getVolumeUuid());
            bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
            bus.send(dmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        MigrateVolumeOnPrimaryStorageReply rly = reply.castReply();
                        mreply.setInventory(rly.getInventory());
                        bus.reply(msg, mreply);
                    } else {
                        mreply.setError(reply.getError());
                        bus.reply(msg, mreply);
                    }
                }
            });
        }
    }

    private void setClusterAndLastHost(FlowTrigger trigger, String vmUuid) {
        final GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
        gmsg.setUuid(vmUuid);
        bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    SQL.New(VmInstanceVO.class)
                            .set(VmInstanceVO_.clusterUuid, null)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .update();
                    trigger.next();
                    return;
                }
                GetVmStartingCandidateClustersHostsReply greply = (GetVmStartingCandidateClustersHostsReply) re;

                if (greply.getHostInventories().isEmpty()) {
                    trigger.next();
                    return;
                }

                HostInventory hostInventory = greply.getHostInventories().get(0);

                SQL.New(VmInstanceVO.class)
                        .set(VmInstanceVO_.clusterUuid, hostInventory.getClusterUuid())
                        .set(VmInstanceVO_.lastHostUuid, hostInventory.getUuid())
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .update();
                trigger.next();
            }
        });
    }

    private void handle(PrimaryStorageCancelMigrateVolumeMsg msg) {
        VolumeVO volumeVO = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        PrimaryStorageCancelMigrateVolumeReply reply = new PrimaryStorageCancelMigrateVolumeReply();
        if (!requireMigration(volumeVO, msg.getDstPrimaryStorageUuid(), null, msg.getSystemTags())) {
            reply.setError(LongJobUtils.noncancelableErr(String.format(
                    "volume[uuid:%s] migrate[dstPsUuid:%s] has succeeded, do not cancel it",
                    msg.getVolumeUuid(), msg.getDstPrimaryStorageUuid()
            )));
            bus.reply(msg, reply);
            return;
        }

        StorageMigrationChainFactory factory =
                getStorageMigrationChainFactory(StorageMigrationChainType.valueOf(msg.getType()));
        FlowChain chain = factory.getCancelStorageMigrationFlowChain();
        if (chain == null) {
            reply.setError(operr("not support to cancel %s", msg.getType()));
            bus.reply(msg, reply);
            return;
        }

        chain.getData().put(StorageMigrationConstant.VOLUME_UUID, msg.getVolumeUuid());
        chain.getData().put(StorageMigrationConstant.SRC_PS_UUID, msg.getSrcPrimaryStorageUuid());
        chain.getData().put(Constants.THREAD_CONTEXT_API, msg.getCancellationApiId());
        chain.getData().put(StorageMigrationConstant.WITH_SNAPSHOTS, msg.isMigrateWithSnapshots());
        chain.setName("cancel migrate volume");
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(PrimaryStorageCancelLiveMigrateVmMsg msg) {
        String hvType = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
        if (hvType == null) {
            PrimaryStorageLiveMigrateVmReply reply = new PrimaryStorageLiveMigrateVmReply();
            reply.setError(operr("VM[uuid: %s] not found", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        HypervisorStorageLiveMigrationBackend bkd = getHypervisorLiveStorageMigrationFactory(hvType);
        bkd.handle(msg);
    }

    private void handle(PrimaryStorageCancelMigrateVmMsg msg) {
        PrimaryStorageCancelMigrateVmReply reply = new PrimaryStorageCancelMigrateVmReply();

        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        List<VolumeVO> volumesToMigrate = getVolumesToMigrate(vmInstanceVO, msg);
        if (volumesToMigrate.isEmpty()) {
            reply.setError(LongJobUtils.noncancelableErr(String.format("vm[uuid:%s] has been migrated, do not cancel it.",
                    msg.getVmInstanceUuid())));
            bus.reply(msg, reply);
            return;
        }

        if (liveMigratingVmStates.contains(vmInstanceVO.getState())) {
            PrimaryStorageCancelLiveMigrateVmMsg cmsg = new PrimaryStorageCancelLiveMigrateVmMsg();
            cmsg.setCancellationApiId(msg.getCancellationApiId());
            cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
            cmsg.setSystemTags(msg.getSystemTags());
            bus.makeLocalServiceId(cmsg, MevocoConstants.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply messageReply) {
                    if (!messageReply.isSuccess()) {
                        reply.setError(LongJobUtils.noncancelableErr(messageReply.getError().getDetails()));
                    }
                    bus.reply(msg, reply);
                }
            });
        } else if (offlineMigratingVmStates.contains(vmInstanceVO.getState())) {
            new While<>(volumesToMigrate).all((vol, compl) -> {
                PrimaryStorageCancelMigrateVolumeMsg cmsg = new PrimaryStorageCancelMigrateVolumeMsg();
                cmsg.setCancellationApiId(msg.getCancellationApiId());
                cmsg.setSrcPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                cmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                cmsg.setVolumeUuid(vol.getUuid());
                cmsg.setSystemTags(msg.getSystemTags());
                cmsg.setType(judgeOfflineStorageMigrationType(vol.getPrimaryStorageUuid(), msg.getDstPrimaryStorageUuid()));
                cmsg.setMigrateWithSnapshots(msg.isWithSnapshots());
                bus.makeLocalServiceId(cmsg, MevocoConstants.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(compl) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            compl.addError(reply.getError());
                        }
                        compl.done();
                    }
                });
            }).run(new WhileDoneCompletion(msg) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (errorCodeList.getCauses().size() == volumesToMigrate.size()) {
                        reply.setError(LongJobUtils.noncancelableErr(String.format(
                                "fail to cancel vm[uuid:%s] primary storage migration",
                                msg.getVmInstanceUuid()), errorCodeList.getCauses()));
                    }
                    bus.reply(msg, reply);
                }
            });
        } else {
            throw new OperationFailureException(Platform.operr(
                    "not support vm state[%s] to do cancellation of storage migration", vmInstanceVO.getState()));
        }
    }

    private void handle(MigrateImageOnBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("migrate-image-%s", msg.getImageUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                migrateImage(msg, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }


    private void migrateImage(MigrateImageOnBackupStorageMsg msg, NoErrorCompletion completion) {
        MigrateImageOnBackupStorageReply reply = new MigrateImageOnBackupStorageReply();

        StorageMigrationChainFactory factory =
                getStorageMigrationChainFactory(StorageMigrationChainType.valueOf(msg.getType()));
        FlowChain chain = factory.getStorageMigrationFlowChain();
        chain.enableProgressReport();
        chain.setName("migrate-image-from-bs-to-bs");
        chain.getData().put(StorageMigrationConstant.IMAGE_UUID, msg.getImageUuid());
        chain.getData().put(StorageMigrationConstant.SRC_BS_UUID, msg.getSrcBackupStorageUuid());
        chain.getData().put(StorageMigrationConstant.DST_BS_UUID, msg.getDstBackupStorageUuid());
        logger.info("Created FlowChain for BackupStorageImageMigration.");

        if (chain != null && !chain.getFlows().isEmpty()) {
            chain.done(new FlowDoneHandler(msg) {
                @Override
                public void handle(Map data) {
                    String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);
                    ImageVO dstImageVO = dbf.findByUuid(imageUuid, ImageVO.class);
                    ImageInventory dstImageInv = ImageInventory.valueOf(dstImageVO);
                    reply.setInventory(dstImageInv);
                    bus.reply(msg, reply);
                }
            }).error(new FlowErrorHandler(msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    reply.setError(errCode);
                    bus.reply(msg, reply);
                }
            }).Finally(new FlowFinallyHandler(completion) {
                @Override
                public void Finally() {
                    completion.done();
                }
            }).start();
        } else {
            throw new CloudRuntimeException("No appropriate FlowChain found to migrate image.");
        }
    }

    private void handle(RollbackVolumeOnPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("rollback-vm-%s", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                rollbackVolume(msg, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(MigrateVolumeOnPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("migrate-volume-%s", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                migrateVolume(msg, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(DiscardVolumeDataOnPrimaryStorageMsg msg) {
        DiscardVolumeDataOnPrimaryStorageReply reply = new DiscardVolumeDataOnPrimaryStorageReply();

        StorageMigrationChainFactory factory =
                getStorageMigrationChainFactory(StorageMigrationChainType.valueOf(msg.getType()));
        FlowChain chain = factory.getStorageMigrationDiscardFlowChain();
        chain.setName(String.format("discard-volume-from-ps-%s", msg.getSrcVolume().getPrimaryStorageUuid()));
        chain.getData().put(StorageMigrationConstant.WITH_SNAPSHOTS, msg.isMigrateWithSnapshots());
        chain.getData().put(StorageMigrationConstant.DISCARD_SOURCE, true);
        chain.getData().put(StorageMigrationConstant.SRC_VOLUME, msg.getSrcVolume());
        chain.getData().put(StorageMigrationConstant.SRC_VOLUME_SNAPSHOTS, msg.getSrcVolumeSnapshots());
        chain.getData().put(StorageMigrationConstant.SRC_VOLUME_INSTALL_PATHS, msg.getSrcVolumeInstallPaths());

        logger.info("Created DiscardFlowChain for PrimaryStorageVolumeMigration.");

        if (chain != null && !chain.getFlows().isEmpty()) {
            chain.done(new FlowDoneHandler(msg) {
                @Override
                public void handle(Map data) {
                    bus.reply(msg, reply);
                }
            }).error(new FlowErrorHandler(msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    reply.setError(errCode);
                    bus.reply(msg, reply);
                }
            }).start();
        } else {
            throw new CloudRuntimeException("No appropriate FlowChain found to discard migrate volume.");
        }
    }

    private void rollbackVolume(RollbackVolumeOnPrimaryStorageMsg msg, NoErrorCompletion completion) {
        RollbackVolumeOnPrimaryStorageReply reply = new RollbackVolumeOnPrimaryStorageReply();
        StorageMigrationChainFactory factory =
                getStorageMigrationChainFactory(StorageMigrationChainType.valueOf(msg.getType()));
        FlowChain chain = factory.getStorageMigrationRollbackFlowChain();
        chain.setName("rollback-migrate-volume-from-ps-to-ps");
        chain.getData().put(StorageMigrationConstant.VOLUME_UUID, msg.getVolumeUuid());
        chain.getData().put(StorageMigrationConstant.SRC_PS_UUID, msg.getSrcPrimaryStorageUuid());
        chain.getData().put(StorageMigrationConstant.DST_PS_UUID, msg.getDstPrimaryStorageUuid());
        chain.getData().put(StorageMigrationConstant.WITH_SNAPSHOTS, msg.isMigrateWithSnapshots());
        chain.getData().put(StorageMigrationConstant.DISCARD_DESTINATION, msg.isDiscardDestinationVolume());

        // NOTE(weiw): if source discarded, we need no migrate data instead of only change in db.
        // As for now, we only support rollback when source not discarded
        chain.getData().put(StorageMigrationConstant.DISCARD_SOURCE, msg.isSourceVolumeDiscarded());
        logger.info(String.format("Created RollbackFlowChain for PrimaryStorageVolumeMigration %s", chain.getFlows()));

        if (chain != null && !chain.getFlows().isEmpty()) {
            chain.done(new FlowDoneHandler(msg) {
                @Override
                public void handle(Map data) {
                    bus.reply(msg, reply);
                }
            }).error(new FlowErrorHandler(msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    reply.setError(errCode);
                    bus.reply(msg, reply);
                }
            }).Finally(new FlowFinallyHandler(msg, completion) {
                @Override
                public void Finally() {
                    completion.done();
                }
            }).start();
        } else {
            throw new CloudRuntimeException("No appropriate FlowChain found to rollback migrate volume.");
        }
    }

    private void migrateVolume(MigrateVolumeOnPrimaryStorageMsg msg, NoErrorCompletion completion) {
        MigrateVolumeOnPrimaryStorageReply reply = new MigrateVolumeOnPrimaryStorageReply();

        StorageMigrationChainFactory factory =
                getStorageMigrationChainFactory(StorageMigrationChainType.valueOf(msg.getType()));
        FlowChain chain = factory.getStorageMigrationFlowChain();
        DebugUtils.Assert(chain != null, "StorageMigrationChainFactory.getStorageMigrationFlowChain is null in xmls");
        chain.setName("migrate-volume-from-ps-to-ps");
        chain.getData().put(StorageMigrationConstant.VOLUME_UUID, msg.getVolumeUuid());
        chain.getData().put(StorageMigrationConstant.SRC_PS_UUID, msg.getSrcPrimaryStorageUuid());
        chain.getData().put(StorageMigrationConstant.DST_PS_UUID, msg.getDstPrimaryStorageUuid());
        chain.getData().put(StorageMigrationConstant.WITH_SNAPSHOTS, msg.isMigrateWithSnapshots());
        chain.getData().put(StorageMigrationConstant.DISCARD_SOURCE, msg.isDiscardSourceVolume());
        chain.getData().put(StorageMigrationConstant.SYSTEM_TAGS, msg.getSystemTags());
        chain.getData().put(StorageMigrationConstant.INDEPENDENT_PATH, VolumeSnapshotReferenceUtils.getVolumeInstallUrlBackingOtherVolume(msg.getVolumeUuid()));
        logger.info("Created FlowChain for PrimaryStorageVolumeMigration.");

        if (!chain.getFlows().isEmpty()) {
            chain.done(new FlowDoneHandler(msg) {
                @Override
                public void handle(Map data) {
                    String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
                    VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
                    VolumeInventory volumeInv = VolumeInventory.valueOf(volume);
                    reply.setInventory(volumeInv);
                    reply.setVolumePathsToTrash((Set<String>) data.get(StorageMigrationConstant.SRC_VOLUME_INSTALL_PATHS));
                    bus.reply(msg, reply);
                }
            }).error(new FlowErrorHandler(msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    reply.setError(errCode);
                    bus.reply(msg, reply);
                }
            }).Finally(new FlowFinallyHandler(msg, completion) {
                @Override
                public void Finally() {
                    completion.done();
                }
            }).start();
        } else {
            throw new CloudRuntimeException("No appropriate FlowChain found to migrate root volume.");
        }
    }

    private void handle(APIGetBackupStorageCandidatesForImageMigrationMsg msg) {
        APIGetBackupStorageCandidatesForImageMigrationReply reply = new APIGetBackupStorageCandidatesForImageMigrationReply();
        List<BackupStorageInventory> candidates = getBackupStorageCandidatesForImageMigration(msg.getSrcBackupStorageUuid());
        reply.setInventories(candidates);
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageCandidatesForVolumeMigrationMsg msg) {
        APIGetPrimaryStorageCandidatesForVolumeMigrationReply reply = new APIGetPrimaryStorageCandidatesForVolumeMigrationReply();
        String srcPsUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).select(VolumeVO_.primaryStorageUuid).findValue();
        String psType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, srcPsUuid).select(PrimaryStorageVO_.type).findValue();
        List<PrimaryStorageInventory> candidates = getPrimaryStorageCandidatesForVolumeMigration(false, msg.getVolumeUuid(), true);

        boolean removeItSelf = true;
        removeItSelf &= !pluginRgty.getExtensionList(VmStorageMigrationMetric.class).stream().anyMatch(ext ->
                ext.isCapable(psType, psType) && ext.isSupportSameStorage());
        if (removeItSelf) {
            candidates.removeIf(it -> it.getUuid().equals(srcPsUuid));
        }
        reply.setInventories(candidates);
        bus.reply(msg, reply);
    }

    private void handle(APIGetHostCandidatesForVmMigrationMsg msg) {
        APIGetHostCandidatesForVmMigrationReply hostCandidatesReply = new APIGetHostCandidatesForVmMigrationReply();
        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        long size = LiveMigrationFunction.getVolumeSizeToMigrate(vmInstanceVO, msg.getDstPrimaryStorageUuid());

        DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();
        amsg.setVmInstance(VmInstanceInventory.valueOf(vmInstanceVO));
        amsg.setL3NetworkUuids(VmNicHelper.getL3Uuids(VmNicInventory.valueOf(vmInstanceVO.getVmNics())));
        amsg.setVmNicParams(VmNicHelper.getVmNicParamsWithVfNic(vmInstanceVO));
        amsg.setRequiredPrimaryStorageUuids(Collections.singleton(msg.getDstPrimaryStorageUuid()));
        amsg.addLocationSpec(VmLocationSpec.avoidHost(vmInstanceVO.getHostUuid()));
        amsg.setCpuCapacity(vmInstanceVO.getCpuNum());
        amsg.setMemoryCapacity(vmInstanceVO.getMemorySize());
        amsg.setDiskSize(size);
        amsg.setVmOperation(VmInstanceConstant.VmOperation.NewCreate.toString());
        amsg.setAllocatorStrategy(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE);
        amsg.setArchitecture(vmInstanceVO.getArchitecture());
        amsg.setZoneUuid(vmInstanceVO.getZoneUuid());
        amsg.setDryRun(true);

        amsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateHostDryRunReply r = reply.castReply();
                    if (msg.getLimit() != null && r.getHosts().size() > msg.getLimit()) {
                        hostCandidatesReply.setInventories(r.getHosts().subList(0, msg.getLimit()));
                    } else {
                        hostCandidatesReply.setInventories(r.getHosts());
                    }
                } else {
                    hostCandidatesReply.setSuccess(false);
                    hostCandidatesReply.setError(operr("failed to get host candidates for vm migration"));
                }
                bus.reply(msg, hostCandidatesReply);
            }
        });
    }

    private void handle(APIGetPrimaryStorageCandidatesForVmMigrationMsg msg) {
        APIGetPrimaryStorageCandidatesForVmMigrationReply reply = new APIGetPrimaryStorageCandidatesForVmMigrationReply();
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        final boolean live = liveStorageVmState.contains(vmInstanceVO.getState());
        List<PrimaryStorageInventory> candidates = getPrimaryStorageCandidatesForVolumeMigration(live, vmInstanceVO.getRootVolumeUuid(), msg.isMigrateStorageOnly());
        logger.debug(String.format("get candidates %s for volume %s",
                candidates.parallelStream()
                        .map(PrimaryStorageInventory::getUuid)
                        .collect(Collectors.toList()),
                vmInstanceVO.getRootVolumeUuid()));

        for (VolumeVO volumeVO : vmInstanceVO.getAllDiskVolumes()) {
            if (!msg.isWithDataVolumes() && volumeVO.getType() == VolumeType.Data) {
                continue;
            }
            List<String> volCandidates = getPrimaryStorageCandidatesForVolumeMigration(live, volumeVO.getUuid(), msg.isMigrateStorageOnly()).stream()
                    .map(PrimaryStorageInventory::getUuid).collect(Collectors.toList());
            logger.debug(String.format("get candidates %s for volume %s",
                    volCandidates, volumeVO.getUuid()));

            candidates = candidates.parallelStream()
                    .filter(c -> volCandidates.contains(c.getUuid())).collect(Collectors.toList());
        }

        // remove srcPsUuid
        String psType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, vmInstanceVO.getRootVolume().getPrimaryStorageUuid()).select(PrimaryStorageVO_.type).findValue();

        boolean removeItSelf = vmInstanceVO.getAllDiskVolumes().stream().map(VolumeAO::getPrimaryStorageUuid).distinct().count() == 1 || !msg.isWithDataVolumes();
        removeItSelf &= !pluginRgty.getExtensionList(VmStorageMigrationMetric.class).stream().anyMatch(ext ->
                ext.isCapable(psType, psType) && ext.isSupportSameStorage());

        if (removeItSelf) {
            candidates.removeIf(it -> it.getUuid().equals(vmInstanceVO.getRootVolume().getPrimaryStorageUuid()));
        }
        reply.setInventories(candidates);
        bus.reply(msg, reply);
    }

    @Override
    public boolean start() {
        populateStorageMigrationChainFactory();
        populateLiveStorageMigrationFactory();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateStorageMigrationChainFactory() {
        for (StorageMigrationChainFactory ext : pluginRgty.getExtensionList(StorageMigrationChainFactory.class)) {
            StorageMigrationChainFactory old = factories.get(ext.getStorageMigrationChainType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("Duplicate StorageMigrationChainFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getStorageMigrationChainType()));
            }
            factories.put(ext.getStorageMigrationChainType().toString(), ext);
        }
    }

    private void populateLiveStorageMigrationFactory() {
        for (HypervisorStorageLiveMigrationBackend ext : pluginRgty.getExtensionList(HypervisorStorageLiveMigrationBackend.class)) {
            HypervisorStorageLiveMigrationBackend old = slmBackends.get(ext.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("Duplicate HypervisorLiveStorageMigrationFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHypervisorType()));
            }
            slmBackends.put(ext.getHypervisorType(), ext);
        }
    }

    private StorageMigrationChainFactory getStorageMigrationChainFactory(StorageMigrationChainType type) {
        StorageMigrationChainFactory factory = factories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(
                    String.format("Unable to find StorageMigrationChainFactory with type[%s]", type)
            );
        }
        return factory;
    }

    private HypervisorStorageLiveMigrationBackend getHypervisorLiveStorageMigrationFactory(String hvType) {
        HypervisorStorageLiveMigrationBackend bkd = slmBackends.get(hvType);
        if (bkd == null) {
            throw new CloudRuntimeException(
                    String.format("Unable to find HypervisorStorageLiveMigrationFactory with type[%s]", hvType)
            );
        }

        return bkd;
    }

    private List<BackupStorageInventory> getBackupStorageCandidatesForImageMigration(String srcBsUuid) {
        BackupStorageVO srcBs = dbf.findByUuid(srcBsUuid, BackupStorageVO.class);

        List<BackupStorageVO> candidates = Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.state, BackupStorageState.Enabled)
                .eq(BackupStorageVO_.status, BackupStorageStatus.Connected)
                .notEq(BackupStorageVO_.uuid, srcBsUuid)
                .list();

        for (BackupStorageVO candidate : new ArrayList<>(candidates)) {
            String srcBsType = srcBs.getType();
            String dstBsType = candidate.getType();
            List<String> types = backupStorageBackupStorageMetrics.get(srcBsType);
            if (types == null || !types.contains(dstBsType)) {
                candidates.remove(candidate);
            }
        }
        return BackupStorageInventory.valueOf(candidates);
    }

    private List<PrimaryStorageInventory> getPrimaryStorageCandidatesForVolumeMigration(boolean live, String srcVolumeUuid, boolean migrateStorageOnly) {
        VolumeVO srcVolume = dbf.findByUuid(srcVolumeUuid, VolumeVO.class);

        String srcPsUuid = srcVolume.getPrimaryStorageUuid();
        PrimaryStorageVO srcPs = dbf.findByUuid(srcPsUuid, PrimaryStorageVO.class);

        List<PrimaryStorageVO> candidates = Q.New(PrimaryStorageVO.class)
                .notEq(PrimaryStorageVO_.state, PrimaryStorageState.Disabled)
                .notEq(PrimaryStorageVO_.state, PrimaryStorageState.Maintenance)
                .notEq(PrimaryStorageVO_.status, PrimaryStorageStatus.Disconnected)
                .list();

        if (live && migrateStorageOnly) {
            List<String> psUuids = SQL.New("select ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref where ref.clusterUuid = (" +
                    "select h.clusterUuid from HostVO h where h.uuid = " +
                    "(select hostUuid from VmInstanceVO where uuid = :vmuuid))").param("vmuuid", srcVolume.getVmInstanceUuid()).list();
            candidates = candidates.parallelStream().filter(it -> psUuids.contains(it.getUuid())).collect(Collectors.toList());
            return PrimaryStorageInventory.valueOf(candidates);
        }

        //remove ps that attached cluster is not matched
        if (VolumeType.Root.toString().equals(srcVolume.getType().toString())) {
            String architecture = dbf.findByUuid(srcVolume.getVmInstanceUuid(), VmInstanceVO.class).getArchitecture();
            if (architecture != null) {
                List<String> cs = Q.New(ClusterVO.class).select(ClusterVO_.uuid).eq(ClusterVO_.architecture, architecture).listValues();
                List<String> withArchPsUuids = Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.primaryStorageUuid).in(PrimaryStorageClusterRefVO_.clusterUuid, cs).listValues();
                candidates = candidates.parallelStream().filter(it -> withArchPsUuids.contains(it.getUuid())).collect(Collectors.toList());
            }
        }

        if (!live) {
            //Get all psUuid in the cluster where srcPs is located
            List<String> psUuids = SQL.New("select primaryStorageUuid from PrimaryStorageClusterRefVO where" +
                    " clusterUuid in (select clusterUuid from PrimaryStorageClusterRefVO where" +
                    " primaryStorageUuid=:primaryStorageUuid)")
                    .param("primaryStorageUuid", srcPsUuid)
                    .list();

            for (PrimaryStorageVO candidate : new ArrayList<>(candidates)) {
                String srcPsType = srcPs.getType();
                String dstPsType = candidate.getType();
                List<String> types = primaryStoragePrimaryStorageMetrics.get(srcPsType);
                if (types == null || !types.contains(dstPsType)) {
                    candidates.remove(candidate);
                    continue;
                }

                // sharedblock couldn't do migrate if there is no cluster cross source and destination storage
                List<String> psTypes = primaryStorageNotSupportCrossClusterMigrationMetrics.get(srcPsType);
                if (psTypes != null && psTypes.contains(dstPsType) && !psUuids.contains(candidate.getUuid())) {
                    candidates.remove(candidate);
                }
            }
        }

        // make sure srcPs and dstPs are attached to clusters that have the same L2 networks
        if (srcVolume.getType() == VolumeType.Data) {
            return PrimaryStorageInventory.valueOf(candidates);
        }

        VmInstanceVO srcVm = dbf.findByUuid(srcVolume.getVmInstanceUuid(), VmInstanceVO.class);
        if (srcVm.getVmNics() == null) {
            return PrimaryStorageInventory.valueOf(candidates);
        }

        List<String> vmNics = SQL.New("select nic.uuid from VmNicVO nic where nic.vmInstanceUuid = :vmInstanceUuid")
                .param("vmInstanceUuid", srcVm.getUuid())
                .list();
        List<String> mutilNicClusters = Q.New(ClusterVO.class)
                .select(ClusterVO_.uuid)
                .listValues();
        for (String vmNic : vmNics) {
            List<String> nicClusters = SQL.New("select ncr.clusterUuid from PrimaryStorageVO ps, VmNicVO nic," +
                            "PrimaryStorageClusterRefVO pcr, L2NetworkClusterRefVO ncr, L3NetworkVO l3, L2NetworkVO l2 where" +
                            " nic.uuid = :vmNicUuid" +
                            " and l3.uuid = nic.l3NetworkUuid" +
                            " and l2.uuid = l3.l2NetworkUuid" +
                            " and ncr.l2NetworkUuid = l2.uuid" +
                            " and pcr.clusterUuid = ncr.clusterUuid" +
                            " and ps.uuid = pcr.primaryStorageUuid")
                    .param("vmNicUuid", vmNic)
                    .list();
            mutilNicClusters.retainAll(nicClusters);
        }

        List<String> psUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .in(PrimaryStorageClusterRefVO_.clusterUuid, mutilNicClusters)
                .listValues();

        for (PrimaryStorageVO candidate : new ArrayList<>(candidates)) {
            if (!psUuids.contains(candidate.getUuid())) {
                candidates.remove(candidate);
            }
        }

        return PrimaryStorageInventory.valueOf(candidates);
    }
}
