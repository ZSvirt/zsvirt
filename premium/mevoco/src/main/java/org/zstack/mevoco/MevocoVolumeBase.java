package org.zstack.mevoco;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.header.vm.additions.VmHostFileBackupJob;
import org.zstack.kvm.KVMConstant;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.volume.*;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.header.volume.block.BlockVolumeVO_;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.kvm.efi.KvmSecureBootManager;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileMsg;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileOnHypervisorMsg;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileReply;
import org.zstack.kvm.vmfiles.message.SyncVmHostFilesFromHostMsg;
import org.zstack.storage.migration.primary.MigrateDataVolumeOverlayMsg;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.local.MigrateVolumeOverlayMsg;
import org.zstack.storage.snapshot.SnapshotCanonicalEvents;
import org.zstack.storage.snapshot.VolumeSnapshotManagerImpl;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.*;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.string.GetCpuRangeMethod;

import javax.persistence.Tuple;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.header.volume.MevocoVolumeConstants.*;
import static org.zstack.header.volume.VolumeQosType.BANDWIDTH;
import static org.zstack.header.volume.VolumeQosType.IOPS;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by miao on 12/23/16.
 */
public class MevocoVolumeBase extends VolumeBase {
    private static final CLogger logger = Utils.getLogger(MevocoVolumeBase.class);

