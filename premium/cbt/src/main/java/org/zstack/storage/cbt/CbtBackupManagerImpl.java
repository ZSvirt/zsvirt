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
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
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
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class CbtBackupManagerImpl extends AbstractService implements Component, CbtBackupManager {
    private static final CLogger logger = Utils.getLogger(CbtBackupManagerImpl.class);
    private final Map<String, CbtBackupFactory> bkFactories = new ConcurrentHashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;

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
        } else if (msg instanceof EnableCbtTaskMsg) {
            handle((EnableCbtTaskMsg) msg);
        } else if (msg instanceof EnableCbtTaskOnHostMsg) {
            handle((EnableCbtTaskOnHostMsg) msg);
        } else if (msg instanceof DeleteCbtTaskMsg) {
            handle((DeleteCbtTaskMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIExportNbdVolumesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("export-volumes-for-cbt-recovery-task-%s", msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CbtBackupHypervisorBackend backend = getCbtBackend(msg.getVmInstanceUuid());
                exportNbdVolumesFlow(backend, msg.isForce(), msg.getVmInstanceUuid(), msg.getVolumeUuids(), new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(msg) {
                    final APIExportNbdVolumesEvent evt = new APIExportNbdVolumesEvent(msg.getId());

                    @Override
                    public void success(List<VolumeCbtBackupInfo> volumeInfos) {
                        evt.setVolumeInfos(volumeInfos);
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
                return String.format("prepare-volumes-for-cbt-recovery-task");
            }
        });
    }

    private void handle(final APIUnexportNbdVolumesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("unexport-volumes-for-cbt-recovery-task-%s",msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CbtBackupHypervisorBackend backend = getCbtBackend(msg.getVmInstanceUuid());
                backend.unexportNbdVolumesForRecovery(msg.getVmInstanceUuid(), msg.getVolumeUuids(), new Completion(msg) {
                    final APIUnexportNbdVolumesEvent evt = new APIUnexportNbdVolumesEvent(msg.getId());

                    @Override
                    public void success() {
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
                return String.format("prepare-volumes-for-cbt-recovery-task");
            }
        });
    }

    private void handle(final APICreateCbtTaskMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "create-cbt-task";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doCreateCbtTask(msg, new ReturnValueCompletion<CbtTaskInventory>(chain) {
                    final APICreateCbtTaskEvent evt = new APICreateCbtTaskEvent(msg.getId());

                    @Override
                    public void success(CbtTaskInventory inv) {
                        evt.setInventory(inv);
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
                return String.format("create-cbt-task-%s", msg.getName());
            }
        });
    }

    private void handle(final EnableCbtTaskMsg msg) {
        final CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
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
                            changeCbtTaskStatus(msg.getUuid(), CbtTaskStatus.Running);
                            ereply.setVolumeCbtBackupInfos(r.getVolumeCbtBackupInfos());
                            bus.reply(msg, ereply);
                            chain.next();
                        } else {
                            changeCbtTaskStatus(msg.getUuid(), CbtTaskStatus.Failed);
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
        final CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
        final EnableCbtTaskOnHostReply reply = new EnableCbtTaskOnHostReply();
        final String vmUuid = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(CbtTaskResourceRefVO_.taskUuid, msg.getUuid()).select(CbtTaskResourceRefVO_.resourceUuid)
                .findValue();
        String hostUuid = getVmHostUuid(vmUuid);
        if (hostUuid == null) {
            reply.setError(operr("host not found for VM: %s", vmUuid));
            bus.reply(msg, reply);
            return;
        }

        CbtBackupHypervisorBackend backend = getCbtBackend(vmUuid);
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("enable-cbt-task-on-host-%s", msg.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return CbtGlobalConfig.START_CBT_CONCURRENT_LEVEL.value(Integer.class);
            }

            @Override
            public void run(SyncTaskChain chain) {
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
                return String.format("running-cbt-task-%s", taskVO.getUuid());
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
                bus.makeLocalServiceId(dmsg, CbtBackupConstant.SERVICE_ID);
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
                return vmNotRunning(vmUuid) || cbtNotRunning(taskUuid);
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

    private void exportNbdVolumesFlow(CbtBackupHypervisorBackend backend,
                                      boolean force,
                                      String vmUuid,
                                      List<String> volumeUuids,
                                      final ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        final List<VolumeCbtBackupInfo>[] volumeInfos = new List[]{new ArrayList<VolumeCbtBackupInfo>()};
        boolean running = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .isExists();
        if (running) {
            completion.fail(operr("vm[uuid: %s] is running，please stop it and try again.", vmUuid));
            return;
        }

        String hostUuid = getVmHostUuid(vmUuid);
        if (hostUuid == null) {
            completion.fail(operr("host not found for VM: %s", vmUuid));
            return;
        }

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
                destroyCbtVolumes(taskVO.getUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        logger.warn(String.format("rollback prepare cbt[uuid %s] volume successfully.", taskVO.getUuid()));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("rollback prepare cbt[uuid %s] volume failed.", taskVO.getUuid()));
                    }
                });
                trigger.rollback();
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
                backend.disableVolumeCbtBackup(taskVO, vmUuid, hostUuid, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
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
                        persistVolumeCbtBackupRecord(taskVO.getUuid(), bitmapName, volumeInfos);
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
                return String.format("take-volume-cbt-backup-on-host-%s", hostUuid);
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
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("disable-cbt-task-%s",msg.getUuid());
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
        String hostUuid = getVmHostUuid(vmUuid);
        if (hostUuid == null) {
            completion.fail(operr("host not found for VM: %s", vmUuid));
            return;
        }
        CbtBackupHypervisorBackend backend = getCbtBackend(vmUuid);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("start-cbt-task-for-vm-%s", vmUuid));
        chain.then(new NoRollbackFlow() {
            final String __name__ = "disable-cbt-task";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
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

    private void doCreateCbtTask(final APICreateCbtTaskMsg msg,
                                 ReturnValueCompletion<CbtTaskInventory> completion) {
        final String uuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();
        final String vmUuid = msg.getVmInstanceUuid();
        List<String> tasks = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceUuid, vmUuid)
                .select(CbtTaskResourceRefVO_.taskUuid)
                .listValues();
        if (!tasks.isEmpty()) {
            completion.fail(operr("VM [uuid: %s] have been protected by task: %s", vmUuid, tasks.get(0)));
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

        List<ErrorCode> errs = new ArrayList<>();
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
                        errs.add(operr("create cbt volumes failed. details is: %s", reply.getError().getDetails()));
                        wcompl.done();
                        return;
                    }
                    CreateVolumeReply cReply = reply.castReply();
                    addCbtTaskResourceRef(cReply.getInventory().getUuid(), cbtTaskUuid);

                    VolumeCbtBackupInfo info = new VolumeCbtBackupInfo();
                    info.setVolume(VolumeTO.valueOf(VolumeInventory.valueOf(vo), inv));
                    info.setTarget(cReply.getInventory().getInstallPath());
                    info.setNbdServer(inv.getManagementIp());
                    infos.add(info);
                    wcompl.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.isEmpty()) {
                    completion.success(infos);
                } else {
                    completion.fail(errs.get(0));
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

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(uuids).step((uuid, wcompl) -> {
            DeleteVolumeMsg msg = new DeleteVolumeMsg();
            msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(wcompl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to expunge the volume[uuid:%s], %s", uuid, reply.getError()));
                        errs.add(reply.getError());
                    } else {
                        deleteResourceUuidsForCbtTask(uuid, taskUuid);
                        logger.debug(String.format("successfully expunged the volume [uuid:%s]", uuid));
                    }
                    wcompl.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errs.get(0));
                }
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

    protected void persistVolumeCbtBackupRecord(String cbtTaskUuid, String lastBitmapName, List<VolumeCbtBackupInfo> backupInfos) {
        for (VolumeCbtBackupInfo info : backupInfos) {
            VolumeCbtBackupRecordVO recodeVO = new VolumeCbtBackupRecordVO();
            recodeVO.setTaskUuid(cbtTaskUuid);
            recodeVO.setMode(info.getMode());
            recodeVO.setBitmapName(info.getBitmapName());
            recodeVO.setLastBitmapName(lastBitmapName);
            recodeVO.setTarget(info.getTarget());
            recodeVO.setVolumeUuid(info.getVolume().getVolumeUuid());
            recodeVO.setScratchNodeName(info.getScratchNodeName());
            dbf.persistAndRefresh(recodeVO);
        }
    }
}
