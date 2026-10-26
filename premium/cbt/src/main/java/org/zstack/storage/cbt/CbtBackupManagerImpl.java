package org.zstack.storage.cbt;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.cbt.*;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupOperationConflictChecker;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.VolumeTO;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageSystemTags;
import org.zstack.storage.backup.BackupOperationConflictManager;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.backup.BackupOperationConflictManager.sendBackupInVmQueue;

public class CbtBackupManagerImpl extends AbstractService implements Component, CbtBackupManager, BackupOperationConflictChecker {
    private static final CLogger logger = Utils.getLogger(CbtBackupManagerImpl.class);
    private static final String CBT_HOST_OPERATION_SYNC_SIGNATURE = "cbt-host-operation-on-host-%s";
    private final Map<String, CbtBackupFactory> bkFactories = new ConcurrentHashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private BackupOperationConflictManager conflictManager;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof CbtTaskMessage) {
            passThrough((CbtTaskMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(CbtTaskMessage msg) {
        CbtTaskVO vo = dbf.findByUuid(msg.getCbtTaskUuid(), CbtTaskVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the cbt task[uuid:%s]", msg.getCbtTaskUuid()));
        }

        CbtBackupBase base = new CbtBackupBase(vo);
        base.handleMessage((Message) msg);
    }


    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateCbtTaskMsg) {
            handle((APICreateCbtTaskMsg) msg);
        } else if (msg instanceof APIExportNbdVolumesMsg) {
            handle((APIExportNbdVolumesMsg) msg);
        } else if (msg instanceof APIUnexportNbdVolumesMsg) {
            handle((APIUnexportNbdVolumesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DisableCbtTaskMsg) {
            handle((DisableCbtTaskMsg) msg);
        } else if (msg instanceof CreateCbtTaskMsg) {
            handle((CreateCbtTaskMsg) msg);
        } else if (msg instanceof EnableCbtTaskMsg) {
            handle((EnableCbtTaskMsg) msg);
        } else if (msg instanceof EnableCbtTaskOnHostMsg) {
            handle((EnableCbtTaskOnHostMsg) msg);
        } else if (msg instanceof DeleteCbtTaskMsg) {
            handle((DeleteCbtTaskMsg) msg);
        } else if (msg instanceof UnexportNbdVolumesMsg) {
            handle((UnexportNbdVolumesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIExportNbdVolumesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getVmSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIExportNbdVolumesEvent evt = new APIExportNbdVolumesEvent(msg.getId());
                String hostUuid = getVmHostUuid(msg.getVmInstanceUuid());
                if (hostUuid == null) {
                    evt.setError(operr("host not found for VM: %s", msg.getVmInstanceUuid()));
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                runInCbtHostOperationQueue(hostUuid, msg, String.format("export-nbd-volumes-on-host-%s", hostUuid), new CbtHostOperation() {
                    @Override
                    public void run(SyncTaskChain hostChain) {
                        exportNbdVolumesFlow(msg.isForce(), msg.getVmInstanceUuid(), msg.getVolumeUuids(), new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(msg, hostChain) {
                            @Override
                            public void success(List<VolumeCbtBackupInfo> volumeInfos) {
                                evt.setVolumeInfos(volumeInfos);
                                bus.publish(evt);
                                hostChain.next();
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                evt.setError(errorCode);
                                bus.publish(evt);
                                hostChain.next();
                                chain.next();
                            }
                        });
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("prepare-volumes-for-cbt-recovery-task");
            }
        });
    }

    private void handle(final APIUnexportNbdVolumesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getVmSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIUnexportNbdVolumesEvent evt = new APIUnexportNbdVolumesEvent(msg.getId());
                UnexportNbdVolumesMsg umsg = new UnexportNbdVolumesMsg();
                umsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                umsg.setVolumeUuids(msg.getVolumeUuids());
                bus.makeLocalServiceId(umsg, CbtBackupConstant.SERVICE_ID);
                bus.send(umsg, new CloudBusCallBack(umsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            evt.setError(reply.getError());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("prepare-volumes-for-cbt-recovery-task");
            }
        });
    }

    private void handle(final UnexportNbdVolumesMsg msg) {
        final UnexportNbdVolumesReply reply = new UnexportNbdVolumesReply();
        String hostUuid = getVmHostUuid(msg.getVmInstanceUuid());
        if (hostUuid == null) {
            reply.setError(operr("host not found for VM: %s", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        runInCbtHostOperationQueue(hostUuid, msg, String.format("unexport-nbd-volumes-on-host-%s", hostUuid), new CbtHostOperation() {
            @Override
            public void run(SyncTaskChain hostChain) {
                CbtBackupHypervisorBackend backend = getCbtBackend(msg.getVmInstanceUuid());
                backend.unexportNbdVolumesForRecovery(msg.getVmInstanceUuid(), msg.getVolumeUuids(), new Completion(msg, hostChain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        hostChain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        hostChain.next();
                    }
                });
            }
        });
    }

    private void handle(final APICreateCbtTaskMsg msg) {
        CreateCbtTaskMsg cmsg = new CreateCbtTaskMsg();
        cmsg.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, CbtBackupConstant.SERVICE_ID, cmsg.getVmInstanceUuid());

        sendBackupInVmQueue(bus, cmsg, cmsg.getVmInstanceUuid(), new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APICreateCbtTaskEvent evt = new APICreateCbtTaskEvent(msg.getId());
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    CreateCbtTaskReply r = reply.castReply();
                    evt.setInventory(r.getInventory());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(final CreateCbtTaskMsg msg) {
        ErrorCode error = conflictManager.check(msg.getVmInstanceUuid(), msg.getUuid());
        if (error != null) {
            bus.replyErrorByMessageType(msg, error);
            return;
        }

        doCreateCbtTask(msg, new ReturnValueCompletion<CbtTaskInventory>(msg) {
            @Override
            public void success(CbtTaskInventory inventory) {
                CreateCbtTaskReply reply = new CreateCbtTaskReply();
                reply.setInventory(inventory);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                bus.replyErrorByMessageType(msg, errorCode);
            }
        });
    }

    private void handle(final EnableCbtTaskMsg msg) {
        final CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
        if (taskVO == null) {
            EnableCbtTaskReply reply = new EnableCbtTaskReply();
            reply.setError(operr("Cbt task not found[uuid: %s]", msg.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("cbt-task-%s", msg.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return CbtGlobalConfig.START_CBT_CONCURRENT_LEVEL.value(Integer.class);
            }

            @Override
            public void run(SyncTaskChain chain) {
                String vmUuid = getVmUuidByCbtTaskUuid(msg.getUuid());
                if (vmUuid == null) {
                    EnableCbtTaskReply reply = new EnableCbtTaskReply();
                    reply.setError(operr("CBT task[uuid: %s] has no VM attached", msg.getUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                ErrorCode err = conflictManager.check(vmUuid, msg.getUuid());
                if (err != null) {
                    EnableCbtTaskReply reply = new EnableCbtTaskReply();
                    reply.setError(err);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                EnableCbtTaskOnHostMsg emsg = new EnableCbtTaskOnHostMsg();
                emsg.setBitmapName(msg.getBitmapName());
                emsg.setUuid(msg.getUuid());
                emsg.setAccountUuid(msg.getAccountUuid());
                bus.makeLocalServiceId(emsg, CbtBackupConstant.SERVICE_ID);
                bus.send(emsg, new CloudBusCallBack(emsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            EnableCbtTaskReply ereply = new EnableCbtTaskReply();
                            EnableCbtTaskOnHostReply r = reply.castReply();
                            ereply.setVolumeCbtBackupInfos(r.getVolumeCbtBackupInfos());
                            bus.reply(msg, ereply);
                            chain.next();
                        } else {
                            reply.setError(reply.getError());
                            bus.reply(msg, reply);
                            chain.next();
                        }
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("running-cbt-task-%s", taskVO.getUuid());
            }
        });
    }

    private void handle(final DeleteCbtTaskMsg msg) {
        final DeleteCbtTaskReply reply = new DeleteCbtTaskReply();
        doDeleteCbtTask(msg.getUuid(), msg.isForce(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final EnableCbtTaskOnHostMsg msg) {
        final EnableCbtTaskOnHostReply reply = new EnableCbtTaskOnHostReply();
        final String vmUuid = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(CbtTaskResourceRefVO_.taskUuid, msg.getUuid()).select(CbtTaskResourceRefVO_.resourceUuid)
                .findValue();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("enable-cbt-host-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                ErrorCode err = validateVmStateForOperation(vmUuid, VmInstanceState.Running, "enable cbt task");
                if (err != null) {
                    reply.setError(err);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                String hostUuid = getVmHostUuid(vmUuid);
                if (hostUuid == null) {
                    changeCbtTaskStatus(msg.getUuid(), CbtTaskStatus.Failed);
                    reply.setError(operr("host not found for VM: %s", vmUuid));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
                if (taskVO == null) {
                    reply.setError(operr("Cbt task not found[uuid: %s]", msg.getUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                if (taskVO.getStatus() == CbtTaskStatus.Running) {
                    reply.setError(operr("Cbt task[uuid: %s] has already enabled, please disable it.", msg.getUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                CbtBackupHypervisorBackend backend = getCbtBackend(vmUuid);
                runStartVmInstanceCbtFlow(backend, vmUuid, taskVO, msg.getAccountUuid(), msg.getBitmapName(), new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(msg, chain) {
                    @Override
                    public void success(List<VolumeCbtBackupInfo> infos) {
                        changeCbtTaskStatus(msg.getUuid(), CbtTaskStatus.Running);
                        reply.setVolumeCbtBackupInfos(infos);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        changeCbtTaskStatus(msg.getUuid(), CbtTaskStatus.Failed);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("running-cbt-task-%s", msg.getUuid());
            }
        });
    }

    private interface CbtHostOperation {
        void run(SyncTaskChain chain);
    }

    private void runInCbtHostOperationQueue(final String hostUuid, final AsyncBackup backup, final String name, final CbtHostOperation operation) {
        thdf.chainSubmit(new ChainTask(backup) {
            @Override
            public String getSyncSignature() {
                return String.format(CBT_HOST_OPERATION_SYNC_SIGNATURE, hostUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                operation.run(chain);
            }

            @Override
            public String getName() {
                return name;
            }
        });
    }

    private void doDeleteCbtTask(final String taskUuid, boolean force, Completion completion) {
        final CbtTaskVO taskVO = dbf.findByUuid(taskUuid, CbtTaskVO.class);
        if (taskVO == null) {
            logger.warn(String.format("cannot find CBT task[uuid: %s]", taskUuid));
            completion.success();
            return;
        }

        final CbtTaskResourceRefVO refVO = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .find();
        if (refVO == null) {
            completion.fail(operr(String.format("cannot find the resource of CBT task[uuid: %s]", taskUuid)));
            return;
        }

        final String vmUuid = refVO.getResourceUuid();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-vm-%s-cbt-task", vmUuid));
        chain.then(new NoRollbackFlow() {
            final String __name__ = "disable-cbt-mirror";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DisableCbtTaskMsg dmsg = new DisableCbtTaskMsg();
                dmsg.setUuid(taskUuid);
                dmsg.setForce(force);
                bus.makeTargetServiceIdByResourceUuid(dmsg, CbtBackupConstant.SERVICE_ID, vmUuid);
                bus.send(dmsg, new CloudBusCallBack(dmsg) {
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
            public boolean skip(Map data) {
                return vmNotRunning(vmUuid) || (cbtNotRunning(taskUuid) && !hasCbtCleanupRecords(taskUuid));
            }
        }).then(new NoRollbackFlow() {
            final String __name__ = "destroy-retained-cbt-volumes";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                destroyCbtVolumes(taskUuid, new Completion(trigger) {
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
            final String __name__ = "delete-cbt-data";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        remove(refVO);

                        sql(VolumeCbtBackupRecordVO.class)
                                .eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid)
                                .hardDelete();

                        sql(CbtTaskVO.class)
                                .eq(CbtTaskVO_.uuid, taskUuid)
                                .hardDelete();
                    }
                }.execute();
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void exportNbdVolumesFlow(boolean force,
                                      String vmUuid,
                                      List<String> volumeUuids,
                                      final ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        final List<VolumeCbtBackupInfo>[] volumeInfos = new List[]{new ArrayList<VolumeCbtBackupInfo>()};
        boolean running = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .isExists();
        if (running) {
            completion.fail(operr("vm[uuid: %s] is running, please stop it and try again.", vmUuid));
            return;
        }

        String hostUuid = getVmHostUuid(vmUuid);
        if (hostUuid == null) {
            completion.fail(operr("host not found for VM: %s", vmUuid));
            return;
        }

        CbtBackupHypervisorBackend backend = getCbtBackend(vmUuid);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("start-export-nbd-volumes-for-vm-%s", vmUuid));
        chain.then(new NoRollbackFlow() {
            final String __name__ = "check-exported-volumes";

            @Override
            public boolean skip(Map data) {
                return force;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                backend.listExportedVolumesForRecovery(vmUuid, volumeUuids, new ReturnValueCompletion<Map<String, Boolean>>(trigger) {
                    @Override
                    public void success(Map<String, Boolean> info) {
                        for (Map.Entry<String, Boolean> entry : info.entrySet()) {
                            if (entry.getValue() != null && entry.getValue()) {
                                ErrorCode err = operr("volume[uuid:%s] for vmInstance[uuid: %s] has already been exported.", entry.getKey(), vmUuid);
                                trigger.fail(err);
                            }
                        }
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        chain.then(new NoRollbackFlow() {
            final String __name__ = "unexport-volumes-if-need";

            @Override
            public boolean skip(Map data) {
                return !force;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                backend.unexportNbdVolumesForRecovery(vmUuid, volumeUuids, new Completion(trigger) {
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
        });
        chain.then(new NoRollbackFlow() {
            final String __name__ = "export-nbd-volumes-for-recovery";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                backend.exportNbdVolumesForRecovery(vmUuid, volumeUuids, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(trigger) {
                    @Override
                    public void success(List<VolumeCbtBackupInfo> infos) {
                        volumeInfos[0] = infos;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(volumeInfos[0]);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void runStartVmInstanceCbtFlow(CbtBackupHypervisorBackend backend,
                                           final String vmUuid,
                                           final CbtTaskVO taskVO,
                                           final String accountUuid,
                                           final String bitmapName,
                                           final ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        final List<String> volumeUuids = getVmVolumeUuids(vmUuid);
        List<VolumeCbtBackupInfo> volumeInfos = new ArrayList<VolumeCbtBackupInfo>();
        String hostUuid = getVmHostUuid(vmUuid);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("start-cbt-task-for-vm-%s", vmUuid));
        chain.then(new NoRollbackFlow() {
            final String __name__ = "cleanup-failed-cbt-task-before-retry";

            @Override
            public boolean skip(Map data) {
                return taskVO.getStatus() != CbtTaskStatus.Failed;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // Keep Enable retryable without a separate public Disable call.
                // Retry scratch deletion too when rollback records are already gone.
                DisableCbtTaskMsg dmsg = new DisableCbtTaskMsg();
                dmsg.setUuid(taskVO.getUuid());
                doDisableCbtTask(dmsg, new Completion(trigger) {
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
        });
        chain.then(new NoRollbackFlow() {
            final String __name__ = "sync-volumes-size";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncVolumeSize(volumeUuids, new Completion(trigger) {
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
        });
        chain.then(new Flow() {
            final String __name__ = "prepare-cbt-volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                prepareCbtVolumes(volumeUuids, accountUuid, hostUuid, taskVO.getUuid(), new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(trigger) {
                    @Override
                    public void success(List<VolumeCbtBackupInfo> infos) {
                        volumeInfos.addAll(infos);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (shouldRetainCbtVolumesAfterRollback(taskVO.getUuid())) {
                    logger.warn(String.format("retain cbt[uuid:%s] volumes because runtime cleanup is pending",
                            taskVO.getUuid()));
                    trigger.rollback();
                    return;
                }

                destroyCbtVolumes(taskVO.getUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        logger.warn(String.format("rollback prepare cbt[uuid %s] volume successfully.", taskVO.getUuid()));
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("rollback prepare cbt[uuid %s] volume failed: %s",
                                taskVO.getUuid(), errorCode));
                        trigger.rollback();
                    }
                });
            }
        });
        chain.then(new Flow() {
            final String __name__ = "take-volumes-backup";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                takeVolumeCbtBackupInQueue(backend, vmUuid, hostUuid, volumeInfos, bitmapName, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(trigger) {
                    @Override
                    public void success(List<VolumeCbtBackupInfo> infos) {
                        volumeInfos.clear();
                        volumeInfos.addAll(infos);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                boolean backupTaken = volumeInfos.stream().anyMatch(info -> !Strings.isNullOrEmpty(info.getScratchNodeName()));
                if (!backupTaken) {
                    trigger.rollback();
                    return;
                }

                List<VolumeCbtBackupRecordVO> rollbackRecords = buildVolumeCbtBackupRecords(
                        taskVO.getUuid(), bitmapName, volumeInfos, true);
                dbf.persistCollection(rollbackRecords);

                backend.rollbackVolumeCbtBackup(vmUuid, hostUuid, bitmapName, volumeInfos, new Completion(trigger) {
                    @Override
                    public void success() {
                        dbf.removeCollection(rollbackRecords, VolumeCbtBackupRecordVO.class);
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("rollback cbt[uuid:%s] backup for vm[uuid:%s] on host[uuid:%s] failed, continue rollback. %s",
                                taskVO.getUuid(), vmUuid, hostUuid, errorCode));
                        trigger.rollback();
                    }
                });
            }
        });
        chain.then(new NoRollbackFlow() {
            final String __name__ = "get-volumes-backup-bitmap";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                backend.getVolumesBitmap(volumeInfos, hostUuid, bitmapName, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(trigger) {
                    @Override
                    public void success(List<VolumeCbtBackupInfo> infos) {
                        volumeInfos.clear();
                        volumeInfos.addAll(infos);
                        persistVolumeCbtBackupRecord(taskVO.getUuid(), bitmapName, volumeInfos, new Completion(trigger) {
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

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(volumeInfos);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    protected void takeVolumeCbtBackupInQueue(CbtBackupHypervisorBackend backend,
                                              String vmUuid,
                                              String hostUuid,
                                              List<VolumeCbtBackupInfo> volumeInfos,
                                              String bitmapName,
                                              ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format(CBT_HOST_OPERATION_SYNC_SIGNATURE, hostUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                backend.takeVolumeCbtBackup(vmUuid, hostUuid, volumeInfos, bitmapName, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(completion) {
                    @Override
                    public void success(List<VolumeCbtBackupInfo> infos) {
                        completion.success(infos);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("take-vm-%s-cbt-backup-on-host-%s", vmUuid, hostUuid);
            }
        });
    }

    private void handle(final DisableCbtTaskMsg msg) {
        String vmUuid = getVmUuidByCbtTaskUuid(msg.getUuid());
        // VM state-change callbacks hold the VM queue while waiting for forced disable.
        final String syncSignature = msg.isFromVmStateChange() || vmUuid == null
                ? String.format("disable-cbt-task-%s", msg.getUuid()) : getVmSyncSignature(vmUuid);
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDisableCbtTask(msg, new Completion(chain) {
                    final DisableCbtTaskReply reply = new DisableCbtTaskReply();

                    @Override
                    public void success() {
                        changeCbtTaskStatus(msg.getUuid(), CbtTaskStatus.Stopped);
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
                return String.format("disable-cbt-task-%s", msg.getUuid());
            }
        });
    }

    private void doDisableCbtTask(DisableCbtTaskMsg msg, Completion completion) {
        final CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
        if (taskVO == null) {
            logger.warn(String.format("cannot find CBT task[uuid: %s]", msg.getUuid()));
            completion.success();
            return;
        }

        if (taskVO.getStatus() == CbtTaskStatus.Stopped) {
            completion.success();
            return;
        }

        final CbtTaskResourceRefVO refVO = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, msg.getUuid())
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .find();
        if (refVO == null) {
            logger.warn(String.format("cannot find the resource of CBT task[uuid: %s]", msg.getUuid()));
            completion.fail(operr(String.format("cannot find the resource of CBT task[uuid: %s]", msg.getUuid())));
            return;
        }

        final String vmUuid = refVO.getResourceUuid();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("start-cbt-task-for-vm-%s", vmUuid));
        chain.then(new NoRollbackFlow() {
            final String __name__ = "disable-cbt-task";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                String hostUuid = getVmHostUuid(vmUuid);
                if (hostUuid == null) {
                    trigger.fail(operr("host not found for VM: %s", vmUuid));
                    return;
                }
                CbtBackupHypervisorBackend backend = getCbtBackend(vmUuid);
                backend.disableVolumeCbtBackup(taskVO, vmUuid, hostUuid, new Completion(completion) {
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

            @Override
            public boolean skip(Map data) {
                return vmNotRunning(vmUuid);
            }
        });
        chain.then(new NoRollbackFlow() {
            final String __name__ = "destroy-cbt-volumes";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                destroyCbtVolumes(taskVO.getUuid(), new Completion(trigger) {
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
        });
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();

    }

    private void syncVolumeSize(final List<String> volumeUuids, Completion completion) {
        new While<>(volumeUuids).step((uuid, c) -> {
            SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
            smsg.setVolumeUuid(uuid);
            bus.makeLocalServiceId(smsg, VolumeConstant.SERVICE_ID);
            bus.send(smsg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        c.addError(reply.getError());
                        c.done();
                        return;
                    }

                    c.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                completion.success();
            }
        });
    }

    private void doCreateCbtTask(final CreateCbtTaskMsg msg,
                                 ReturnValueCompletion<CbtTaskInventory> completion) {
        final String uuid = msg.getUuid();
        final String vmUuid = msg.getVmInstanceUuid();
        List<String> existingTaskUuids = CbtTaskConflictChecker.getTaskUuids(vmUuid);
        if (!existingTaskUuids.isEmpty()) {
            completion.fail(operr("VM[uuid: %s] already has a CBT task[uuid: %s], cannot create another task.",
                    vmUuid, existingTaskUuids.get(0)));
            return;
        }
        CbtTaskVO taskVO = new SQLBatchWithReturn<CbtTaskVO>() {
            @Override
            protected CbtTaskVO scripts() {
                CbtTaskVO vo = new CbtTaskVO();
                vo.setUuid(uuid);
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setStatus(CbtTaskStatus.Created);
                vo = persist(vo);

                final String resourceType = q(ResourceVO.class)
                        .eq(ResourceVO_.uuid, msg.getVmInstanceUuid())
                        .select(ResourceVO_.resourceType)
                        .findValue();

                CbtTaskResourceRefVO refVO = new CbtTaskResourceRefVO();
                refVO.setTaskUuid(uuid);
                refVO.setResourceType(resourceType);
                refVO.setResourceUuid(vmUuid);
                persist(refVO);

                return vo;
            }
        }.execute();

        completion.success(CbtTaskInventory.valueOf(dbf.reload(taskVO)));
    }

    public void prepareCbtVolumes(List<String> volumeUuids, String accountUuid, String hostUuid, String cbtTaskUuid, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        List<VolumeCbtBackupInfo> infos = new ArrayList<VolumeCbtBackupInfo>();
        KVMHostInventory inv = KVMHostInventory.valueOf(dbf.findByUuid(hostUuid, KVMHostVO.class));
        String nbdServer = CbtBackupNetworkUtils.getBackupAddress(inv);

        new While<>(volumeUuids).step((uuid, wcompl) -> {
            VolumeVO vo = getVolumeByUuid(uuid);
            PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).find();
            String scratchVolumeUuid = Platform.getUuid();

            CreateDataVolumeMsg cmsg = new CreateDataVolumeMsg();
            cmsg.setDiskSize(vo.getSize());
            cmsg.setName("Cbt-volume-of-" + vo.getName());
            cmsg.setAccountUuid(accountUuid);
            cmsg.setResourceUuid(scratchVolumeUuid);
            cmsg.setSystemTags(new ArrayList<>(asList(VolumeSystemTags.FORMAT_QCOW2.getTagFormat(),VolumeSystemTags.NO_ZERO_FILLED_VOLUME.getTagFormat())));
            cmsg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
            cmsg.setDescription(String.format("cbt-volume-for-%s", vo.getUuid()));
            if (ps.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
                cmsg.addSystemTag(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                        Collections.singletonMap(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, hostUuid)));
            }

            bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(wcompl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        wcompl.addError(operr("create cbt volumes failed. details is: %s", reply.getError().getDetails()));
                        wcompl.done();
                        return;
                    }
                    CreateVolumeReply cReply = reply.castReply();
                    addCbtTaskResourceRef(cReply.getInventory().getUuid(), cbtTaskUuid);

                    VolumeCbtBackupInfo info = new VolumeCbtBackupInfo();
                    info.setVolume(VolumeTO.valueOf(VolumeInventory.valueOf(vo), inv));
                    info.setTarget(VolumeTO.valueOf(cReply.getInventory(), inv).getInstallPath());
                    info.setNbdServer(nbdServer);
                    infos.add(info);
                    wcompl.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.isEmpty()) {
                    completion.success(infos);
                } else {
                    completion.fail(errorCodeList.wrapOrNull());
                }
            }
        });
    }

    public void destroyCbtVolumes(String taskUuid, Completion completion) {
        List<String> uuids = getResourceUuidsByCbtTask(VolumeVO.class.getSimpleName(), taskUuid);
        if (CollectionUtils.isEmpty(uuids)) {
            completion.success();
            return;
        }

        new While<>(uuids).step((uuid, wcompl) -> {
            DeleteVolumeMsg delete = new DeleteVolumeMsg();
            delete.setUuid(uuid);
            delete.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Delay.toString());
            bus.makeTargetServiceIdByResourceUuid(delete, VolumeConstant.SERVICE_ID, uuid);
            bus.send(delete, new CloudBusCallBack(wcompl) {
                @Override
                public void run(MessageReply deleteReply) {
                    if (!deleteReply.isSuccess()) {
                        logger.warn(String.format("failed to delete the volume[uuid:%s], %s", uuid, deleteReply.getError()));
                        wcompl.addError(deleteReply.getError());
                        wcompl.done();
                        return;
                    }

                    ExpungeVolumeMsg expunge = new ExpungeVolumeMsg();
                    expunge.setVolumeUuid(uuid);
                    bus.makeTargetServiceIdByResourceUuid(expunge, VolumeConstant.SERVICE_ID, uuid);
                    bus.send(expunge, new CloudBusCallBack(wcompl) {
                        @Override
                        public void run(MessageReply expungeReply) {
                            if (expungeReply.isSuccess()) {
                                deleteResourceUuidsForCbtTask(uuid, taskUuid);
                                logger.debug(String.format("successfully expunged the volume [uuid:%s]", uuid));
                            } else {
                                logger.warn(String.format("failed to expunge the volume[uuid:%s], %s", uuid, expungeReply.getError()));
                                wcompl.addError(expungeReply.getError());
                            }
                            wcompl.done();
                        }
                    });
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.next(errorCodeList.wrapOrNull());
            }
        });
    }

    private List<String> getResourceUuidsByCbtTask(String resourceType, String taskUuid) {
        return Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceType, resourceType)
                .select(CbtTaskResourceRefVO_.resourceUuid)
                .listValues();
    }

    private VolumeVO getVolumeByUuid(String uuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, uuid)
                .find();
    }

    private void deleteResourceUuidsForCbtTask(String resourceUuid, String taskUuid) {
        SQL.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceUuid, resourceUuid)
                .delete();
    }

    private void addCbtTaskResourceRef(String resourceUuid, String taskUuid) {
        new SQLBatchWithReturn<CbtTaskResourceRefVO>() {
            @Override
            protected CbtTaskResourceRefVO scripts() {
                final String resourceType = q(ResourceVO.class)
                        .eq(ResourceVO_.uuid, resourceUuid)
                        .select(ResourceVO_.resourceType)
                        .findValue();

                CbtTaskResourceRefVO refVO = new CbtTaskResourceRefVO();
                refVO.setTaskUuid(taskUuid);
                refVO.setResourceType(resourceType);
                refVO.setResourceUuid(resourceUuid);
                persist(refVO);
                return refVO;
            }
        }.execute();
    }

    private String getHypervisorTypeByVmUuid(String vmUuid) {
        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
    }

    private CbtBackupHypervisorBackend getCbtBackend(String vmUuid) {
        String hypervisor = getHypervisorTypeByVmUuid(vmUuid);
        if (Strings.isNullOrEmpty(hypervisor)) {
            throw new OperationFailureException(Platform.operr("No hypervisor type for VM %s", vmUuid));
        }
        CbtBackupFactory factory = this.getHypervisorBackupFactory(hypervisor);
        return factory.getHypervisorBackend();
    }

    private CbtBackupFactory getHypervisorBackupFactory(String hypervisorType) {
        CbtBackupFactory factory = bkFactories.get(hypervisorType);
        if (factory == null) {
            throw new OperationFailureException(Platform.operr("No CbtBackupFactory of type[%s] found", hypervisorType));
        }
        return factory;
    }

    private void populateCbtFactories() {
        for (CbtBackupFactory ext : pluginRgty.getExtensionList(CbtBackupFactory.class)) {
            CbtBackupFactory old = bkFactories.get(ext.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate CbtBackupFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHypervisorType()));
            }

            bkFactories.put(ext.getHypervisorType(), ext);
        }
    }

    private boolean vmNotRunning(String vmUuid) {
        VmInstanceState state = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.state)
                .findValue();
        return state == null || state == VmInstanceState.Stopped || state == VmInstanceState.Destroyed;
    }

    private boolean cbtNotRunning(String taskUuid) {
        CbtTaskStatus status = Q.New(CbtTaskVO.class)
                .eq(CbtTaskVO_.uuid, taskUuid)
                .select(CbtTaskVO_.status)
                .findValue();
        return status != CbtTaskStatus.Running;
    }

    private String getVmUuidByCbtTaskUuid(String taskUuid) {
        return Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .select(CbtTaskResourceRefVO_.resourceUuid)
                .findValue();
    }

    private String getVmSyncSignature(String vmUuid) {
        return String.format("Vm-%s", vmUuid);
    }

    private ErrorCode validateVmStateForOperation(String vmUuid, VmInstanceState expectedState, String operation) {
        VmInstanceState state = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.state)
                .findValue();
        if (state != expectedState) {
            return operr("Cannot %s for vm[uuid: %s], current vm state is %s, the expected vm state is %s.",
                    operation, vmUuid, state, expectedState);
        }
        return null;
    }

    private List<String> getVmVolumeUuids(String vmUuid) {
        List<String> volumeUuids = getDiskVolumeUuidsByVmUuid(vmUuid);
        if (volumeUuids == null) {
            throw new CloudRuntimeException(String.format("cannot find volume attached on this vm instance[uuid:%s].", vmUuid));
        }
        return volumeUuids;
    }

    private List<String> getDiskVolumeUuidsByVmUuid(String vmUuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .eq(VolumeVO_.status, VolumeStatus.Ready)
                .in(VolumeVO_.type, asList(VolumeType.Root, VolumeType.Data))
                .select(VolumeVO_.uuid)
                .listValues();
    }

    private String getVmHostUuid(String vmUuid) {
        String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (StringUtils.isNotEmpty(hostUuid)) {
            return hostUuid;
        }

        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.lastHostUuid)
                .findValue();
    }

    private boolean hasCbtCleanupRecords(String taskUuid) {
        return Q.New(VolumeCbtBackupRecordVO.class)
                .eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid)
                .isExists();
    }

    private boolean shouldRetainCbtVolumesAfterRollback(String taskUuid) {
        List<VolumeCbtBackupRecordVO> records = Q.New(VolumeCbtBackupRecordVO.class)
                .eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid)
                .notNull(VolumeCbtBackupRecordVO_.cleanupAttemptUuid)
                .list();
        return CbtCleanupRecordPolicy.shouldRetainScratchVolumes(records);
    }


    @Override
    public ErrorCode check(String vmUuid, String currentResourceUuid) {
        return CbtTaskConflictChecker.checkTaskConflict(vmUuid, currentResourceUuid);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(CbtBackupConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateCbtFactories();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected void changeCbtTaskStatus(String cbtTaskUuid, CbtTaskStatus newStatus) {
        CbtTaskVO vo = dbf.findByUuid(cbtTaskUuid, CbtTaskVO.class);
        if (vo == null || vo.getStatus() == newStatus) {
            return;
        }

        SQL.New(CbtTaskVO.class)
                .eq(CbtTaskVO_.uuid, cbtTaskUuid)
                .set(CbtTaskVO_.status, newStatus)
                .update();
        logger.debug(String.format("Cbt task[%s]'s status changed from %s to %s", cbtTaskUuid, vo.getStatus(), newStatus));
    }

    protected void persistVolumeCbtBackupRecord(String cbtTaskUuid, String lastBitmapName,
                                                 List<VolumeCbtBackupInfo> backupInfos, Completion completion) {
        List<VolumeCbtBackupRecordVO> records = buildVolumeCbtBackupRecords(cbtTaskUuid, lastBitmapName, backupInfos, false);
        if (records.isEmpty()) {
            completion.success();
            return;
        }
        dbf.persistCollection(records);
        completion.success();
    }

    private List<VolumeCbtBackupRecordVO> buildVolumeCbtBackupRecords(String cbtTaskUuid, String lastBitmapName,
                                                                      List<VolumeCbtBackupInfo> backupInfos,
                                                                      boolean onlyStarted) {
        List<VolumeCbtBackupRecordVO> records = new ArrayList<>(backupInfos.size());
        String cleanupAttemptUuid = onlyStarted ? Platform.getUuid() : null;
        for (VolumeCbtBackupInfo info : backupInfos) {
            if (onlyStarted && Strings.isNullOrEmpty(info.getScratchNodeName())) {
                continue;
            }
            VolumeCbtBackupRecordVO recodeVO = new VolumeCbtBackupRecordVO();
            recodeVO.setTaskUuid(cbtTaskUuid);
            recodeVO.setMode(info.getMode());
            recodeVO.setBitmapName(info.getBitmapName());
            recodeVO.setLastBitmapName(lastBitmapName);
            recodeVO.setTarget(info.getTarget());
            recodeVO.setVolumeUuid(info.getVolume().getVolumeUuid());
            recodeVO.setScratchNodeName(info.getScratchNodeName());
            recodeVO.setCleanupAttemptUuid(cleanupAttemptUuid);
            records.add(recodeVO);
        }
        return records;
    }
}
