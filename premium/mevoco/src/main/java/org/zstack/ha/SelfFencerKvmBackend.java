package org.zstack.ha;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.externalStorage.primary.ExternalStorageFencerType;
import org.zstack.ha.hostHastate.HostHaState;
import org.zstack.ha.hostHastate.HostHaStateVO;
import org.zstack.ha.hostHastate.HostHaStateVO_;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.*;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.storage.primary.nfs.NfsSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.*;

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
    private ApiTimeoutManager timeoutManager;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    public static class AgentCmd {
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error = "";
    }

    public static class SetupSelfFencerCmd extends AgentCmd {
        public String hostUuid;
        public long interval;
        public int maxAttempts;
        public List<String> mountPaths = new ArrayList<>();
        public List<String> uuids = new ArrayList<>();
        public List<String> urls = new ArrayList<>();
        public List<Boolean> mountedByZStack = new ArrayList<>();
        public List<String> mountOptions = new ArrayList<>();
        public int storageCheckerTimeout;
        public String strategy;
        public List<String> fencers;
    }

    public static class CancelSelfFencerCmd extends AgentCmd {
        public String hostUuid;
        public List<String> mountPoints = new ArrayList<>();
        public List<String> psUuids = new ArrayList<>();
    }

    public static class ReportVmSelfFencerCmd {
        public List<ReportVmSelfFencerTuple> values = new ArrayList<>();
        public String hostUuid;
    }

    public static class ReportVmSelfFencerTuple {
        public String vmUuid;
        public String fencerName;
    }

    public static final String SETUP_SELF_FENCER_PATH = "/ha/selffencer/setup";
    public static final String CANCEL_SELF_FENCER_PATH = "/ha/selffencer/cancel";
    public static final String KVM_REPORT_VM_FENCED_EVENT = "/ha/events/vm-fenced";

    private List<PrimaryStorageInventory> fileSystemStorage;
    private Map<PrimaryStorageInventory, KvmSetupSelfFencerExtensionPoint> hasExtensions;

    public SelfFencerKvmBackend(SelfFencerStruct param) {
        this.param = param;

        fileSystemStorage = new ArrayList<>();
        hasExtensions = new HashMap<>();
        for (PrimaryStorageInventory ps : param.getPrimaryStorage()) {
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

    private void cancelForFileSystemPrimaryStorage(List<PrimaryStorageInventory> ps, final Completion completion) {
        HostInventory host = param.getHost();
        CancelSelfFencerCmd cmd = new CancelSelfFencerCmd();
        cmd.hostUuid = host.getUuid();
        ps.forEach(it -> {
            cmd.mountPoints.add(it.getMountPath());
            cmd.psUuids.add(it.getUuid());
        });

        new KvmCommandSender(host.getUuid()).send(cmd, CANCEL_SELF_FENCER_PATH, wrapper -> {
            AgentRsp rsp = wrapper.getResponse(AgentRsp.class);
            return rsp.success ? null : operr("operation error, because:%s", rsp.error);
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

    private void setupForFileSystemPrimaryStorage(List<PrimaryStorageInventory> ps, final Completion completion) {
        HostInventory host = param.getHost();
        SetupSelfFencerCmd cmd = new SetupSelfFencerCmd();
        cmd.hostUuid = host.getUuid();
        cmd.interval = HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class);
        cmd.maxAttempts = HaGlobalConfig.HOST_SELF_FENCER_ATTEMPTS.value(Integer.class);
        ps.forEach(it -> {
            cmd.mountPaths.add(it.getMountPath());
            cmd.uuids.add(it.getUuid());
            cmd.urls.add(it.getUrl());
            cmd.mountedByZStack.add(Objects.equals(it.getType(), NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE));
            cmd.mountOptions.add(Optional.ofNullable(NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(it.getUuid(),
                    NfsSystemTags.MOUNT_OPTIONS_TOKEN)).orElse(""));
        });
        cmd.storageCheckerTimeout = HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        cmd.strategy = param.getStrategy();
        cmd.fencers = param.getFencers();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(host.getUuid());
        msg.setPath(SETUP_SELF_FENCER_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                AgentRsp rsp = kr.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    private void updateHostHaState(HostHaState state) {
        UpdateQuery.New(HostHaStateVO.class)
                .eq(HostHaStateVO_.uuid, param.getHost().getUuid())
                .set(HostHaStateVO_.state, state)
                .update();
    }

    @Override
    public void setup(final Completion completion) {
        class Result {
            volatile boolean success = true;
            List<ErrorCode> errors = new ArrayList<ErrorCode>();
        }

        final Result ret = new Result();
        updateHostHaState(HostHaState.Setuping);
        logger.debug(String.format("start setup self-fencer on the KVM host[uuid:%s, name: %s]",
                param.getHost().getUuid(), param.getHost().getName()));
        final AsyncLatch latch = new AsyncLatch(fileSystemStorage.size() + hasExtensions.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (ret.success) {
                    updateHostHaState(HostHaState.Done);
                    logger.debug(String.format("setup self-fencer on the KVM host[uuid:%s, name: %s] end",
                            param.getHost().getUuid(), param.getHost().getName()));
                    completion.success();
                } else {
                    updateHostHaState(HostHaState.Fail);
                    completion.fail(errf.stringToOperationError(String.format("unable to setup self-fencer on the KVM host[uuid:%s, name: %s]",
                            param.getHost().getUuid(), param.getHost().getName()), ret.errors));
                }
            }
        });

        if (!fileSystemStorage.isEmpty()) {
            setupForFileSystemPrimaryStorage(fileSystemStorage, new Completion(latch) {
                @Override
                public void success() {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errors.add(errorCode);
                    ret.success = false;
                    latch.ack();
                }
            });
        }

        for (Map.Entry<PrimaryStorageInventory, KvmSetupSelfFencerExtensionPoint> e : hasExtensions.entrySet()) {
            PrimaryStorageInventory ps = e.getKey();
            KvmSetupSelfFencerExtensionPoint ext = e.getValue();

            KvmSetupSelfFencerParam p = new KvmSetupSelfFencerParam();
            p.setHostUuid(param.getHost().getUuid());
            p.setInterval(HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class));
            p.setMaxAttempts(HaGlobalConfig.HOST_SELF_FENCER_ATTEMPTS.value(Integer.class));
            p.setStorageCheckerTimeout(HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class));
            p.setPrimaryStorage(ps);
            p.setStrategy(HaStrategyHelper.getPrimaryStorageSelfFencerStrategy(ps.getUuid(), ps.getType()).toString());
            p.setFencers(param.getFencers());
            ext.kvmSetupSelfFencer(p, new Completion(latch) {
                @Override
                public void success() {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errors.add(errorCode);
                    ret.success = false;
                    latch.ack();
                }
            });
        }
    }

    @Override
    public void cancel(Completion completion) {
        class Result {
            volatile boolean success = true;
            List<ErrorCode> errors = new ArrayList<ErrorCode>();
        }

        final Result ret = new Result();
        updateHostHaState(HostHaState.Cancelling);
        logger.debug(String.format("start cancel self-fencer on the KVM host[uuid:%s, name: %s]",
                param.getHost().getUuid(), param.getHost().getName()));
        final AsyncLatch latch = new AsyncLatch(fileSystemStorage.size() + hasExtensions.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (ret.success) {
                    updateHostHaState(HostHaState.None);
                    logger.debug(String.format("cancel self-fencer on the KVM host[uuid:%s, name: %s] end",
                            param.getHost().getUuid(), param.getHost().getName()));
                    completion.success();
                } else {
                    updateHostHaState(HostHaState.Fail);
                    completion.fail(errf.stringToOperationError(String.format("unable to cancel self-fencer on the KVM host[uuid:%s, name: %s]",
                            param.getHost().getUuid(), param.getHost().getName()), ret.errors));
                }
            }
        });

        if (!fileSystemStorage.isEmpty()) {
            cancelForFileSystemPrimaryStorage(fileSystemStorage, new Completion(latch) {
                @Override
                public void success() {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errors.add(errorCode);
                    ret.success = false;
                    latch.ack();
                }
            });
        }

        for (Map.Entry<PrimaryStorageInventory, KvmSetupSelfFencerExtensionPoint> e : hasExtensions.entrySet()) {
            PrimaryStorageInventory ps = e.getKey();
            KvmSetupSelfFencerExtensionPoint ext = e.getValue();

            KvmCancelSelfFencerParam p = new KvmCancelSelfFencerParam();
            p.setHostUuid(param.getHost().getUuid());
            p.setPrimaryStorage(ps);
            ext.kvmCancelSelfFencer(p, new Completion(latch) {
                @Override
                public void success() {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errors.add(errorCode);
                    ret.success = false;
                    latch.ack();
                }
            });
        }
    }
}