    MevocoVolumeBase(VolumeVO vo) {
        super(vo);

        allowedOperations.addState(VolumeStatus.Ready,
                MigrateDataVolumeOverlayMsg.class.getName(),
                MigrateVolumeOverlayMsg.class.getName(),
                VolumeSnapshotDeletionOverlayVolumeMsg.class.getName()
                );
    }

    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private KvmSecureBootManager secureBootManager;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeDeletionMsg) {
            handle((VolumeDeletionMsg) msg);
        } else if (msg instanceof CreateVolumesSnapshotMsg) {
            handle((CreateVolumesSnapshotMsg) msg);
        } else if (msg instanceof SetVolumeQosMsg) {
            handle((SetVolumeQosMsg) msg);
        } else if (msg instanceof CreateVolumesSnapshotOverlayInnerMsg) {
            handle((CreateVolumesSnapshotOverlayInnerMsg) msg);
        } else if (msg instanceof ResizeVolumeMsg) {
            handle((ResizeVolumeMsg) msg);
        } else if (msg instanceof InstantiateRootVolumeForRecoveryMsg) {
            handle((InstantiateRootVolumeForRecoveryMsg) msg);
        } else if (msg instanceof APIMessage) {
            handle((APIMessage) msg);
        } else {
            super.handleMessage(msg);
        }
    }
    private void handle(APIMessage msg) {
        if (msg instanceof APISetVolumeQosMsg) {
            handle((APISetVolumeQosMsg) msg);
        } else if (msg instanceof APIGetVolumeQosMsg) {
            handle((APIGetVolumeQosMsg) msg);
        } else if (msg instanceof APIDeleteVolumeQosMsg) {
            handle((APIDeleteVolumeQosMsg) msg);
        } else if (msg instanceof APIResizeRootVolumeMsg) {
            handle((APIResizeRootVolumeMsg) msg);
        } else if (msg instanceof APIResizeDataVolumeMsg) {
            handle((APIResizeDataVolumeMsg) msg);
        } else if (msg instanceof APICreateVolumesSnapshotMsg) {
            handle((APICreateVolumesSnapshotMsg) msg);
        } else if (msg instanceof APIValidateVolumeSnapshotChainMsg) {
            handle((APIValidateVolumeSnapshotChainMsg) msg);
        } else if (msg instanceof APIGetVolumeIoThreadPinMsg) {
            handle((APIGetVolumeIoThreadPinMsg) msg);
        } else if (msg instanceof APISetVolumeIoThreadPinMsg) {
            handle((APISetVolumeIoThreadPinMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(APIGetVolumeIoThreadPinMsg msg) {
        APIGetVolumeIoThreadPinReply reply = new APIGetVolumeIoThreadPinReply();
        String pin = MevocoVolumeSystemTags.IO_THREAD_PIN.getTokenByResourceUuid(msg.getUuid(), MevocoVolumeSystemTags.IO_THREAD_PIN_TOKEN);
        if (pin == null || pin.isEmpty()) {
            reply.setIoThreadId(DEFAULT_NULL_IOTHREADID);
            reply.setPin(DEFAULT_NULL_IOTHREADPIN);
        } else {
            String[] temp = pin.split(IOTHREADPIN_SEPARATOR);
            reply.setIoThreadId(temp[0]);
            reply.setPin(temp[1]);
        }
        reply.setVolumeUuid(msg.getVolumeUuid());
        bus.reply(msg, reply);
    }

    private void handle(APISetVolumeIoThreadPinMsg msg) {
        APISetVolumeIoThreadPinEvent evt = new APISetVolumeIoThreadPinEvent(msg.getId());

        VmInstanceVO vm = dbf.findByUuid(msg.getVmUuid(), VmInstanceVO.class);
        String hostUuid = vm.getHostUuid();
        if (hostUuid == null) {
            hostUuid = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolume().getUuid())
                    .select(LocalStorageResourceRefVO_.hostUuid)
                    .findValue();
        }
        String finalHostUuid = hostUuid;

        FlowChain flow = FlowChainBuilder.newSimpleFlowChain().allowEmptyFlow();
        flow.setName(String.format("set-iothreadpin-on-volume-%s", msg.getVolumeUuid()));

        if (msg.getPin().isEmpty()) {
            flow.then(new NoRollbackFlow() {
                String __name__ = String.format("delete-iothread-on-volume-%s", msg.getVolumeUuid());
                @Override
                public boolean skip(Map data) {
                    if (!MevocoVolumeSystemTags.IO_THREAD_PIN.hasTag(msg.getVolumeUuid())) {
                        logger.warn(String.format("delete iothreadpin on a no-iothreadpin volume[%s], skip.", msg.getUuid()));
                        return true;
                    }
                    return false;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    MevocoVolumeSystemTags.IO_THREAD_PIN.delete(msg.getVolumeUuid());
                    logger.info(String.format("delete iothread pin[%s] on volume[%s].", msg.getPin(), msg.getVolumeUuid()));
                    trigger.next();
                }
            });
        } else {
            flow.then(new NoRollbackFlow() {
                String __name__ = String.format("is-out-of-max-cpu-id-on-host-%s", finalHostUuid);
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    int maxCpuId = GetCpuRangeMethod.getMaxCpuIdInString(msg.getPin());
                    if (maxCpuId == -1) {
                        ErrorCode err = Platform.err(SysErrors.OPERATION_ERROR, "invalid volume[%s] iothread pin[%s]!", msg.getUuid(), msg.getPin());
                        trigger.fail(err);
                        return;
                    }
                    String maxHostCpuIdStr = HostSystemTags.CPU_PROCESSOR_NUM.getTokenByResourceUuid(finalHostUuid, HostSystemTags.CPU_PROCESSOR_NUM_TOKEN);
                    int maxHostCpuId = Integer.parseInt(maxHostCpuIdStr);
                    if (maxCpuId > (maxHostCpuId-1)) {
                        ErrorCode err = Platform.err(SysErrors.OPERATION_ERROR, String.format("pin[%s] is out of the max cpu id[%d] on host[%s].", msg.getPin(), maxHostCpuId, finalHostUuid));
                        trigger.fail(err);
                        return;
                    }
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = String.format("limit-number-of-iothreadpined-virtio-scsi-volume-on-vm-%s", msg.getVmUuid());

                @Override
                public boolean skip(Map data) {
                    if (!KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(msg.getVolumeUuid())) {
                        return true;
                    }
                    return false;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    int numberOfIoThreadPin = 0;
                    for (VolumeVO vol: vm.getAllDataVolumes()) {
                        if (vol.getUuid().equals(msg.getVolumeUuid())) {
                            continue;
                        }
                        boolean existTag = MevocoVolumeSystemTags.IO_THREAD_PIN.hasTag(vol.getUuid());
                        boolean isVirtioScsi = KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(vol.getUuid());
                        if (existTag && isVirtioScsi) {
                            numberOfIoThreadPin += 1;
                        }
                    }
                    if (numberOfIoThreadPin >= IOTHREADPIN_QUANTITY_LIMIT_PER_VM) {
                        ErrorCode err = Platform.err(SysErrors.OPERATION_ERROR, String.format("beyond the number limit[%d] of volumes with iothreadPin on VM[%s].", IOTHREADPIN_QUANTITY_LIMIT_PER_VM, vm.getUuid()));
                        trigger.fail(err);
                        return;
                    }
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = String.format("set-iothreadpin-on-vm-%s", msg.getVmUuid());
                @Override
                public boolean skip(Map data) {
                    if (!vm.getState().equals(VmInstanceState.Running)) {
                        logger.info(String.format("Vm[%s] attached by volume[%s] is not running, skip set iothread cmd.", vm.getUuid(), msg.getVolumeUuid()));
                        return true;
                    }
                    return false;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    MevocoKVMAgentCommands.SetVmIoThreadPinCmd cmd = new MevocoKVMAgentCommands.SetVmIoThreadPinCmd();
                    cmd.setVmUuid(msg.getVmUuid());
                    cmd.setIoThreadId(msg.getIoThreadId());
                    cmd.setPin(msg.getPin());

                    KVMHostAsyncHttpCallMsg kMsg = new KVMHostAsyncHttpCallMsg();
                    kMsg.setPath(MevocoKVMConstant.SET_VM_IOTHREAD_PIN_PATH);
                    kMsg.setHostUuid(finalHostUuid);
                    kMsg.setCommand(cmd);
                    kMsg.setNoStatusCheck(true);
                    bus.makeTargetServiceIdByResourceUuid(kMsg, HostConstant.SERVICE_ID, finalHostUuid);
                    bus.send(kMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                trigger.fail(reply.getError());
                            } else {
                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                MevocoKVMAgentCommands.SetVmIoThreadPinRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetVmIoThreadPinRsp.class);
                                if (rsp.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(operr("Failed set iothread[%d] pin[%s] on vm[%s]: %s.", msg.getIoThreadId(), msg.getPin(), vm.getUuid(), rsp.getError()));
                                }
                            }
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = String.format("save-io-thread-pin-on-volume-%s", msg.getVolumeUuid());
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    String s = String.format("%d%s%s", msg.getIoThreadId(), IOTHREADPIN_SEPARATOR, msg.getPin());
                    SystemTagCreator creator = MevocoVolumeSystemTags.IO_THREAD_PIN.newSystemTagCreator(msg.getVolumeUuid());
                    creator.setTagByTokens(map(e(MevocoVolumeSystemTags.IO_THREAD_PIN_TOKEN, s)));
                    creator.recreate = true;
                    creator.create();
                    logger.info(String.format("set iothread[%d] pin[%s] on volume[%s].", msg.getIoThreadId(), msg.getPin(), msg.getVolumeUuid()));
                    trigger.next();
                }
            });
        }

        flow.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                logger.info(String.format("set vol[%s] iothread[%d] pin[%s] on VM[%s] successfully", msg.getVolumeUuid(), msg.getIoThreadId(), msg.getPin(), msg.getVmUuid()));
                evt.setIoThreadId(msg.getIoThreadId());
                evt.setPin(msg.getPin());
                evt.setVolumeUuid(msg.getVolumeUuid());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }


    private void handle(APIValidateVolumeSnapshotChainMsg msg) {
        APIValidateVolumeSnapshotChainEvent evt = new APIValidateVolumeSnapshotChainEvent(msg.getId());

        if (!Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, msg.getUuid()).isExists()) {
            logger.debug(String.format("volume[uuid: %s] have no snapshot, return success", msg.getUuid()));
            bus.publish(evt);
            return;
        }

        String currentTreeUuid = Q.New(VolumeSnapshotTreeVO.class)
                .select(VolumeSnapshotTreeVO_.uuid)
                .eq(VolumeSnapshotTreeVO_.current, true)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, msg.getUuid())
                .findValue();
        if (currentTreeUuid == null) {
            evt.setError(operr("can not found in used snapshot tree of volume[uuid: %s]." +
                    " Maybe no snapshot chain need to validate.", msg.getUuid()));
            bus.publish(evt);
            return;
        }

        // find current in used snapshot
        Tuple tuple = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.distance, VolumeSnapshotVO_.primaryStorageUuid)
                .eq(VolumeSnapshotVO_.latest, true)
                .eq(VolumeSnapshotVO_.treeUuid, currentTreeUuid)
                .eq(VolumeSnapshotVO_.volumeUuid, msg.getUuid()).findTuple();
        if (tuple == null) {
            evt.setError(operr("can not found latest snapshot from tree[uuid: %s] of volume[uuid: %s]." +
                    " Maybe no snapshot chain need to validate.", currentTreeUuid, msg.getUuid()));
            bus.publish(evt);
            return;
        }

        Integer distance = (Integer) tuple.get(0);
        String primaryStorageUuid = (String) tuple.get(1);
        Map<String, Integer> volumeChainToCheck = new HashMap<>();
        List<Tuple> tuples = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.primaryStorageInstallPath, VolumeSnapshotVO_.distance)
                .eq(VolumeSnapshotVO_.treeUuid, currentTreeUuid)
                .lte(VolumeSnapshotVO_.distance, distance)
                .listTuple();

        if (tuples.isEmpty()) {
            evt.setError(operr("can not found snapshots from tree[uuid: %s] of volume[uuid: %s]." +
                    " Maybe no snapshot chain need to validate.", currentTreeUuid, msg.getUuid()));
            bus.publish(evt);
            return;
        }

        for (Tuple t : tuples) {
            volumeChainToCheck.put((String) t.get(0), (Integer) t.get(1));
        }

        // put volume install path on top of latest snapshot
        String volumeInstallPath = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getUuid())
                .select(VolumeVO_.installPath)
                .findValue();
        volumeChainToCheck.put(volumeInstallPath, distance + 1);

        CheckSnapshotMsg pmsg = new CheckSnapshotMsg();
        pmsg.setVolumeChainToCheck(volumeChainToCheck);
        pmsg.setPrimaryStorageUuid(primaryStorageUuid);
        pmsg.setVolumeUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(pmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(pmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }

                bus.publish(evt);
            }
        });
    }

    private void handle(InstantiateRootVolumeForRecoveryMsg msg) {
        InstantiateVolumeMsg imsg = new InstantiateVolumeMsg();
        // When volume is recovering, we cannot use thin volume.
        // Data volume is handled in DownloadDataVolumeToPrimaryStorageMsg
        VolumeUtils.SetVolumeProvisioningStrategy(msg.getVolumeUuid(), VolumeProvisioningStrategy.ThickProvisioning);
        imsg.setVolumeUuid(msg.getVolumeUuid());
        imsg.setHostUuid(msg.getHostUuid());
        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        imsg.setPrimaryStorageAllocated(msg.isPrimaryStorageAllocated());
        imsg.setSkipIfExisting(msg.isSkipIfExisting());
        imsg.setAllocatedInstallUrl(msg.getAllocatedInstallUrl());
        bus.makeTargetServiceIdByResourceUuid(imsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                InstantiateRootVolumeForRecoveryReply r = new InstantiateRootVolumeForRecoveryReply();
                if (!reply.isSuccess()) {
                    r.setError(reply.getError());
                } else {
                    InstantiateVolumeReply r2 = reply.castReply();
                    r.setVolume(r2.getVolume());
                    r.getVolume().setInstallPath(String.format("%s?r=%s",
                            r2.getVolume().getInstallPath(),
                            msg.getSelectedBackupStorage().getInstallPath())
                    );
                    SQL.New(VolumeVO.class)
                            .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                            .set(VolumeVO_.installPath, r.getVolume().getInstallPath())
                            .update();
                }
                bus.reply(msg, r);
            }
        });
    }

    private void handle(ResizeVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                ResizeVolumeReply reply = new ResizeVolumeReply();
                final VolumeVO vvo = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
                resizeVolume(msg, vvo, new ReturnValueCompletion<VolumeInventory>(chain) {
                    @Override
                    public void success(VolumeInventory returnValue) {
                        reply.setInventory(returnValue);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("resize-volume-%s", msg.getVolumeUuid());
            }
        });
    }

    private static class CreateVolumesSnapshotOverlayInnerContext {
        List<VolumeSnapshotInventory> volumeSnapshotList;
        List<String> hostBackupFileUuidList;
        boolean backupHostFileIfNeeded;
    }

    // all these logical should implement in a manager rather than base
    private void handle(CreateVolumesSnapshotOverlayInnerMsg msg) {
        DebugUtils.Assert(msg.getLockedVmInstanceUuids().size() == 1,
                "vm instance must has been locked by outer overlay message when take snapshot");
        DebugUtils.Assert(msg.getLockedVolumeUuids().size() <= msg.getVolumeSnapshotJobs().size(),
                "size of locked volumes can not larger than jobs");

        if (msg.getLockedVolumeUuids().size() < msg.getVolumeSnapshotJobs().size()) {
            CreateVolumesSnapshotOverlayInnerMsg innerMsg = new CreateVolumesSnapshotOverlayInnerMsg();
            innerMsg.setAccountUuid(msg.getAccountUuid());
            innerMsg.setVolumeSnapshotJobs(msg.getVolumeSnapshotJobs());
            innerMsg.setLockedVmInstanceUuids(msg.getLockedVmInstanceUuids());
            innerMsg.setConsistentType(msg.getConsistentType());
            innerMsg.setBackupHostFileIfNeeded(msg.isBackupHostFileIfNeeded());

            List<String> lockedVolumeUuids = new ArrayList<>(msg.getLockedVolumeUuids());
            String newLockVolumeUuid = null;
            for (CreateVolumesSnapshotsJobStruct volumesSnapshotsJob : msg.getVolumeSnapshotJobs()) {
                if (!lockedVolumeUuids.contains(volumesSnapshotsJob.getVolumeUuid())) {
                    newLockVolumeUuid = volumesSnapshotsJob.getVolumeUuid();
                }
            }
            lockedVolumeUuids.add(newLockVolumeUuid);
            innerMsg.setLockedVolumeUuids(lockedVolumeUuids);
            bus.makeTargetServiceIdByResourceUuid(innerMsg, VolumeConstant.SERVICE_ID, innerMsg.getVolumeUuid());

            CreateVolumesSnapshotOverlayVolumeMsg overlayVolumeMsg = new CreateVolumesSnapshotOverlayVolumeMsg();
            overlayVolumeMsg.setVolumeUuid(newLockVolumeUuid);
            overlayVolumeMsg.setMessage(innerMsg);
            bus.makeTargetServiceIdByResourceUuid(overlayVolumeMsg, VolumeConstant.SERVICE_ID, overlayVolumeMsg.getVolumeUuid());
            bus.send(overlayVolumeMsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        boolean snapshotOffline = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getLockedVmInstanceUuids().get(0))
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Stopped, VmInstanceState.Paused))
                .isExists();

        CreateVolumesSnapshotOverlayInnerContext context = new CreateVolumesSnapshotOverlayInnerContext();
        context.backupHostFileIfNeeded = msg.isBackupHostFileIfNeeded();
        CreateVolumesSnapshotOverlayInnerReply innerReply = new CreateVolumesSnapshotOverlayInnerReply();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-snapshot-for-volumes-%s",
                        msg.getVolumeSnapshotJobs()
                                .stream()
                                .map(CreateVolumesSnapshotsJobStruct::getVolumeUuid)
                                .collect(Collectors.toList())));
        chain.then(new NoRollbackFlow() {
            String __name__ = "ask-snapshot-struct";

            @Override
            public boolean skip(Map data) {
                return snapshotOffline;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(msg.getVolumeSnapshotJobs()).all((job, completion1) -> {
                    AskVolumeSnapshotStructMsg askMsg = new AskVolumeSnapshotStructMsg();
                    askMsg.setAccountUuid(msg.getAccountUuid());
                    askMsg.setDescription(job.getDescription());
                    askMsg.setName(job.getName());
                    askMsg.setResourceUuid(job.getResourceUuid());
                    askMsg.setVolumeUuid(job.getVolumeUuid());

                    bus.makeLocalServiceId(askMsg, VolumeSnapshotConstant.SERVICE_ID);
                    bus.send(askMsg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errList.getCauses().add(reply.getError());
                                completion1.done();
                                return;
                            }
                            AskVolumeSnapshotStructReply areply = reply.castReply();
                            if (!areply.isSuccess()) {
                                errList.getCauses().add(areply.getError());
                            } else {
                                job.setVolumeSnapshotStruct(areply.getStruct());
                            }
                            completion1.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errList.getCauses().isEmpty()) {
                            trigger.fail(errList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-volumes-snapshot-if-vm-online";

            @Override
            public boolean skip(Map data) {
                return snapshotOffline;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                createVolumesSnapshotOnline(msg, context, new Completion(trigger) {
                    @Override
                    public void success() {
                        innerReply.setInventories(context.volumeSnapshotList);
                        innerReply.setHostBackupFileUuidList(context.hostBackupFileUuidList);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-volumes-snapshot-if-vm-offline";

            @Override
            public boolean skip(Map data) {
                return !snapshotOffline;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                createVolumesSnapshotOffline(msg, context, new Completion(trigger) {
                    @Override
                    public void success() {
                        innerReply.setInventories(context.volumeSnapshotList);
                        innerReply.setHostBackupFileUuidList(context.hostBackupFileUuidList);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                innerReply.setError(errCode);
                bus.reply(msg, innerReply);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, innerReply);
            }
        }).start();
    }

    private void beforeTakeVolumesSnapshot(final Iterator<BeforeTakeLiveSnapshotsOnVolumes> it, CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmMsg tmsg, Map flowData, Completion completion) {
        if(!it.hasNext()) {
            completion.success();
            return;
        }

        BeforeTakeLiveSnapshotsOnVolumes ext = it.next();
        ext.beforeTakeLiveSnapshotsOnVolumes(msg, tmsg, flowData, new Completion(completion) {
            @Override
            public void success() {
                beforeTakeVolumesSnapshot(it, msg, tmsg, flowData, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void afterTakeVolumesSnapshot(final Iterator<VolumeSnapshotCreationExtensionPoint> it, CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion) {
        if(!it.hasNext()) {
            completion.success();
            return;
        }

        VolumeSnapshotCreationExtensionPoint ext = it.next();
        ext.afterVolumeLiveSnapshotGroupCreatedOnBackend(msg, treply, new Completion(completion) {
            @Override
            public void success() {
                afterTakeVolumesSnapshot(it, msg, treply, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void afterTakeVolumesSnapshotFailed(final Iterator<VolumeSnapshotCreationExtensionPoint> it, CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply) {
        if(!it.hasNext()) {
            return;
        }

        VolumeSnapshotCreationExtensionPoint ext = it.next();
        ext.afterVolumeLiveSnapshotGroupCreationFailsOnBackend(msg, treply);
        afterTakeVolumesSnapshotFailed(it, msg, treply);
    }

    // TODO(weiw): provides atomic
    // TODO(weiw): need code refactoring since this function is too big
    private void createVolumesSnapshotOnline(CreateVolumesSnapshotOverlayInnerMsg msg,
                                             CreateVolumesSnapshotOverlayInnerContext context,
                                             Completion completion) {
        List<String> volUuids = msg.getVolumeSnapshotJobs().stream().map(CreateVolumesSnapshotsJobStruct::getVolumeUuid).collect(Collectors.toList());
        Map<String, VolumeVO> volumeVOS = Q.New(VolumeVO.class).in(VolumeVO_.uuid, volUuids)
                .list().stream()
                .collect(Collectors.toMap(vo -> ((VolumeVO) vo).getUuid(), v -> (VolumeVO) v));

        DebugUtils.Assert(msg.getConsistentType() !=  ConsistentType.Application ||
                        volumeVOS.values().stream().anyMatch(it -> it.getType() == VolumeType.Memory),
                "take application consistent snapshot should contains memory volume.");
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, volumeVOS.values().iterator().next().getVmInstanceUuid())
                .find();

        String hostUuid = vmInstanceVO.getHostUuid();
        if (hostUuid == null) {
            if (vmInstanceVO.getState() != VmInstanceState.Running) {
                completion.fail(operr("Unexpectedly, VM[uuid:%s] is not running any more, please try again later", vmInstanceVO.getUuid()));
            } else {
                completion.fail(operr("How can a Running VM[uuid:%s] has no hostUuid?", vmInstanceVO.getUuid()));
            }
            return;
        }

        KVMHostVO host = dbf.findByUuid(hostUuid, KVMHostVO.class);

        TakeVolumesSnapshotOnKvmMsg tmsg = new TakeVolumesSnapshotOnKvmMsg();
        List<TakeSnapshotsOnKvmJobStruct> snapshotsOnKvmJobs = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct jobStruct : msg.getVolumeSnapshotJobs()) {
            TakeSnapshotsOnKvmJobStruct snapshotsOnKvmJob = new TakeSnapshotsOnKvmJobStruct();
            VolumeVO volumeVO = volumeVOS.get(jobStruct.getVolumeUuid());

            snapshotsOnKvmJob.setVolumeUuid(volumeVO.getUuid());
            snapshotsOnKvmJob.setVmInstanceUuid(volumeVO.getVmInstanceUuid());
            snapshotsOnKvmJob.setVolume(VolumeTO.valueOf(VolumeInventory.valueOf(volumeVO), KVMHostInventory.valueOf(host)));
            snapshotsOnKvmJob.setInstallPath(
                    jobStruct.getVolumeSnapshotStruct().getCurrent().getPrimaryStorageInstallPath());
            snapshotsOnKvmJob.setNewVolumeInstallPath(jobStruct.getVolumeSnapshotStruct()
                    .getCurrent().getPrimaryStorageInstallPath());
            snapshotsOnKvmJob.setSnapshotUuid(jobStruct.getVolumeSnapshotStruct()
                    .getCurrent().getUuid());

            snapshotsOnKvmJob.setMemory(VolumeType.Memory == volumeVO.getType());
            snapshotsOnKvmJob.setFull(false);
            snapshotsOnKvmJob.setLive(true);
            snapshotsOnKvmJob.setPreviousInstallPath(((VolumeTO) snapshotsOnKvmJob.getVolume()).getInstallPath());
            snapshotsOnKvmJobs.add(snapshotsOnKvmJob);
        }
        tmsg.setSnapshotJobs(snapshotsOnKvmJobs);
        tmsg.setHostUuid(hostUuid);

        if (context.backupHostFileIfNeeded) {
            String vmUuid = vmInstanceVO.getUuid();
            List<VmHostFileVO> hostFiles = Q.New(VmHostFileVO.class)
                    .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                    .eq(VmHostFileVO_.hostUuid, hostUuid)
                    .list();
            if (!hostFiles.isEmpty()) {
                List<VmHostFileBackupJob> backupJobs = new ArrayList<>();
                for (VmHostFileVO hf : hostFiles) {
                    VmHostFileBackupJob job = new VmHostFileBackupJob();
                    job.setSrcPath(hf.getPath());
                    job.setDestPath(KVMConstant.buildSnapshotBackupPathForVmHostFileType(hf.getType(), vmUuid));
                    job.setType(hf.getType().toString());
                    backupJobs.add(job);
                }
                tmsg.setVmHostFileBackupJobs(backupJobs);
            }
        }

        Map<VolumeVO, Long> requireSize = new HashMap<>();
        Map<VolumeVO, Long> allocatedSize = new HashMap<>();
        Map<VolumeVO, String> allocatedInstallUrls = new HashMap<>();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format(
                "create-snapshot-for-volumes-%s-on-vm-%s-online",
                msg.getLockedVolumeUuids(), volumeVOS.values().iterator().next().getUuid()));
        chain.putData(
                e(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR, true),
                e(VolumeSnapshotConstant.NEED_BLOCK_STREAM_ON_HYPERVISOR, true)
        );
        chain.then(new NoRollbackFlow() {
            String __name__ = "make-install-path-of-snapshots-by-ask-ps";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(msg.getVolumeSnapshotJobs()).all((job, whileCompletion) -> {
                    AskInstallPathForNewSnapshotMsg amsg = new AskInstallPathForNewSnapshotMsg();
                    amsg.setPrimaryStorageUuid(job.getPrimaryStorageUuid());
                    amsg.setSnapshotUuid(job.getVolumeSnapshotStruct().getCurrent().getUuid());
                    amsg.setVolumeInventory(VolumeInventory.valueOf(
                            (VolumeVO) Q.New(VolumeVO.class).eq(VolumeVO_.uuid, job.getVolumeUuid()).find())
                    );
                    amsg.setHostUuid(tmsg.getHostUuid());
                    bus.makeTargetServiceIdByResourceUuid(
                            amsg, PrimaryStorageConstant.SERVICE_ID, amsg.getPrimaryStorageUuid());
                    bus.send(amsg, new CloudBusCallBack(amsg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errList.getCauses().add(reply.getError());
                                whileCompletion.done();
                                return;
                            }

                            AskInstallPathForNewSnapshotReply areply = reply.castReply();
                            if (!areply.isSuccess()) {
                                errList.getCauses().add(areply.getError());
                                whileCompletion.done();
                                return;
                            }

                            for (TakeSnapshotsOnKvmJobStruct job : tmsg.getSnapshotJobs()) {
                                if (job.getSnapshotUuid().equals(amsg.getSnapshotUuid())) {
                                    job.setInstallPath(areply.getSnapshotInstallPath());
                                }
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
        }).then(new NoRollbackFlow() {
            String __name__ = "sync-root-volume-size";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(volumeVOS.values()).each((volumeVO, whileCompleteion) -> {
                    if (VolumeType.Memory.equals(volumeVO.getType())) {
                        requireSize.put(volumeVO, vmInstanceVO.getMemorySize());
                        whileCompleteion.done();
                        return;
                    }
                    SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();
                    smsg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                    smsg.setVolumeUuid(volumeVO.getUuid());
                    smsg.setInstallPath(volumeVO.getInstallPath());
                    bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                    bus.send(smsg, new CloudBusCallBack(msg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompleteion.addError(reply.getError());
                                whileCompleteion.allDone();
                                return;
                            }

                            SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                            requireSize.put(volumeVO, r.getActualSize());
                            whileCompleteion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }
                    }
                });

            }
        }).then(new Flow() {
            String __name__ = "pre-allocate-primary-storage-capacity";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(requireSize.keySet()).each((vol, compl) -> {
                    AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                    amsg.setRequiredPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                    amsg.setSize(requireSize.get(vol));
                    amsg.setRequiredInstallUri(String.format("volume://%s", vol.getUuid()));
                    amsg.setNoOverProvisioning(true);

                    bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                    bus.send(amsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                compl.addError(reply.getError());
                                compl.allDone();
                                return;
                            }

                            AllocatePrimaryStorageSpaceReply r = reply.castReply();
                            allocatedSize.put(vol, r.getSize());
                            allocatedInstallUrls.put(vol, r.getAllocatedInstallUrl());
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                // rollback when no snapshot created
                if (data.containsKey(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS)) {
                    trigger.rollback();
                    return;
                }

                allocatedSize.keySet().forEach(vol -> {
                    ReleasePrimaryStorageSpaceMsg dmsg = new ReleasePrimaryStorageSpaceMsg();
                    dmsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                    dmsg.setDiskSize(allocatedSize.get(vol));
                    dmsg.setNoOverProvisioning(true);
                    dmsg.setAllocatedInstallUrl(allocatedInstallUrls.get(vol));
                    bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                    bus.send(dmsg);
                });
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "run-extension-point-before-take-volumes-snapshot";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<BeforeTakeLiveSnapshotsOnVolumes> exts = pluginRgty.getExtensionList(BeforeTakeLiveSnapshotsOnVolumes.class);
                beforeTakeVolumesSnapshot(exts.iterator(), msg, tmsg, chain.getData(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "block-stream-if-full-snapshot-need";

            @Override
            public boolean skip(Map data) {
                return !(Boolean) data.get(VolumeSnapshotConstant.NEED_BLOCK_STREAM_ON_HYPERVISOR);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(msg.getVolumeSnapshotJobs()).each((job, whileCompletion) -> {
                    if (!job.getVolumeSnapshotStruct().isFullSnapshot()) {
                        whileCompletion.done();
                        return;
                    }

                    blockStreamVolume(job, new Completion(trigger) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
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
        }).then(new Flow() {
            String __name__ = String.format("send-take-snapshot-message-to-kvm-host-%s", tmsg.getHostUuid());

            @Override
            public boolean skip(Map data) {
                return !(Boolean) data.get(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bus.makeTargetServiceIdByResourceUuid(tmsg, HostConstant.SERVICE_ID, tmsg.getHostUuid());
                bus.send(tmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        TakeVolumesSnapshotOnKvmReply treply = reply.castReply();
                        if (!treply.isSuccess()) {
                            trigger.fail(treply.getError());
                            return;
                        }

                        for (TakeSnapshotsOnKvmResultStruct result: treply.getSnapshotsResults()) {
                            for (CreateVolumesSnapshotsJobStruct job : msg.getVolumeSnapshotJobs()) {
                                if (job.getVolumeUuid().equals(result.getVolumeUuid())) {
                                    updateVolumeSnapshotInDB(result, job);
                                }
                            }
                        }
                        data.put(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS, treply);
                        trigger.next();
                    }
                });
            }

            private void updateVolumeSnapshotInDB(TakeSnapshotsOnKvmResultStruct result, CreateVolumesSnapshotsJobStruct job) {
                VolumeVO volumeVO = Q.New(VolumeVO.class)
                        .eq(VolumeVO_.uuid, job.getVolumeUuid())
                        .find();

                VolumeSnapshotVO svo = dbf.findByUuid(job.getVolumeSnapshotStruct().getCurrent().getUuid(), VolumeSnapshotVO.class);
                svo.setStatus(VolumeSnapshotStatus.Ready);
                svo.setSize(result.getSize());
                svo.setPrimaryStorageUuid(job.getPrimaryStorageUuid());
                svo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());

                if (volumeVO.getType().equals(VolumeType.Memory)) {
                    svo.setPrimaryStorageInstallPath(result.getInstallPath());
                } else {
                    svo.setPrimaryStorageInstallPath(result.getPreviousInstallPath());
                    if (result.getPreviousInstallPath() != null) {
                        volumeVO.setInstallPath(result.getInstallPath());
                        dbf.update(volumeVO);
                    }
                }

                svo = dbf.updateAndRefresh(svo);
                VolumeSnapshotInventory sinv = svo.toInventory();
                job.getVolumeSnapshotStruct().setCurrent(sinv);

                new FireSnapShotCanonicalEvent().fireSnapShotStatusChangedEvent(svo.getStatus(), sinv);
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }

        }).then(new NoRollbackFlow() {
            String __name__ = "backup-vm-hostfile-on-hypervisor-when-snapshot-skipped";

            @Override
            public boolean skip(Map data) {
                return (Boolean) data.get(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR)
                        || tmsg.getVmHostFileBackupJobs() == null
                        || tmsg.getVmHostFileBackupJobs().isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BackupVmHostFileOnHypervisorMsg bmsg = new BackupVmHostFileOnHypervisorMsg();
                bmsg.setHostUuid(tmsg.getHostUuid());
                bmsg.setVmHostFileBackupJobs(tmsg.getVmHostFileBackupJobs());
                bus.makeLocalServiceId(bmsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                bus.send(bmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        // An empty reply; afterTakeVolumesSnapshot hooks are responsible for
                        // populating hostBackupTempResourceUuid after syncing backup files from host to DB
                        TakeVolumesSnapshotOnKvmReply treply = new TakeVolumesSnapshotOnKvmReply();
                        treply.setSnapshotsResults(new ArrayList<>());
                        data.put(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS, treply);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "adjust-snapshot-capacity-on-primary-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                new While<>(msg.getVolumeSnapshotJobs()).each((job, compl) -> {
                    long size = job.getVolumeSnapshotStruct().getCurrent().getSize() -
                            allocatedSize.getOrDefault(volumeVOS.get(job.getVolumeUuid()), 0L);

                    if (size == 0) {
                        compl.done();
                        return;
                    }

                    if (size > 0) {
                        logger.debug(String.format("reserve snapshot size[%s] on primary storage[%s] for volume[uuid:%s]",
                                size, job.getPrimaryStorageUuid(), job.getVolumeUuid()));
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(job.getPrimaryStorageUuid());
                        amsg.setSize(size);
                        amsg.setRequiredInstallUri(String.format("volume://%s", job.getVolumeUuid()));
                        amsg.setForce(true);
                        amsg.setNoOverProvisioning(true);
                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, job.getPrimaryStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    allocatedSize.put(volumeVOS.get(job.getVolumeUuid()), job.getVolumeSnapshotStruct().getCurrent().getSize());
                                }
                                compl.done();
                            }
                        });
                        return;
                    }

                    logger.debug(String.format("release snapshot size[%s] on primary storage[%s] for volume[uuid:%s]",
                            Math.abs(size), job.getPrimaryStorageUuid(), job.getVolumeUuid()));
                    ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                    rmsg.setPrimaryStorageUuid(job.getPrimaryStorageUuid());
                    rmsg.setDiskSize(Math.abs(size));
                    rmsg.setNoOverProvisioning(true);
                    rmsg.setAllocatedInstallUrl(allocatedInstallUrls.get(volumeVOS.get(job.getVolumeUuid())));
                    bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, job.getPrimaryStorageUuid());
                    bus.send(rmsg);
                    allocatedSize.put(volumeVOS.get(job.getVolumeUuid()), job.getVolumeSnapshotStruct().getCurrent().getSize());
                    compl.done();
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            // TODO: this flow should be refactored
            String __name__ = "run-extension-point-after-take-volumes-snapshot";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VolumeSnapshotCreationExtensionPoint> exts = pluginRgty.getExtensionList(VolumeSnapshotCreationExtensionPoint.class);
                afterTakeVolumesSnapshot(exts.iterator(), msg,
                        (TakeVolumesSnapshotOnKvmReply) data.get(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "save-volume-snapshot-integrity";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (CreateVolumesSnapshotsJobStruct job : msg.getVolumeSnapshotJobs()) {
                    if (job.getVolumeSnapshotStruct().isNewChain()) {
                        VolumeSnapshotVO svo = dbf.findByUuid(job.getVolumeSnapshotStruct().getCurrent().getUuid(), VolumeSnapshotVO.class);
                        VolumeSnapshotReferenceUtils.updateReferenceAfterFirstSnapshot(svo);
                    }
                }

                List<VolumeSnapshotCreationExtensionPoint> extensionList = pluginRgty.getExtensionList(VolumeSnapshotCreationExtensionPoint.class);

                if (extensionList.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<VolumeSnapshotInventory> inventories = new ArrayList<>();
                TakeVolumesSnapshotOnKvmReply reply = (TakeVolumesSnapshotOnKvmReply) data.get(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS);
                if (reply == null || reply.getSnapshotsResults() == null || reply.getSnapshotsResults().isEmpty()) {
                    trigger.next();
                    return;
                }


                for (TakeSnapshotsOnKvmResultStruct result: reply.getSnapshotsResults()) {
                    for (CreateVolumesSnapshotsJobStruct job : msg.getVolumeSnapshotJobs()) {
                        if (job.getVolumeUuid().equals(result.getVolumeUuid())) {
                            VolumeSnapshotVO svo = dbf.findByUuid(job.getVolumeSnapshotStruct().getCurrent().getUuid(), VolumeSnapshotVO.class);
                            inventories.add(VolumeSnapshotInventory.valueOf(svo));
                        }
                    }
                }
                new While<>(inventories).each((inv, compl) -> {
                    afterVolumeSnapshotCreated(extensionList.iterator(), inv, new Completion(compl) {
                        @Override
                        public void success() {
                            compl.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            compl.addError(errorCode);
                            compl.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        trigger.fail(multiErr(errorCodeList));
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                List<VolumeSnapshotInventory> inventories = msg.getVolumeSnapshotJobs().stream()
                        .map(job -> VolumeSnapshotInventory.valueOf(
                                (VolumeSnapshotVO) Q.New(VolumeSnapshotVO.class)
                                        .eq(VolumeSnapshotVO_.uuid, job.getVolumeSnapshotStruct().getCurrent().getUuid())
                                        .find()))
                        .collect(Collectors.toList());
                for (VolumeSnapshotInventory inventory: inventories) {
                    VolumeSnapshotManagerImpl.markSnapshotTreeCompleted(inventory);
                }
                context.volumeSnapshotList = inventories;

                final TakeVolumesSnapshotOnKvmReply tReply =
                        (TakeVolumesSnapshotOnKvmReply) data.get(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS);
                if (tReply != null && tReply.getHostBackupTempResourceUuid() != null) {
                    context.hostBackupFileUuidList = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.resourceUuid, tReply.getHostBackupTempResourceUuid())
                            .select(VmHostBackupFileVO_.uuid)
                            .listValues();
                }

                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                for (CreateVolumesSnapshotsJobStruct jobStruct : msg.getVolumeSnapshotJobs()) {
                    rollbackSnapshot(jobStruct.getVolumeSnapshotStruct().getCurrent().getUuid());
                }
                List<VolumeSnapshotCreationExtensionPoint> exts = pluginRgty.getExtensionList(VolumeSnapshotCreationExtensionPoint.class);
                afterTakeVolumesSnapshotFailed(exts.iterator(), msg, (TakeVolumesSnapshotOnKvmReply) data.get(MevocoConstants.VOLUMES_SNAPSHOT_RESULTS));
                completion.fail(errCode);
            }
        }).start();
    }

    private void afterVolumeSnapshotCreated(final Iterator<VolumeSnapshotCreationExtensionPoint> it, VolumeSnapshotInventory inventory,
                                           Completion completion) {
        if(!it.hasNext()) {
            completion.success();
            return;
        }

        VolumeSnapshotCreationExtensionPoint ext = it.next();
        ext.afterVolumeSnapshotCreated(inventory, new Completion(completion) {
            @Override
            public void success() {
                afterVolumeSnapshotCreated(it, inventory, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    private void blockStreamVolume(CreateVolumesSnapshotsJobStruct struct, Completion completion) {
        BlockStreamVolumeMsg bmsg = new BlockStreamVolumeMsg();
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, struct.getVolumeUuid()).find();
        DebugUtils.Assert(volumeVO.getVmInstanceUuid() != null, String.format(
                "volume[uuid: %s] must attached vm instance", volumeVO.getUuid()));
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid())
                .find();

        String hostUuid = vmInstanceVO.getHostUuid() != null ? vmInstanceVO.getHostUuid() : vmInstanceVO.getLastHostUuid();

        bmsg.setVmInstanceUuid(volumeVO.getVmInstanceUuid());
        bmsg.setVolume(VolumeInventory.valueOf(volumeVO));
        bmsg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(bmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(bmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                BlockStreamVolumeReply breply = reply.castReply();
                if(!breply.isSuccess()) {
                    throw new OperationFailureException(breply.getError());
                }
                completion.success();
            }
        });
    }

    // TODO(weiw): provides atomic
    private void createVolumesSnapshotOffline(CreateVolumesSnapshotOverlayInnerMsg msg,
                                              CreateVolumesSnapshotOverlayInnerContext context,
                                              Completion completion) {
        DebugUtils.Assert(msg.getLockedVmInstanceUuids().size() == 1, "only one vm can be locked");

        List<CreateVolumeSnapshotMsg> createVolumeSnapshotMsgs = new ArrayList<>();

        for (CreateVolumesSnapshotsJobStruct volumesSnapshotsJob : msg.getVolumeSnapshotJobs()) {
            CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
            cmsg.setAccountUuid(msg.getAccountUuid());
            cmsg.setName(volumesSnapshotsJob.getName());
            cmsg.setDescription(volumesSnapshotsJob.getDescription());
            cmsg.setVolumeUuid(volumesSnapshotsJob.getVolumeUuid());
            cmsg.setResourceUuid(volumesSnapshotsJob.getResourceUuid());

            createVolumeSnapshotMsgs.add(cmsg);
        }

        VmHostFileVO nvRamFile = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, msg.getLockedVmInstanceUuids().get(0))
                .eq(VmHostFileVO_.type, VmHostFileType.NvRam)
                .orderByDesc(VmHostFileVO_.lastOpDate)
                .limit(1)
                .find();
        VmHostFileVO tpmStateFile = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, msg.getLockedVmInstanceUuids().get(0))
                .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                .orderByDesc(VmHostFileVO_.lastOpDate)
                .limit(1)
                .find();
        String tempUuidForVmHostFile = Platform.getUuid();

        context.volumeSnapshotList = Collections.synchronizedList(new ArrayList<>());
        SimpleFlowChain.of("create-volumes-snapshot-on-vm-offline")
            .then(Flow.of("create-volume-snapshots")
                .handle((trigger) ->
                    new While<>(createVolumeSnapshotMsgs).step((cmsg, whileCompletion) -> {
                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateVolumeSnapshotReply reply1 = reply.castReply();
                                    context.volumeSnapshotList.add(reply1.getInventory());
                                } else {
                                    whileCompletion.addError(reply.getError());
                                }
                                whileCompletion.done();
                            }
                        });
                    }, 5).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (errorCodeList.hasError()) {
                                trigger.fail(operr("failed to create volumes snapshots").withCause(errorCodeList));
                            } else {
                                trigger.next();
                            }
                        }
                    })
                )
                .rollback(trigger -> {
                    CollectionUtils.safeForEach(context.volumeSnapshotList, snapshot -> rollbackSnapshot(snapshot.getUuid()));
                    trigger.rollback();
                })
                .build())
            .then(Flow.of("sync-vm-host-files")
                .skipIf(data -> !context.backupHostFileIfNeeded || nvRamFile == null && tpmStateFile == null)
                .handle((trigger, data) -> {
                    SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();

                    Tuple tuple = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, msg.getLockedVmInstanceUuids().get(0))
                            .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                            .findTuple();
                    VmInstanceState vmState = tuple.get(0, VmInstanceState.class);
                    syncMsg.setVmUuid(msg.getLockedVmInstanceUuids().get(0));
                    syncMsg.setHostUuid((vmState == VmInstanceState.Paused) ?
                            tuple.get(1, String.class) : tuple.get(2, String.class));
                    data.put("host.uuid", syncMsg.getHostUuid());

                    if (nvRamFile != null) {
                        syncMsg.setNvRamPath(nvRamFile.getPath());
                    }
                    if (tpmStateFile != null) {
                        syncMsg.setTpmStateFolder(tpmStateFile.getPath());
                    }

                    bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                    bus.send(syncMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                trigger.next();
                                return;
                            }

                            if (vmState == VmInstanceState.Paused) {
                                trigger.fail(reply.getError());
                            } else {
                                // if VM is stopped, host file will send to MN already via libvirt shutdown event
                                // we can get file again. If file is missing, we will skip and use content in DB.
                                logger.info(String.format("failed to read VmHostFile[uuid=%s] from host, and we will use file content saved in DB.",
                                        msg.getLockedVmInstanceUuids().get(0)));
                                trigger.next();
                            }
                        }
                    });
                })
                .build())
            .then(Flow.of("copy-host-files")
                .skipIf(data -> !context.backupHostFileIfNeeded || nvRamFile == null && tpmStateFile == null)
                .handle((trigger, data) -> {
                    // because this time VolumeSnapshotGroupVO has not been persisted,
                    // so we create VmHostBackupFileVO [resourceUuid = vmUuid].
                    // and update resourceUuid to VolumeSnapshotGroupVO.uuid after VolumeSnapshotGroupVO created

                    // copy host files
                    //     from VmHostFileVO[vmInstanceUuid=vmUuid, hostUuid=data[host.uuid]]
                    //     to   VmHostBackupFileVO[resourceUuid=random]            (now)
                    //     to   VmHostBackupFileVO[resourceUuid=snapshotGroupUuid] (after SnapshotGroupVO persisting)
                    BackupVmHostFileMsg backupMsg = new BackupVmHostFileMsg();
                    backupMsg.setVmUuid(msg.getLockedVmInstanceUuids().get(0));
                    backupMsg.setHostUuid((String) data.get("host.uuid"));
                    backupMsg.setToResourceUuidList(list(tempUuidForVmHostFile));
                    bus.makeLocalServiceId(backupMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                    bus.send(backupMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                trigger.fail(reply.getError());
                                return;
                            }
                            context.hostBackupFileUuidList = ((BackupVmHostFileReply)reply.castReply()).getBackupFileUuidList();
                            trigger.next();
                        }
                    });
                })
                .rollback(trigger -> {
                    secureBootManager.cleanVmHostBackupFile(tempUuidForVmHostFile);
                    trigger.rollback();
                })
                .build())
            .propagateExceptionTo(completion)
            .done(completion::success)
            .error(completion::fail)
            .start();
    }

    @Transactional
    protected void rollbackSnapshot(String uuid) {
        VolumeSnapshotVO vo = dbf.getEntityManager().find(VolumeSnapshotVO.class, uuid);
        if (vo == null) {
            return;
        }

        dbf.getEntityManager().remove(vo);

        SQL.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, uuid)
                .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .delete();

        if (vo.getParentUuid() != null) {
            VolumeSnapshotVO parent = dbf.getEntityManager().find(VolumeSnapshotVO.class, vo.getParentUuid());
            parent.setLatest(true);
            dbf.getEntityManager().merge(parent);
        } else {
            VolumeSnapshotTreeVO chain = dbf.getEntityManager().find(VolumeSnapshotTreeVO.class, vo.getTreeUuid());
            dbf.getEntityManager().remove(chain);
        }
    }

    private ErrorCode check(CreateVolumesSnapshotMsg msg) {
        List<VolumeVO> volumeVOS = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct job : msg.getVolumeSnapshotJobs()) {
            VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, job.getVolumeUuid()).find();
            if (volumeVO == null || volumeVO.getVmInstanceUuid() == null) {
                return argerr("can not take snapshot for volumes[%s] while volume[uuid: %s] not attached",
                        msg.getVolumeSnapshotJobs().stream().map(CreateVolumesSnapshotsJobStruct::getVolumeUuid).collect(Collectors.toList()),
                        job.getVolumeUuid());
            }

            if (volumeVOS.stream().anyMatch(vo -> vo.getUuid().equals(job.getVolumeUuid()))) {
                return argerr("can not take snapshot for volumes[%s] while volume[uuid: %s] appears twice",
                        msg.getVolumeSnapshotJobs().stream().map(CreateVolumesSnapshotsJobStruct::getVolumeUuid).collect(Collectors.toList()),
                        job.getVolumeUuid());
            }

            volumeVOS.add(volumeVO);
            if (!volumeVO.getVmInstanceUuid().equals(volumeVOS.get(0).getVmInstanceUuid())) {
                return argerr("can not take snapshot for volumes[%s] attached multiple vms[%s, %s]",
                        msg.getVolumeSnapshotJobs().stream().map(CreateVolumesSnapshotsJobStruct::getVolumeUuid).collect(Collectors.toList()),
                        job.getVolumeUuid(), volumeVOS.get(0).getVmInstanceUuid());
            }
        }

        if (volumeVOS.isEmpty()) {
            return argerr("no volumes found");
        }
        return null;
    }

    private void handle(CreateVolumesSnapshotMsg msg) {
        CreateVolumesSnapshotReply originalReply = new CreateVolumesSnapshotReply();
        if (msg.getVolumeSnapshotJobs().isEmpty()) {
            bus.reply(msg, originalReply);
            return;
        }

        logger.debug("check volumes for bulk create snapshot");

        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        VmInstanceState vmState = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).findValue();
        boolean needCheck = volumeVO.getVmInstanceUuid() != null &&
                MevocoConstants.supportedVmStatesForLiveSnapshotOnVolumes.contains(vmState);
        ErrorCode err;
        if (needCheck && (err = check(msg)) != null) {
            originalReply.setError(err);
            bus.reply(msg, originalReply);
            return;
        }

        logger.debug("get lock and create snapshots");
        CreateVolumesSnapshotOverlayInnerMsg innerMsg = new CreateVolumesSnapshotOverlayInnerMsg();
        innerMsg.setConsistentType(msg.getConsistentType());
        innerMsg.setAccountUuid(msg.getAccountUuid());
        innerMsg.setVolumeSnapshotJobs(msg.getVolumeSnapshotJobs());
        innerMsg.setLockedVolumeUuids(new ArrayList<>());
        innerMsg.setLockedVmInstanceUuids(Collections.singletonList(volumeVO.getVmInstanceUuid()));
        innerMsg.setBackupHostFileIfNeeded(msg.isBackupHostFileIfNeeded());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, VolumeConstant.SERVICE_ID, innerMsg.getVolumeUuid());

        CreateVolumesSnapshotOverlayVmMsg overlayVmMsg = new CreateVolumesSnapshotOverlayVmMsg();
        overlayVmMsg.setMessage(innerMsg);
        overlayVmMsg.setVmInstanceUuid(volumeVO.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(overlayVmMsg, VmInstanceConstant.SERVICE_ID, overlayVmMsg.getVmInstanceUuid());
        bus.send(overlayVmMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                ErrorCode errCode = null;
                CreateVolumesSnapshotOverlayInnerReply innerReply = null;
                if (!reply.isSuccess()) {
                    errCode = reply.getError();
                } else if (!(innerReply = reply.castReply()).isSuccess()) {
                    errCode = innerReply.getError();
                }

                if (errCode == null && innerReply != null) {
                    originalReply.setInventories(innerReply.getInventories());
                    originalReply.setHostBackupFileUuidList(innerReply.getHostBackupFileUuidList());
                } else {
                    originalReply.setError(errCode);
                }

                bus.reply(msg, originalReply);
            }
        });
    }

    // TODO(weiw): should implement in a manager rather than base
    private void handle(APICreateVolumesSnapshotMsg msg) {
        APICreateVolumesSnapshotEvent event = new APICreateVolumesSnapshotEvent(msg.getId());

        if (msg.getVolumeUuids().isEmpty()) {
            event.setInventories(new ArrayList<>());
            bus.publish(event);
            return;
        }

        CreateVolumesSnapshotMsg msg1 = new CreateVolumesSnapshotMsg();
        List<CreateVolumesSnapshotsJobStruct> volumesSnapshotsJobs = new ArrayList<>();
        msg1.setAccountUuid(msg.getSession().getAccountUuid());
        for (String volumeUuid : msg.getVolumeUuids()) {
            CreateVolumesSnapshotsJobStruct volumesSnapshotsJob = new CreateVolumesSnapshotsJobStruct();
            VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();

            volumesSnapshotsJob.setVolumeUuid(volumeUuid);
            volumesSnapshotsJob.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
            volumesSnapshotsJob.setResourceUuid(getUuid());
            volumesSnapshotsJob.setName(String.format("volume-%s-snapshot-%s", volumeUuid, volumesSnapshotsJob.getResourceUuid()));
            volumesSnapshotsJob.setDescription(String.format("bulk snapshot for volumes[%s]", msg.getVolumeUuids()));
            volumesSnapshotsJobs.add(volumesSnapshotsJob);
        }
        msg1.setVolumeSnapshotJobs(volumesSnapshotsJobs);
        msg1.setConsistentType(ConsistentType.Crash);

        bus.makeTargetServiceIdByResourceUuid(msg1, VolumeConstant.SERVICE_ID, msg1.getVolumeSnapshotJobs().get(0).getVolumeUuid());
        bus.send(msg1, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                CreateVolumesSnapshotReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                }

                event.setInventories(reply1.getInventories());
                bus.publish(event);
            }
        });
    }

    private void resizeVolume(Message msg, final VolumeVO vvo, ReturnValueCompletion<VolumeInventory> completion) {
        String volumeUuid;
        long resize;
        VolumeType type;

        final ResizeVolumeStruct struct = new ResizeVolumeStruct();
        final Boolean[] hasTakeSnapshot = new Boolean[1];
        final AtomicBoolean force = new AtomicBoolean(false);

        String accountUuid;
        if (msg instanceof APIResizeDataVolumeMsg) {
            volumeUuid = ((APIResizeDataVolumeMsg) msg).getUuid();
            resize = ((APIResizeDataVolumeMsg) msg).getSize();
            type = VolumeType.Data;
            accountUuid = ((APIResizeDataVolumeMsg) msg).getSession().getAccountUuid();
        } else if (msg instanceof APIResizeRootVolumeMsg) {
            volumeUuid = ((APIResizeRootVolumeMsg) msg).getUuid();
            resize = ((APIResizeRootVolumeMsg) msg).getSize();
            type = VolumeType.Root;
            accountUuid = ((APIResizeRootVolumeMsg) msg).getSession().getAccountUuid();
        } else if (msg instanceof ResizeVolumeMsg) {
            volumeUuid = ((ResizeVolumeMsg) msg).getVolumeUuid();
            resize = ((ResizeVolumeMsg) msg).getSize();
            hasTakeSnapshot[0] = ((ResizeVolumeMsg) msg).getTakeSnapshot();
            force.set(((ResizeVolumeMsg) msg).isForce());
            type = vvo.getType();
            accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID;
        } else {
            throw new CloudRuntimeException(String.format("It's not supposed to be here, error msg : [%s]", msg.getClass().getSimpleName()));
        }

        if (vvo.getStatus().equals(VolumeStatus.NotInstantiated) && type.equals(VolumeType.Data)){
            vvo.setSize(resize);
            VolumeVO _vo = dbf.updateAndRefresh(vvo);
            completion.success(VolumeInventory.valueOf(_vo));
            return;
        }

        if (vvo.getVmInstanceUuid() != null) {
            boolean ret = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vvo.getVmInstanceUuid())
                    .eq(VmInstanceVO_.state, VmInstanceState.Running).isExists();
            struct.setVmRunning(ret);
            struct.setVmInstanceUuid(vvo.getVmInstanceUuid());

            if (ret) {
                String vmHostUuid = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.hostUuid)
                        .eq(VmInstanceVO_.uuid, vvo.getVmInstanceUuid()).findValue();

                struct.setVmHostUuid(vmHostUuid);
            }
        }

        String primaryStorageUuid = vvo.getPrimaryStorageUuid();
        final long increaseSize = resize - vvo.getSize();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("resize-%s-volume-%s", type, vvo.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    boolean success = false;
                    String allocatedInstall;

                    @Override
                    public boolean skip(Map data) {
                        return increaseSize <= 0;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(primaryStorageUuid);
                        amsg.setSize(increaseSize);

                        String hostUuid = Q.New(LocalStorageResourceRefVO.class)
                                .select(LocalStorageResourceRefVO_.hostUuid)
                                .eq(LocalStorageResourceRefVO_.resourceUuid, volumeUuid)
                                .findValue();
                        if (hostUuid != null) {
                            amsg.setRequiredHostUuid(hostUuid);
                            amsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
                        }
                        amsg.setRequiredInstallUri(String.format("volume://%s", volumeUuid));

                        // Do not remove this line
                        // Total size is offered for primary storage physical capacity checking
                        amsg.setTotalSize(resize);

                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstall = ar.getAllocatedInstallUrl();
                                success = true;
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setAllocatedInstallUrl(allocatedInstall);
                            rmsg.setPrimaryStorageUuid(primaryStorageUuid);
                            rmsg.setDiskSize(increaseSize);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                            bus.send(rmsg);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-snapshot-for-volume-" + vvo.getUuid();

                    @Override
                    public boolean skip(Map data) {
                        if (hasTakeSnapshot[0] == null) {
                            // per-request config not set, use global config.
                            boolean autoCreate = VolumeGlobalConfig.AUTO_SNAPSHOT_BEFORE_CHANGE_OPERATION.value(Boolean.class);
                            if (!autoCreate) {
                                return true;
                            }
                        } else if (!hasTakeSnapshot[0]) {
                            return true;
                        }

                        List<ResizeVolumeExtensionPoint> exts = pluginRgty.getExtensionList(ResizeVolumeExtensionPoint.class);
                        for (ResizeVolumeExtensionPoint ext: exts) {
                            if (!ext.snapshotBeforeResizeVolume(vvo.getInstallPath(), vvo.getType())) {
                                return true;
                            }
                        }

                        return false;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        //see http://dev.zstack.io/browse/ZSTAC-8317
                        cmsg.setName(String.format("snapshot-auto-created-%s", LocalDateTime.now().toString()));
                        cmsg.setDescription(i18n("this snapshot recording the volume state before resize to %fG is created automatically", SizeUnit.BYTE.toGigaByte((double) resize)));
                        cmsg.setAccountUuid(accountUuid);
                        cmsg.setVolumeUuid(vvo.getUuid());
                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CreateVolumeSnapshotReply createSnapshotReply = reply.castReply();
                                VolumeSnapshotInventory snapshot = createSnapshotReply.getInventory();
                                if (snapshot != null) {
                                    SystemTagCreator creator = VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.newSystemTagCreator(snapshot.getUuid());
                                    creator.inherent = false;
                                    creator.recreate = false;
                                    creator.create();

                                    SnapshotCanonicalEvents.InnerVolumeSnapshotCreated event = new SnapshotCanonicalEvents.InnerVolumeSnapshotCreated(vvo.getUuid(), vvo.getPrimaryStorageUuid(), snapshot);
                                    event.fire();
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "excute-extension-point";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<ResizeVolumeExtensionPoint> exts = pluginRgty.getExtensionList(ResizeVolumeExtensionPoint.class);
                        if (exts == null || exts.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        VolumeVO volumeAfterSnapshot = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vvo.getUuid()).find();
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(exts).each((ext, completion1) -> {
                            ext.beforeResizeVolume(volumeAfterSnapshot, resize, type, struct, new Completion(completion1) {
                                @Override
                                public void success() {
                                    completion1.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    completion1.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errList.getCauses() == null || errList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                VolumeProtocolCapability capability = VolumeProtocolCapability.get(self.getProtocol(), KVMConstant.KVM_HYPERVISOR_TYPE);
                boolean supportOnlineResize = capability == null || capability.isSupportResizeOnHypervisor();
                if (struct.isVmRunning() && supportOnlineResize) {
                    // if vm is running we need host to resize it
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("resize-%s-volume", type);

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            refreshVO();

                            ResizeVolumeOnKvmMsg rmsg = new ResizeVolumeOnKvmMsg();
                            rmsg.setVolume(VolumeInventory.valueOf(self));
                            rmsg.setHostUuid(struct.getVmHostUuid());
                            rmsg.setVmInstanceUuid(struct.getVmInstanceUuid());
                            // qemu-monitor block resize using gigabyte as size unit
                            rmsg.setSize(resize);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, HostConstant.SERVICE_ID, struct.getVmHostUuid());

                            ResizeVolumeVmOverlayMsg resizeVolumeOverlayMsg = new ResizeVolumeVmOverlayMsg();
                            resizeVolumeOverlayMsg.setVmInstanceUuid(struct.getVmInstanceUuid());
                            bus.makeTargetServiceIdByResourceUuid(resizeVolumeOverlayMsg, VmInstanceConstant.SERVICE_ID, struct.getVmInstanceUuid());
                            resizeVolumeOverlayMsg.setMessage(rmsg);

                            bus.send(resizeVolumeOverlayMsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }

                                    self = dbf.reload(self);
                                    self.setSize(resize);
                                    dbf.update(self);
                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("sync-volume-%s-size-after-resize", volumeUuid);

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                            msg.setVolumeUuid(volumeUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, primaryStorageUuid);
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to sync volume[%s] size, because %s", volumeUuid, reply.getError()));
                                        trigger.next();
                                        return;
                                    }

                                    SyncVolumeSizeReply r = reply.castReply();
                                    self.setSize(r.getSize());
                                    self.setActualSize(r.getActualSize());
                                    self = dbf.updateAndRefresh(self);
                                    trigger.next();
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("resize-%s-volume", type);
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            refreshVO();

                            ResizeVolumeOnPrimaryStorageMsg rmsg = new ResizeVolumeOnPrimaryStorageMsg();
                            rmsg.setVolume(VolumeInventory.valueOf(self));
                            rmsg.setSize(resize);
                            rmsg.setForce(force.get());
                            rmsg.setPrimaryStorageUuid(primaryStorageUuid);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                            NeedReplyMessage message = rmsg;

                            // if volume is initialized and attached to vm use overlay message
                            if (struct.getVmInstanceUuid() != null) {
                                ResizeVolumeVmOverlayMsg overlayMsg = new ResizeVolumeVmOverlayMsg();
                                overlayMsg.setVmInstanceUuid(struct.getVmInstanceUuid());
                                overlayMsg.setMessage(rmsg);
                                bus.makeTargetServiceIdByResourceUuid(overlayMsg, VmInstanceConstant.SERVICE_ID, struct.getVmInstanceUuid());
                                message = overlayMsg;
                            }
                            
                            bus.send(message, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    ResizeVolumeOnPrimaryStorageReply r = reply.castReply();
                                    self.setSize(r.getVolume().getSize());
                                    self = dbf.updateAndRefresh(self);
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        new FireVolumeCanonicalEvent().fireVolumeConfigChangedEvent(getSelfInventory(), acntMgr.getOwnerAccountUuidOfResource(self.getUuid()));
                        completion.success(getSelfInventory());
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIResizeDataVolumeMsg msg) {
        APIResizeDataVolumeEvent event = new APIResizeDataVolumeEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final VolumeVO vvo = dbf.findByUuid(msg.getUuid(), VolumeVO.class);
                resizeVolume(msg, vvo, new ReturnValueCompletion<VolumeInventory>(chain) {
                    @Override
                    public void success(VolumeInventory returnValue) {
                        event.setInventory(returnValue);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("resize-volume-%s", msg.getUuid());
            }
        });
    }

    private void handle(final APIResizeRootVolumeMsg msg) {
        APIResizeRootVolumeEvent event = new APIResizeRootVolumeEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final VolumeVO vvo = dbf.findByUuid(msg.getUuid(), VolumeVO.class);
                resizeVolume(msg, vvo, new ReturnValueCompletion<VolumeInventory>(null) {
                    @Override
                    public void success(VolumeInventory returnValue) {
                        event.setInventory(returnValue);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("resize-volume-%s", msg.getUuid());
            }
        });
    }

    private void handle(final APIDeleteVolumeQosMsg msg) {
        APIDeleteVolumeQosEvent evt = new APIDeleteVolumeQosEvent(msg.getId());
        self = dbf.reload(self);
        VolumeQosMode mode = VolumeQosMode.getQosMode(msg.getMode());
        VolumeQos newQos = VolumeQosHelper.deleteQosByMode(VolumeQosHelper.getVolumeQos(self.getVolumeQos()), mode);
        self.setVolumeQos(VolumeQosHelper.getVolumeQosString(newQos));

        if (!acntMgr.isAdmin(msg.getSession())) {
            boolean allowed = true;
            ErrorCode qosErr = null;

            switch (mode) {
                case OVERWRITE:
                    qosErr = doCheckNonAdminQosLimit(BANDWIDTH, null, null, null);
                    if (qosErr != null) {
                        qosErr = doCheckNonAdminQosLimit(IOPS, null, null, null);
                    }
                    break;
                case TOTAL:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN, -1L);
                    break;
                case READ:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN, -1L);
                    break;
                case WRITE:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN, -1L);
                    break;
                case ALL:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN, -1L)
                            && volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN, -1L)
                            && volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN, -1L);
                    break;
                default:
                    evt.setError(argerr("invalid volume qos mode: %s", msg.getMode()));
                    bus.publish(evt);
                    return;
            }

            if (qosErr != null) {
                evt.setError(qosErr);
                bus.publish(evt);
                return;
            }

            if (!allowed) {
                evt.setError(argerr("DeleteVolumeQos [%s] ignore because of account privilege.", msg.getUuid()));
                bus.publish(evt);
                return;
            }
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteVolumeQos(msg, new ReturnValueCompletion<VolumeInventory>(msg) {
                    @Override
                    public void success(VolumeInventory returnValue) {
                        evt.setInventory(returnValue);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-%s-qos", msg.getUuid());
            }
        });
    }

    private void deleteVolumeQosOnPrimaryStorage(final APIDeleteVolumeQosMsg msg, ReturnValueCompletion<VolumeInventory> completion) {
        DeleteVolumeQosOnPrimaryStorageMsg hmsg = new DeleteVolumeQosOnPrimaryStorageMsg();
        hmsg.setVolumeUuid(self.getUuid());
        hmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());

        bus.makeTargetServiceIdByResourceUuid(hmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(hmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    self = dbf.updateAndRefresh(self);
                    completion.success(VolumeInventory.valueOf(self));
                }
            }
        });
    }

    private void deleteVolumeQos(final APIDeleteVolumeQosMsg msg, final ReturnValueCompletion<VolumeInventory> completion) {
        self = dbf.reload(self);
        VolumeQos newQos = VolumeQosHelper.deleteQosByMode(VolumeQosHelper.getVolumeQos(self.getVolumeQos()), VolumeQosMode.getQosMode(msg.getMode()));
        self.setVolumeQos(VolumeQosHelper.getVolumeQosString(newQos));

        if (self.getProtocol() != null) {
            VolumeProtocolCapability cap = VolumeProtocolCapability.get(self.getProtocol(), KVMConstant.KVM_HYPERVISOR_TYPE);
            if (cap != null && (!cap.isSupportQosOnHypervisor())) {
                deleteVolumeQosOnPrimaryStorage(msg, completion);
                return;
            }
        }

        if (self.getVmInstanceUuid() != null && !self.isShareable()) {
            VmInstanceVO ivo = dbf.findByUuid(self.getVmInstanceUuid(), VmInstanceVO.class);
            if (ivo.getHostUuid() != null && (ivo.getState() == VmInstanceState.Running || ivo.getState() == VmInstanceState.Paused)) {
                logger.debug(String.format("Start delete volume qos on kvm, volume: %s, host: %s, mode: %s",
                        self.getUuid(), ivo.getHostUuid(), msg.getMode()));
                DeleteVolumeQosOnKVMHostMsg hmsg = new DeleteVolumeQosOnKVMHostMsg();
                hmsg.setVolume(VolumeInventory.valueOf(self));
                hmsg.setVmUuid(self.getVmInstanceUuid());
                hmsg.setHostUuid(ivo.getHostUuid());
                hmsg.setMode(msg.getMode());

                bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
                bus.send(hmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                        } else {
                            self = dbf.updateAndRefresh(self);
                            completion.success(VolumeInventory.valueOf(self));
                        }
                    }
                });
                return;
            } else if (ivo.getHostUuid() != null) {
                throw new OperationFailureException(operr("Cannot delete vm's volume qos on host %s, because the current vm is in state of %s," +
                        " but support expect states are [%s, %s]", ivo.getHostUuid(), ivo.getState(), VmInstanceState.Running.toString(), VmInstanceState.Stopped.toString()));
            }
        }

        self = dbf.updateAndRefresh(self);
        completion.success(VolumeInventory.valueOf(self));
    }

    private boolean isAnyQosOnDifferentMode(String token, String volumeUuid) {
        // if mode total on offering, user cannot set to read/write
        if (MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN.equals(token)) {
            String read = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTokenByResourceUuid(volumeUuid, MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN);
            String write = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTokenByResourceUuid(volumeUuid, MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN);
            return NumberUtils.isNumber(read) || NumberUtils.isNumber(write);
        } else {
            String total = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTokenByResourceUuid(volumeUuid, MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN);
            return NumberUtils.isNumber(total);
        }
    }

    private boolean volumeQosIsAllowedByNonAdminAccount(String token, Long newBandWidth) {
        String bandWidth = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTokenByResourceUuid(self.getUuid(), VolumeVO.class, token);
        if (!NumberUtils.isNumber(bandWidth) || bandWidth.equals("-1")) {
            return !isAnyQosOnDifferentMode(token, self.getUuid());
        }

        if (newBandWidth == -1L) {
            return false;
        }

        return newBandWidth <= Long.parseLong(bandWidth);
    }

    private void setVolumeQosOnPrimaryStorage(final SetVolumeQosMsg msg, Completion completion) {
        SetVolumeQosOnPrimaryStorageMsg hmsg = new SetVolumeQosOnPrimaryStorageMsg();
        if (msg.getVersion() == 1) {
            completion.fail(argerr("SetVolumeQosMsg version 1 is deprecated, please use version 2"));
            return;
        }

        hmsg.setMode(VOLUME_QOS_MODE_OVERWRITE);
        hmsg.setReadBandwidth(msg.getReadBandwidth());
        hmsg.setWriteBandwidth(msg.getWriteBandwidth());
        hmsg.setTotalBandWidth(msg.getTotalBandwidth());
        hmsg.setReadIOPS(msg.getReadIOPS());
        hmsg.setWriteIOPS(msg.getWriteIOPS());
        hmsg.setTotalIOPS(msg.getTotalIOPS());
        hmsg.setVolumeUuid(self.getUuid());
        hmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());

        bus.makeTargetServiceIdByResourceUuid(hmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(hmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void setVolumeQosOnKvm(final SetVolumeQosMsg msg, String hostUuid, Completion completion) {
        SetVolumeQosOnKVMHostMsg hmsg = new SetVolumeQosOnKVMHostMsg();
        hmsg.setInstallPath(self.getInstallPath());
        hmsg.setVmUuid(self.getVmInstanceUuid());
        hmsg.setVolume(VolumeInventory.valueOf(self));
        hmsg.setHostUuid(hostUuid);

        if (msg.getVersion() == 1) {
            String mode = msg.getMode();
            Long bandwidth = msg.getVolumeBandwidth();
            logger.debug(
                    String.format("Start set volume qos on volume: %s, host: %s, mode: %s", self.getUuid(), hostUuid,
                            mode
                    ));
            hmsg.setMode(mode);
            VolumeQosMode qosMode = VolumeQosMode.getQosMode(mode);
            switch (qosMode) {
                case ALL:
                    // APISetVolumeQosMsg only support 1 bandwidth, so we do this
                    hmsg.setReadBandwidth(bandwidth);
                    hmsg.setWriteBandwidth(bandwidth);
                    break;
                case READ:
                    hmsg.setReadBandwidth(bandwidth);
                    break;
                case WRITE:
                    hmsg.setWriteBandwidth(bandwidth);
                    break;
                case TOTAL:
                default:
                    hmsg.setTotalBandWidth(bandwidth);
            }
        } else {
            hmsg.setMode(VOLUME_QOS_MODE_OVERWRITE);
            hmsg.setReadBandwidth(msg.getReadBandwidth() != null ? msg.getReadBandwidth() : 0L);
            hmsg.setWriteBandwidth(msg.getWriteBandwidth() != null ? msg.getWriteBandwidth() : 0L);
            hmsg.setTotalBandWidth(msg.getTotalBandwidth() != null ? msg.getTotalBandwidth() : 0L);
            hmsg.setReadIOPS(msg.getReadIOPS() != null ? msg.getReadIOPS() : 0L);
            hmsg.setWriteIOPS(msg.getWriteIOPS() != null ? msg.getWriteIOPS() : 0L);
            hmsg.setTotalIOPS(msg.getTotalIOPS() != null ? msg.getTotalIOPS() : 0L);
        }

        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
        bus.send(hmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void setVolumeQos(final SetVolumeQosMsg msg, final Completion completion) {
        self = dbf.reload(self);
        if (msg.getVersion() == 1) {
            VolumeQos oldQos = VolumeQosHelper.getVolumeQos(self.getVolumeQos());
            VolumeQos newQos = VolumeQosHelper.getVolumeQos(msg.getVolumeBandwidth(), VolumeQosMode.getQosMode(msg.getMode()));
            self.setVolumeQos(VolumeQosHelper.mergeVolumeQosAndGetString(oldQos, newQos));
        } else {
            VolumeQos newQos = new VolumeQos(msg.getReadBandwidth(), msg.getWriteBandwidth(), msg.getTotalBandwidth(),
                    msg.getReadIOPS(), msg.getWriteIOPS(), msg.getTotalIOPS()
            );
            self.setVolumeQos(VolumeQosHelper.getVolumeQosString(newQos));
        }

        if (self.getProtocol() != null) {
            VolumeProtocolCapability cap = VolumeProtocolCapability.get(self.getProtocol(), KVMConstant.KVM_HYPERVISOR_TYPE);
            BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class).eq(BlockVolumeVO_.uuid, self.getUuid()).find();
            if (cap != null && (!cap.isSupportQosOnHypervisor() || blockVolumeVO != null)) {
                setVolumeQosOnPrimaryStorage(msg, completion);
                return;
            }
        }

        if (self.getVmInstanceUuid() != null && !self.isShareable()) {
            VmInstanceVO ivo = dbf.findByUuid(self.getVmInstanceUuid(), VmInstanceVO.class);
            if (ivo.getHostUuid() != null && (ivo.getState() == VmInstanceState.Running || ivo.getState() == VmInstanceState.Paused)) {
                setVolumeQosOnKvm(msg, ivo.getHostUuid(), completion);
            } else {
                completion.success();
            }
        } else {
            completion.success();
        }
    }

    private ErrorCode checkNonAdminAccountQosLimit(final APISetVolumeQosMsg msg) {
        if (msg.getVersion() == 1) {
            boolean allowed = false;
            VolumeQosMode mode = VolumeQosMode.getQosMode(msg.getMode());
            switch (mode) {
                case TOTAL:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN,
                            msg.getVolumeBandwidth()
                    );
                    break;
                case READ:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN,
                            msg.getVolumeBandwidth()
                    );
                    break;
                case WRITE:
                    allowed = volumeQosIsAllowedByNonAdminAccount(MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN,
                            msg.getVolumeBandwidth()
                    );
                    break;
            }
            return allowed ? null : argerr("non admin account cannot set bandwidth more than %s",
                    VolumeQosHelper.getVolumeQosByMode(self.getVolumeQos(), mode)
            );
        } else if (msg.getVersion() == 2) {
            ErrorCode bandwidthErr = doCheckNonAdminQosLimit(BANDWIDTH, msg.getTotalBandwidth(),
                    msg.getReadBandwidth(), msg.getWriteBandwidth());
            if (bandwidthErr != null) {
                return bandwidthErr;
            }
            return doCheckNonAdminQosLimit(IOPS, msg.getTotalIOPS(),
                    msg.getReadIOPS(), msg.getWriteIOPS());
        }
        return argerr("unknown message version.");
    }

    private ErrorCode doCheckNonAdminQosLimit(VolumeQosType limitType,  Long total, Long read, Long write) {
        PatternedSystemTag upThresholdTag;
        String readToken, writeToken, totalToken;
        if (IOPS == limitType) {
            upThresholdTag = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD;
            readToken = MevocoVolumeSystemTags.VOLUME_READ_IOPS_TOKEN;
            writeToken = MevocoVolumeSystemTags.VOLUME_WRITE_IOPS_TOKEN;
            totalToken = MevocoVolumeSystemTags.VOLUME_TOTAL_IOPS_TOKEN;
        } else if (BANDWIDTH == limitType) {
            upThresholdTag = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD;
            readToken = MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN;
            writeToken = MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN;
            totalToken = MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN;
        } else {
            return argerr("unknown qos limit type.");
        }

        Map<String, String> tagMap = upThresholdTag.getTokensByResourceUuid(self.getUuid());
        if (tagMap == null) {
            return null;
        }

        String readLimit = tagMap.get(readToken);
        String writeLimit = tagMap.get(writeToken);
        String totalLimit = tagMap.get(totalToken);

        if (NumberUtils.isNumber(totalLimit)) {
            if (read != null || write != null) {
                return argerr("Non-admin account is only allowed to set the total %s limit.", limitType);
            }

            long totalValue = Long.parseLong(totalLimit);
            if (totalValue == -1) {
                return null;
            }

            if (total == null) {
                return argerr("Non-admin account cannot set the total %s limits as unlimited.",
                        limitType.getType());
            }

            return totalValue >= total ? null : argerr(
                    "Non-admin account cannot set the total %s limit greater than: %s",
                    limitType.getType(), totalLimit);
        }

        if (NumberUtils.isNumber(readLimit) || NumberUtils.isNumber(writeLimit)) {
            if (total != null) {
                return argerr("Non-admin account is only allowed to set the read/write %s limits.",
                        limitType.getType());
            }
            boolean readAllowed = true, writeAllowed = true;
            if (NumberUtils.isNumber(readLimit)) {
                if (read == null) {
                    return argerr("Non-admin account cannot set the read %s limits as unlimited.",
                            limitType.getType());
                }
                long readValue = Long.parseLong(readLimit);
                if (readValue != -1) {
                    readAllowed = readValue >= read;
                }
            }

            if (NumberUtils.isNumber(writeLimit)) {
                if (write == null) {
                    return argerr("Non-admin account cannot set the write %s limits as unlimited.",
                            limitType.getType());
                }
                long writeValue = Long.parseLong(writeLimit);
                if (writeValue != -1) {
                    writeAllowed = Long.parseLong(writeLimit) >= write;
                }
            }

            return readAllowed && writeAllowed ? null : argerr(
                    "Non-admin account cannot set the read/write %s limits greater than: %s/%s",
                    limitType.getType(), readLimit, writeLimit
            );
        }

        return null;
    }

    private void handle(final SetVolumeQosMsg msg) {
        SetVolumeQosReply reply = new SetVolumeQosReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                setVolumeQos(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        self = dbf.updateAndRefresh(self);
                        reply.setInventory(VolumeInventory.valueOf(self));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("set-volume-%s-qos", msg.getUuid());
            }
        });
    }

    private void handle(final APISetVolumeQosMsg msg) {
        APISetVolumeQosEvent evt = new APISetVolumeQosEvent(msg.getId());

        self = dbf.reload(self);
        if (!acntMgr.isAdmin(msg.getSession())) {
            ErrorCode err = checkNonAdminAccountQosLimit(msg);

            if (err != null) {
                evt.setError(err);
                bus.publish(evt);
                return;
            }
        }

        SetVolumeQosMsg smsg = new SetVolumeQosMsg();
        smsg.setUuid(msg.getUuid());
        smsg.setMode(msg.getMode());
        smsg.setReadBandwidth(msg.getReadBandwidth());
        smsg.setWriteBandwidth(msg.getWriteBandwidth());
        smsg.setReadIOPS(msg.getReadIOPS());
        smsg.setWriteIOPS(msg.getWriteIOPS());
        smsg.setVolumeBandwidth(msg.getVolumeBandwidth());
        smsg.setVersion(msg.getVersion());
        smsg.setTotalBandwidth(msg.getTotalBandwidth());
        smsg.setTotalIOPS(msg.getTotalIOPS());
        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, smsg.getUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }
                self = dbf.reload(self);
                evt.setInventory(VolumeInventory.valueOf(self));
                bus.publish(evt);
            }
        });
    }

    private void getVolumeQosFromHost(final ReturnValueCompletion<VolumeQos> completion) {
        if (self.getVmInstanceUuid() == null) {
            completion.fail(operr("volume [%s] isn't attached to any vm, cannot get qos by forceSync", self.getUuid()));
            return;
        }

        VmInstanceVO vm = dbf.findByUuid(self.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm == null) {
            completion.fail(operr("volume [%s] isn't attached to any vm (or vm is not existed now), cannot sync volume qos", self.getUuid()));
            return;
        }

        if (vm.getState() != VmInstanceState.Running && vm.getState() != VmInstanceState.Paused) {
            completion.fail(operr("vm [%s]' state must be Running or Paused to sync volume qos", vm.getUuid()));
            return;
        }

        if (vm.getHostUuid() == null) {
            completion.fail(operr("vm [%s]'s HostUuid is null, cannot sync volume qos"));
            return;
        }

        GetVolumeQosOnKVMHostMsg gmsg = new GetVolumeQosOnKVMHostMsg();
        gmsg.setHostUuid(vm.getHostUuid());
        gmsg.setVmUuid(vm.getUuid());
        gmsg.setVolume(VolumeInventory.valueOf(self));

        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    GetVolumeQosOnKVMHostReply r = reply.castReply();
                    VolumeQos syncQos = VolumeQosHelper.getVolumeQos(r.getVolumeBandwidth(), r.getVolumeBandwidthWrite(),
                            r.getVolumeBandwidthRead(), r.getIopsTotal(), r.getIopsWrite(), r.getIopsRead());
                    if (!VolumeQosHelper.getVolumeQos(self.getVolumeQos()).equals(syncQos)) {
                        self.setVolumeQos(VolumeQosHelper.getVolumeQosString(syncQos));
                        dbf.updateAndRefresh(self);
                    }
                    completion.success(syncQos);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });

    }

    private void setUpthreshold(final APIGetVolumeQosMsg msg, final APIGetVolumeQosReply reply) {
        if (!acntMgr.isAdmin(msg.getSession())) {
            Map<String, String> bandwidthUpThresholds =
                    MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTokensByResourceUuid(
                    self.getUuid());
            Map<String, String> iopsUpThresholds =
                    MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.getTokensByResourceUuid(
                            self.getUuid());

            String bandWidthUpthreshold = null, bandWidthReadUpthreshold = null, bandWidthWriteUpthreshold = null;
            if (bandwidthUpThresholds != null) {
                bandWidthUpthreshold = bandwidthUpThresholds.get(
                        MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN);
                bandWidthReadUpthreshold= bandwidthUpThresholds.get(
                        MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN);
                bandWidthWriteUpthreshold = bandwidthUpThresholds.get(
                        MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN);
            }
            String iopsTotalUpThreshold = null, iopsReadUpThreshold = null, iopsWriteUpThreshold = null;
            if (iopsUpThresholds != null) {
                iopsTotalUpThreshold = iopsUpThresholds.get(MevocoVolumeSystemTags.VOLUME_TOTAL_IOPS_TOKEN);
                iopsReadUpThreshold = iopsUpThresholds.get(MevocoVolumeSystemTags.VOLUME_READ_IOPS_TOKEN);
                iopsWriteUpThreshold = iopsUpThresholds.get(MevocoVolumeSystemTags.VOLUME_WRITE_IOPS_TOKEN);
            }

            if (NumberUtils.isNumber(bandWidthUpthreshold)) {
                reply.setVolumeBandwidthUpthreshold(Long.parseLong(bandWidthUpthreshold));
            }
            if (NumberUtils.isNumber(bandWidthReadUpthreshold)) {
                reply.setVolumeBandwidthReadUpthreshold(Long.parseLong(bandWidthReadUpthreshold));
            }
            if (NumberUtils.isNumber(bandWidthWriteUpthreshold)) {
                reply.setVolumeBandwidthWriteUpthreshold(Long.parseLong(bandWidthWriteUpthreshold));
            }
            if (NumberUtils.isNumber(iopsTotalUpThreshold)) {
                reply.setIopsTotalUpthreshold(Long.parseLong(iopsTotalUpThreshold));
            }
            if (NumberUtils.isNumber(iopsReadUpThreshold)) {
                reply.setIopsReadUpthreshold(Long.parseLong(iopsReadUpThreshold));
            }
            if (NumberUtils.isNumber(iopsWriteUpThreshold)) {
                reply.setIopsWriteUpthreshold(Long.parseLong(iopsWriteUpThreshold));
            }
        }
    }

    private void handle(final APIGetVolumeQosMsg msg) {
        APIGetVolumeQosReply reply = new APIGetVolumeQosReply();
        reply.setVolumeUuid(msg.getUuid());
        setUpthreshold(msg, reply);

        if (msg.getForceSync()) {
            getVolumeQosFromHost(new ReturnValueCompletion<VolumeQos>(msg) {
                @Override
                public void success(VolumeQos bandWidths) {
                    reply.setVolumeBandwidth(bandWidths.getTotalBandwidth());
                    reply.setVolumeBandwidthWrite(bandWidths.getWriteBandwidth());
                    reply.setVolumeBandwidthRead(bandWidths.getReadBandwidth());
                    reply.setIopsTotal(bandWidths.getTotalIOPS());
                    reply.setIopsRead(bandWidths.getReadIOPS());
                    reply.setIopsWrite(bandWidths.getWriteIOPS());
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            VolumeQos qos = VolumeQosHelper.getVolumeQos(self.getVolumeQos());
            reply.setVolumeBandwidthRead(qos.getReadBandwidth());
            reply.setVolumeBandwidthWrite(qos.getWriteBandwidth());
            reply.setVolumeBandwidth(qos.getTotalBandwidth());
            reply.setIopsTotal(qos.getTotalIOPS());
            reply.setIopsRead(qos.getReadIOPS());
            reply.setIopsWrite(qos.getWriteIOPS());
            bus.reply(msg, reply);
        }
    }

    private void callSuper(Message msg) {
        super.handleMessage(msg);
    }

    private void handle(final VolumeDeletionMsg msg) {
        VolumeVO volumeVO = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        VolumeInventory volumeInventory = VolumeInventory.valueOf(volumeVO);
        if (!volumeVO.isShareable()) {
            callSuper(msg);
            return;
        }

        List<String> vmUuids = GetVolumeAttachedVmUuids(VolumeInventory.valueOf(volumeVO));
        new LoopAsyncBatch<String>(msg) {

            @Override
            protected Collection<String> collect() {
                return vmUuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String vmUuid) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        DetachDataVolumeFromVmMsg msg = new DetachDataVolumeFromVmMsg();
                        msg.setVolume(volumeInventory);
                        msg.setVmInstanceUuid(vmUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                        bus.send(msg, new CloudBusCallBack(completion) {

                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    errors.add(operr("failed to detach shareable volume[uuid:%s] from VmInstance[uuid:%s]", msg.getVolume().getUuid(), msg.getVmInstanceUuid()));
                                }
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                if (!errors.isEmpty()) {
                    throw new OperationFailureException(operr("failed to detach shareable volume from VmInstance:[\n%s]",
                            StringUtils.join(errors, "\n\n")));
                }
                callSuper(msg);
            }

        }.start();
    }


    private List<String> GetVolumeAttachedVmUuids(VolumeInventory volumeInventory) {
        if (volumeInventory.isShareable()) {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            q.select(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid);
            q.add(ShareableVolumeVmInstanceRefVO_.volumeUuid, SimpleQuery.Op.EQ, volumeInventory.getUuid());
            return q.listValue();
        } else {
            return Arrays.asList(volumeInventory.getVmInstanceUuid());
        }
    }
}
