package org.zstack.ha.fencers;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.externalStorage.primary.ExternalStorageFencerType;
import org.zstack.ha.HostHaStateMachine;
import org.zstack.ha.entity.HaCommands.GetFencerStateFromHostCmd;
import org.zstack.ha.entity.HaCommands.GetFencerStateFromHostRsp;
import org.zstack.ha.entity.HostHaStateVO;
import org.zstack.ha.entity.HostHaStateVO_;
import org.zstack.ha.header.HaErrors;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.header.HostHaState;
import org.zstack.ha.header.HostHaStateEvent;
import org.zstack.ha.message.SelfFencerStruct;
import org.zstack.ha.spi.SelfFencerHypervisorBackend;
import org.zstack.header.agent.AgentResponse;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudStateMachineException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.storage.primary.nfs.NfsSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.zstack.core.Platform.err;
import static org.zstack.ha.entity.HaCommands.CancelSelfFencerCmd;
import static org.zstack.ha.entity.HaCommands.SetupSelfFencerCmd;
import static org.zstack.ha.fencers.HaStrategyHelper.getPrimaryStorageSelfFencerStrategy;

/**
 * Created by xing5 on 2016/3/30.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SelfFencerKvmBackend implements SelfFencerHypervisorBackend {
    private static final CLogger logger = Utils.getLogger(SelfFencerKvmBackend.class);
    private SelfFencerStruct param;

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    private List<PrimaryStorageInventory> fileSystemStorage;
    private Map<PrimaryStorageInventory, KvmSetupSelfFencerExtensionPoint> hasExtensions;

    public SelfFencerKvmBackend(SelfFencerStruct param) {
        this.param = param;

        fileSystemStorage = new ArrayList<>();
        hasExtensions = new HashMap<>();
        for (PrimaryStorageInventory ps : param.primaryStorage) {
            KvmSetupSelfFencerExtensionPoint ext = pluginRgty.getExtensionFromMap(ps.getType(), KvmSetupSelfFencerExtensionPoint.class);
            if (ext != null) {
                hasExtensions.put(ps, ext);
                continue;
            }

            String identity = Q.New(ExternalPrimaryStorageVO.class)
                    .select(ExternalPrimaryStorageVO_.identity)
                    .eq(ExternalPrimaryStorageVO_.uuid, ps.getUuid())
                    .findValue();

            if (StringUtils.isNotEmpty(identity)) {
                ext = pluginRgty.getExtensionFromMap(ExternalStorageFencerType.getProtocolFromIdentity(identity), KvmSetupSelfFencerExtensionPoint.class);
            }

            if (ext != null) {
                hasExtensions.put(ps, ext);
            } else {
                fileSystemStorage.add(ps);
            }
        }
    }

    private void cancelForFileSystemPrimaryStorage(PrimaryStorageInventory ps, final Completion completion) {
        HostInventory host = param.host;
        CancelSelfFencerCmd cmd = new CancelSelfFencerCmd();
        cmd.hostUuid = host.getUuid();
        cmd.mountPoints.add(ps.getMountPath());
        cmd.psUuids.add(ps.getUuid());

        new KvmCommandSender(host.getUuid()).send(cmd, CancelSelfFencerCmd.PATH, wrapper -> {
            AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
            return rsp.isSuccess() ? null : err(HaErrors.CANCEL_SELF_FENCER_FAILED, "operation error, because:%s", rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void setupForFileSystemPrimaryStorage(PrimaryStorageInventory ps, final Completion completion) {
        HostInventory host = param.host;
        SetupSelfFencerCmd cmd = new SetupSelfFencerCmd();
        cmd.hostUuid = host.getUuid();
        cmd.interval = HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class);
        cmd.maxAttempts = HaGlobalConfig.HOST_SELF_FENCER_ATTEMPTS.value(Integer.class);
        cmd.mountPaths.add(ps.getMountPath());
        cmd.uuids.add(ps.getUuid());
        cmd.urls.add(ps.getUrl());
        cmd.mountedByZStack.add(Objects.equals(ps.getType(), NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE));
        cmd.mountOptions.add(Optional.ofNullable(NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(ps.getUuid(),
                NfsSystemTags.MOUNT_OPTIONS_TOKEN)).orElse(""));
        cmd.storageCheckerTimeout = HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        cmd.strategy = (param.strategy == null) ?
                getPrimaryStorageSelfFencerStrategy(ps.getUuid(), ps.getType()).toString() : param.strategy;
        cmd.fencers = param.fencers;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(host.getUuid());
        msg.setPath(SetupSelfFencerCmd.PATH);
        msg.setNoStatusCheck(param.skipStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                AgentResponse rsp = kr.toResponse(AgentResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(err(HaErrors.SETUP_SELF_FENCER_FAILED, "operation error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Transactional
    public void updateHostHaState(String psUuid, HostHaStateEvent evt) {
        HostHaStateVO vo = Q.New(HostHaStateVO.class)
                .eq(HostHaStateVO_.hostUuid, param.host.getUuid())
                .eq(HostHaStateVO_.primaryStorageUuid, psUuid)
                .find();
        if (vo != null) {
            try {
                HostHaState state = HostHaStateMachine.getState(vo.getState(), evt);
                logger.debug(String.format("update primaryStorage[uuid:%s]'s fencer state on host[uuid:%s] from state[%s] to state[%s] with event[%s]",
                        vo.getPrimaryStorageUuid(), vo.getHostUuid(), vo.getState(), state, evt));
                vo.setState(state);
                dbf.update(vo);
            } catch (CloudStateMachineException e) {
                logger.warn(String.format("unable to find suitable fencer state for primaryStorage[uuid:%s], probably old state changed, current state[%s], current event[%s], error[%s]",
                        psUuid, vo.getState(), evt, e.getMessage()));
            }
        } else {
            HostHaState state = HostHaState.None;
            try {
                state = HostHaStateMachine.getState(state, evt);
            } catch (CloudStateMachineException e) {
                logger.warn(String.format("unable to find suitable fencer state for primaryStorage[uuid:%s], probably old state changed, current state[%s], current event[%s], error[%s]",
                        psUuid, null, evt, e.getMessage()));
            }
            vo = new HostHaStateVO();
            vo.setHostUuid(param.host.getUuid());
            vo.setPrimaryStorageUuid(psUuid);
            vo.setState(state);
            dbf.persist(vo);
            logger.debug(String.format("update primaryStorage[uuid:%s]'s fencer state on host[uuid:%s] from state[%s] to state[%s] with event[%s]",
                    vo.getPrimaryStorageUuid(), vo.getHostUuid(), HostHaState.None, state, evt));
        }
    }

    @Override
    @ExceptionSafe
    public void setup(final Completion completion) {
        class StorageResult {
            volatile boolean success = true;
            ErrorCode error = null;

            StorageResult(boolean success, ErrorCode error) {
                this.success = success;
                this.error = error;
            }
        }

        final Map<String, StorageResult> ret = new HashMap<>();
        logger.debug(String.format("start setup self-fencer on the KVM host[uuid:%s, name: %s]",
                param.host.getUuid(), param.host.getName()));
        final AsyncLatch latch = new AsyncLatch(fileSystemStorage.size() + hasExtensions.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                for (String psUuid : ret.keySet()) {
                    StorageResult result = ret.get(psUuid);
                    if (result.success) {
                        updateHostHaState(psUuid, HostHaStateEvent.setupDone);
                        logger.debug(String.format("setup storage[uuid:%s] self-fencer on the KVM host[uuid:%s, name: %s] success",
                                psUuid, param.host.getUuid(), param.host.getName()));
                        completion.success();
                    } else {
                        updateHostHaState(psUuid, HostHaStateEvent.setupFail);
                        completion.fail(err(HaErrors.SETUP_SELF_FENCER_FAILED,
                                "unable to setup storage[uuid:%s] self-fencer on the KVM host[uuid:%s, name: %s]",
                                psUuid, param.host.getUuid(), param.host.getName()).withCause(result.error));
                    }
                }
            }
        });

        for (PrimaryStorageInventory fs : fileSystemStorage) {
            updateHostHaState(fs.getUuid(), HostHaStateEvent.setup);
            setupForFileSystemPrimaryStorage(fs, new Completion(latch) {
                @Override
                public void success() {
                    ret.put(fs.getUuid(), new StorageResult(true, null));
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.put(fs.getUuid(), new StorageResult(false, errorCode));
                    latch.ack();
                }
            });
        }

        for (Map.Entry<PrimaryStorageInventory, KvmSetupSelfFencerExtensionPoint> e : hasExtensions.entrySet()) {
            PrimaryStorageInventory ps = e.getKey();
            KvmSetupSelfFencerExtensionPoint ext = e.getValue();

            updateHostHaState(ps.getUuid(), HostHaStateEvent.setup);

            KvmSetupSelfFencerParam p = new KvmSetupSelfFencerParam();
            p.setHostUuid(param.host.getUuid());
            p.setInterval(HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class));
            p.setMaxAttempts(HaGlobalConfig.HOST_SELF_FENCER_ATTEMPTS.value(Integer.class));
            p.setStorageCheckerTimeout(HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class));
            p.setPrimaryStorage(ps);
            p.setStrategy(getPrimaryStorageSelfFencerStrategy(ps.getUuid(), ps.getType()).toString());
            p.setFencers(param.fencers);
            ext.kvmSetupSelfFencer(p, new Completion(latch) {
                @Override
                public void success() {
                    ret.put(ps.getUuid(), new StorageResult(true, null));
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.put(ps.getUuid(), new StorageResult(false, errorCode));
                    latch.ack();
                }
            });
        }
    }

    @Override
    @ExceptionSafe
    public void cancel(Completion completion) {
        class StorageResult {
            volatile boolean success = true;
            ErrorCode error = null;

            StorageResult(boolean success, ErrorCode error) {
                this.success = success;
                this.error = error;
            }
        }

        final Map<String, StorageResult> ret = new HashMap<>();
        logger.debug(String.format("start cancel self-fencer on the KVM host[uuid:%s, name: %s]",
                param.host.getUuid(), param.host.getName()));
        final AsyncLatch latch = new AsyncLatch(fileSystemStorage.size() + hasExtensions.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                for (String psUuid : ret.keySet()) {
                    StorageResult result = ret.get(psUuid);
                    if (result.success) {
                        updateHostHaState(psUuid, HostHaStateEvent.cancelDone);
                        logger.debug(String.format("cancel storage[uuid:%s] self-fencer on the KVM host[uuid:%s, name: %s] end",
                                psUuid, param.host.getUuid(), param.host.getName()));
                        completion.success();
                    } else {
                        updateHostHaState(psUuid, HostHaStateEvent.cancelFail);
                        completion.fail(err(HaErrors.CANCEL_SELF_FENCER_FAILED,
                                "unable to cancel storage[uuid:%s] self-fencer on the KVM host[uuid:%s, name: %s]",
                                psUuid, param.host.getUuid(), param.host.getName()).withCause(result.error));
                    }
                }
            }
        });

        for (PrimaryStorageInventory fs : fileSystemStorage) {
            updateHostHaState(fs.getUuid(), HostHaStateEvent.cancel);
            cancelForFileSystemPrimaryStorage(fs, new Completion(latch) {
                @Override
                public void success() {
                    ret.put(fs.getUuid(), new StorageResult(true, null));
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.put(fs.getUuid(), new StorageResult(false, errorCode));
                    latch.ack();
                }
            });
        }

        for (Map.Entry<PrimaryStorageInventory, KvmSetupSelfFencerExtensionPoint> e : hasExtensions.entrySet()) {
            PrimaryStorageInventory ps = e.getKey();
            KvmSetupSelfFencerExtensionPoint ext = e.getValue();

            updateHostHaState(ps.getUuid(), HostHaStateEvent.cancel);

            KvmCancelSelfFencerParam p = new KvmCancelSelfFencerParam();
            p.setHostUuid(param.host.getUuid());
            p.setPrimaryStorage(ps);
            ext.kvmCancelSelfFencer(p, new Completion(latch) {
                @Override
                public void success() {
                    ret.put(ps.getUuid(), new StorageResult(true, null));
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.put(ps.getUuid(), new StorageResult(false, errorCode));
                    latch.ack();
                }
            });
        }
    }

    @Override
    public void sync(Completion completion) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public void run(SyncTaskChain chain) {
                GetFencerStateFromHostCmd cmd = new GetFencerStateFromHostCmd();

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(param.host.getUuid());
                msg.setPath(GetFencerStateFromHostCmd.PATH);
                msg.setCommand(cmd);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, param.host.getUuid());

                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to sync the self-fencer state from the host[uuid:%s, name:%s]",
                                    param.host.getUuid(), param.host.getName()));
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        GetFencerStateFromHostRsp rsp = kr.toResponse(GetFencerStateFromHostRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(err(HaErrors.SYNC_SELF_FENCER_STATE_FAILED,
                                    "exception occurred when sync the self-fencer state from the host[uuid: %s], %s",
                                    param.host.getUuid(), rsp.getError()));
                            return;
                        }

                        syncFencerState(rsp.psUuids);
                        completion.success();
                    }
                });

                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public String getName() {
                return String.format("do-sync-fencer-state-from-host-%s-task", param.host.getUuid());
            }

            @Override
            protected int getMaxPendingTasks() {
                return 1;
            }

            @Override
            protected String getDeduplicateString() {
                return getSyncSignature();
            }

            @Override
            protected void exceedMaxPendingCallback() {
                completion.fail(err(HaErrors.FENCER_SYNC_TASK_CONFLICT,
                        "in task %s: an other fencer state sync task in running, cancel the new task", getName()));
            }
        });
    }

    private void syncFencerState(List<String> psUuids) {
        List<HostHaStateVO> hostHaStateVOs = Q.New(HostHaStateVO.class)
                .eq(HostHaStateVO_.hostUuid, param.host.getUuid())
                .list();
        for (HostHaStateVO hostHaStateVO : hostHaStateVOs) {
            if (psUuids != null && !psUuids.isEmpty() && psUuids.contains(hostHaStateVO.getPrimaryStorageUuid())) {
                updateHostHaState(hostHaStateVO.getPrimaryStorageUuid(), HostHaStateEvent.syncExist);
            } else {
                updateHostHaState(hostHaStateVO.getPrimaryStorageUuid(), HostHaStateEvent.syncNotExist);
            }
        }
    }
}
