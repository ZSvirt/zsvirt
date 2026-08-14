package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.DeleteVmGC;
import org.zstack.compute.vm.VmTracer;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMAgentCommands.ReportVmStateCmd;
import org.zstack.kvm.KVMAgentCommands.VmSyncCmd;
import org.zstack.kvm.KVMAgentCommands.VmSyncResponse;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.getReflections;
import static org.zstack.core.Platform.operr;

public class KvmVmSyncPingTask extends VmTracer implements KVMPingAgentNoFailureExtensionPoint, KVMHostConnectExtensionPoint,
        MarshalReplyMessageExtensionPoint, HostConnectionReestablishExtensionPoint, HostAfterConnectedExtensionPoint, Component,
        ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(KvmVmSyncPingTask.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private PluginRegistry pluginRgty;

    // A map from apiId to VM instance uuid
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> vmApis = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> vmsToSkip = new ConcurrentHashMap<>();
    private List<Class<? extends Message>> skipVmTracerMessages = new ArrayList<>();
    private List<Class> skipVmTracerReplies = new ArrayList<>();
    private Map<String, Integer> vmInShutdownMap = new ConcurrentHashMap<>();

    {
        getReflections().getTypesAnnotatedWith(SkipVmTracer.class).forEach(clz -> {
            skipVmTracerMessages.add(clz.asSubclass(Message.class));
            skipVmTracerReplies.add(clz.getAnnotation(SkipVmTracer.class).replyClass());
        });
    }

    @SuppressWarnings("unused")
    void init() {
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (msg.getServiceId().equals(ApiMediatorConstant.SERVICE_ID)) {
                    // the API message will be routed by ApiMediator,
                    // filter out this message to avoid reporting the same
                    // API message twice
                    return;
                }

                final String vmUuid;

                if (msg instanceof VmInstanceMessage) {
                    vmUuid = ((VmInstanceMessage) msg).getVmInstanceUuid();
                } else {
                    throw new OperationFailureException(operr("cannot get vmUuid from msg %s", msg.getMessageName()));
                }

                VmTracerCanonicalEvents.VmSkipTraceData data = new VmTracerCanonicalEvents.VmSkipTraceData();
                data.setMsgName(msg.getMessageName());
                data.setVmUuid(vmUuid);
                if (msg instanceof APIMessage) {
                    final String apiId = msg.getId();
                    data.setApiId(apiId);
                }

                data.setManagementNodeId(Platform.getManagementServerId());

                evtf.fire(VmTracerCanonicalEvents.VM_SKIP_TRACE_PATH, data);
            }
        }, skipVmTracerMessages);

        evtf.on(VmTracerCanonicalEvents.VM_SKIP_TRACE_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmTracerCanonicalEvents.VmSkipTraceData data1
                        = (VmTracerCanonicalEvents.VmSkipTraceData) data;
                if (data1.getVmUuid() != null) {
                    vmsToSkip.putIfAbsent(data1.getManagementNodeId(), ConcurrentHashMap.newKeySet());
                    vmsToSkip.get(data1.getManagementNodeId()).add(data1.getVmUuid());
                }

                if (data1.getApiId() == null) {
                    logger.info(String.format("Skipping tracing VM[uuid:%s], due to %s",
                            data1.getVmUuid(), data1.getMsgName()));
                    return;
                }

                vmApis.putIfAbsent(data1.getManagementNodeId(), new ConcurrentHashMap<>());
                if (vmApis.get(data1.getManagementNodeId()).putIfAbsent(data1.getApiId(), data1.getVmUuid()) == null) {
                    logger.info(String.format("Skipping tracing VM[uuid:%s], due to %s, api=%s", data1.getVmUuid(), data1.getMsgName(), data1.getApiId()));
                }
            }
        });

        evtf.on(VmTracerCanonicalEvents.VM_CONTINUE_TRACE_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmTracerCanonicalEvents.VmContinueTraceData data1
                        = (VmTracerCanonicalEvents.VmContinueTraceData) data;
                if (data1.getApiId() != null && vmApis.containsKey(data1.getManagementNodeId()) && vmApis.get(data1.getManagementNodeId()).contains(data1.getApiId())) {
                    String vmUuid = vmApis.get(data1.getManagementNodeId()).remove(data1.getApiId());
                    logger.info("Continuing tracing VM: " + vmUuid);
                    vmsToSkip.get(data1.getManagementNodeId()).remove(vmUuid);
                    return;
                }

                if (data1.getVmUuid() != null) {
                    logger.info("Continuing tracing VM: " + data1.getVmUuid());
                    vmsToSkip.get(data1.getManagementNodeId()).remove(data1.getVmUuid());
                }
            }
        });
    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        return skipVmTracerReplies;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        continueTraceVmAfterWhenMessageReplied(replyOrEvent, msg);
    }

    @Override
    public void marshalReplyMessageBeforeDropping(Message replyOrEvent, NeedReplyMessage msg) {
        continueTraceVmAfterWhenMessageReplied(replyOrEvent, msg);
    }

    private void continueTraceVmAfterWhenMessageReplied(Message replyOrEvent, NeedReplyMessage msg) {
        String vmUuid = null;
        String apiId = null;

        if (msg instanceof VmInstanceMessage) {
            vmUuid = ((VmInstanceMessage) msg).getVmInstanceUuid();
        } else if (replyOrEvent instanceof APIEvent) {
            // do not rely on the inventory from event reply - the VM operation might fail
            apiId = ((APIEvent) replyOrEvent).getApiId();
        }

        if (apiId != null) {
            vmUuid = vmApis.get(Platform.getManagementServerId()).get(apiId);
        }

        if (vmUuid != null) {
            VmTracerCanonicalEvents.VmContinueTraceData data = new VmTracerCanonicalEvents.VmContinueTraceData();
            data.setApiId(apiId);
            data.setVmUuid(vmUuid);
            data.setManagementNodeId(Platform.getManagementServerId());
            evtf.fire(VmTracerCanonicalEvents.VM_CONTINUE_TRACE_PATH, data);
        }
    }

    private void syncVm(final HostInventory host, final Completion completion) {
        // Get vms to skip before send command to host to confirm the vm will be skipped after sync command finished.
        // The problem is if one vm-sync skipped operation is started and finished during vm sync command's handling
        // vm state would still be sync to mn
        Set<String> vmsToSkipSetHostSide = new HashSet<>();
        vmsToSkip.values().forEach(vmsToSkipSetHostSide::addAll);

        // if the vm is not running on host when sync command executing but started as soon as possible
        // before response handling of vm sync, mgmtSideStates will including the running vm but not result in
        // vm sync response the vm would be changed to Stopped which is not expected.
        // but if the vm is running on host but stopped on management node side in same pattern, the result is
        // wrong but it will be fixed in next vm sync and different from creation, normally vm operations skip
        // vm sync during processing
        Map<String, VmInstanceState> mgmtSideStates = buildManagementServerSideVmStates(host.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        VmSyncCmd cmd = new VmSyncCmd();
        msg.setCommand(cmd);
        msg.setNoStatusCheck(true);
        msg.setHostUuid(host.getUuid());
        msg.setPath(KVMConstant.KVM_VM_SYNC_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                VmSyncResponse ret = r.toResponse(VmSyncResponse.class);
                if (ret.isSuccess()) {
                    Map<String, VmInstanceState> states = new HashMap<>(ret.getStates().size());

                    // Get vms to skip after sync result returned.
                    vmsToSkip.values().forEach(vmsToSkipSetHostSide::addAll);

                    Collection<String> vmUuidsInDeleteVmGC = DeleteVmGC.queryVmInGC(host.getUuid(), ret.getStates().keySet());

                    for (Map.Entry<String, String> e : ret.getStates().entrySet()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("state from vmsync vm %s state %s", e.getKey(), e.getValue()));
                        }
                        if (vmUuidsInDeleteVmGC != null && vmUuidsInDeleteVmGC.contains(e.getKey())) {
                            /*the vm has been deleted and recovered that no resource, so skip to trace */
                            vmsToSkipSetHostSide.add(e.getKey());
                        }

                        VmInstanceState state = KvmVmState.valueOf(e.getValue()).toVmInstanceState();
                        if (state == VmInstanceState.Running
                                || state == VmInstanceState.Paused
                                || state == VmInstanceState.Unknown
                                || state == VmInstanceState.Stopped
                                || state == VmInstanceState.Crashed
                                || state == VmInstanceState.NoState) {
                            states.put(e.getKey(), state);
                        }

                    }

                    for (KvmVmSyncExtensionPoint ext : pluginRgty.getExtensionList(KvmVmSyncExtensionPoint.class)) {
                        ext.afterVmSync(host, states, vmsToSkipSetHostSide);
                    }

                    checkVmInShutdown(ret.getVmInShutdowns(), states);
                    reportVmState(host.getUuid(), states, vmsToSkipSetHostSide, mgmtSideStates);
                    completion.success();
                } else {
                    ErrorCode errorCode = operr("unable to do vm sync on host[uuid:%s, ip:%s] because %s", host.getUuid(), host.getManagementIp(), ret.getError());
                    completion.fail(errorCode);
                }
            }
        });
    }

    private void checkVmInShutdown(final List<String> vmInShutdowns, final Map<String, VmInstanceState> states) {
        if (vmInShutdowns.isEmpty() && vmInShutdownMap.isEmpty()) {
            return;
        }

        final int vmInShutdownMaxnum = 10;
        for (String vmUuid : states.keySet()) {
            if (!vmInShutdowns.contains(vmUuid)) {
                vmInShutdownMap.remove(vmUuid);
                continue;
            }

            if (vmInShutdownMap.get(vmUuid) == null) {
                vmInShutdownMap.put(vmUuid, 1);
                continue;
            }

            if (vmInShutdownMap.get(vmUuid) < vmInShutdownMaxnum) {
                vmInShutdownMap.put(vmUuid, vmInShutdownMap.get(vmUuid) + 1);
            } else {
                VmTracerCanonicalEvents.VmStateInShutdownData data = new VmTracerCanonicalEvents.VmStateInShutdownData();
                data.setVmUuid(vmUuid);
                ErrorCode err = operr("The vm[%s] state is in shutdown for a long time, check whether the vm is normal", vmUuid);
                data.setReason(err);
                evtf.fire(VmTracerCanonicalEvents.VM_STATE_IN_SHUTDOWN_PATH, data);
                vmInShutdownMap.remove(vmUuid);
            }
        }
    }

    @Override
    public void connectionReestablished(HostInventory inv) {
        syncVm(inv, new NopeCompletion());
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_STATE, ReportVmStateCmd.class, new SyncHttpCallHandler<ReportVmStateCmd>() {
            private void reportState(final ReportVmStateCmd cmd) {
                thdf.chainSubmit(new ChainTask(null) {
                    @Override
                    public String getSyncSignature() {
                        return String.format("report-state-of-vm-%s", cmd.vmUuid);
                    }

                    @Override
                    public void run(final SyncTaskChain chain) {
                        VmInstanceState state = KvmVmState.valueOf(cmd.vmState).toVmInstanceState();

                        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
                        q.select(VmInstanceVO_.state);
                        q.add(VmInstanceVO_.uuid, Op.EQ, cmd.vmUuid);
                        VmInstanceState stateInDb = q.findValue();
                        if (stateInDb == null) {
                            logger.warn(String.format("an anonymous VM[uuid:%s, state:%s] is detected on the host[uuid:%s]", cmd.vmUuid, state, cmd.hostUuid));
                            chain.next();
                            return;
                        }

                        VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
                        msg.setVmStateAtTracingMoment(stateInDb);
                        msg.setVmInstanceUuid(cmd.vmUuid);
                        msg.setStateOnHost(state);
                        msg.setHostUuid(cmd.hostUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, cmd.vmUuid);
                        bus.send(msg, new CloudBusCallBack(chain) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to report state[%s] of the vm[uuid:%s] on the host[uuid:%s], %s",
                                            cmd.vmState, cmd.vmUuid, cmd.hostUuid, reply.getError()));
                                }

                                chain.next();
                            }
                        });
                    }

                    @Override
                    public String getName() {
                        return "report-vm-state";
                    }
                });
            }

            @Override
            public String handleSyncHttpCall(ReportVmStateCmd cmd) {
                reportState(cmd);
                return null;
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterHostConnected(final HostInventory host) {
        //syncVm has done the same work, so abandon it
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                syncVm(context.getInventory(), new Completion(trigger) {
                    String __name__ = "sync-vm-state";

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
        };
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        if (!KVMGlobalConfig.VM_SYNC_ON_HOST_PING.value(Boolean.class)) {
            completion.done();
            return;
        }

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-vm-state-after-ping-host-%s-success", host.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                syncVm(host, new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to sync VM states on the host[uuid:%s, name:%s], %s",
                                host.getUuid(), host.getName(), errorCode));
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
        completion.done();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        vmApis.putIfAbsent(inv.getUuid(), new ConcurrentHashMap<>());
        vmsToSkip.putIfAbsent(inv.getUuid(), ConcurrentHashMap.newKeySet());
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        vmApis.remove(inv.getUuid());
        vmsToSkip.remove(inv.getUuid());
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
        vmApis.putIfAbsent(inv.getUuid(), new ConcurrentHashMap<>());
        vmsToSkip.putIfAbsent(inv.getUuid(), ConcurrentHashMap.newKeySet());
    }
}
