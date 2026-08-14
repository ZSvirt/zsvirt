package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmSchedHistoryRecorder;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CanonicalEventEmitter;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.allocator.AllocationScene;
import org.zstack.header.allocator.HostAllocatorConstant;
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
import org.zstack.header.host.HostCanonicalEvents.HostDeletedData;
import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData;
import org.zstack.header.host.HostErrors.Opaque;
import org.zstack.header.host.HostMaintenancePolicyExtensionPoint.HostMaintenancePolicy;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.*;
import org.zstack.utils.data.Pair;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class HostBase extends AbstractHost {
    protected static final CLogger logger = Utils.getLogger(HostBase.class);
    protected HostVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected HostExtensionPointEmitter extpEmitter;
    @Autowired
    protected GlobalConfigFacade gcf;
    @Autowired
    protected HostManager hostMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected HostTracker tracker;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected HostMaintenancePolicyManager hostMaintenancePolicyMgr;

    public static class HostDisconnectedCanonicalEvent extends CanonicalEventEmitter {
        HostCanonicalEvents.HostDisconnectedData data;

        public HostDisconnectedCanonicalEvent(String hostUuid, ErrorCode reason) {
            data = new HostCanonicalEvents.HostDisconnectedData();
            data.hostUuid = hostUuid;
            data.reason = reason;
        }

        public void fire() {
            fire(HostCanonicalEvents.HOST_DISCONNECTED_PATH, data);
        }
    }

    public static class HostHardwareChangedCanonicalEvent extends CanonicalEventEmitter {
        HostCanonicalEvents.HostHardwareChangedData data;

        public HostHardwareChangedCanonicalEvent(String hostUuid, ErrorCode reason) {
            data = new HostCanonicalEvents.HostHardwareChangedData();
            data.hostUuid = hostUuid;
            data.reason = reason;
        }

        public void fire() {
            fire(HostCanonicalEvents.HOST_HARDWARE_CHANGED_PATH, data);
        }
    }

    protected final String id;

    protected abstract void pingHook(Completion completion);

    protected abstract void deleteTakeOverFlag(Completion completion);

    protected abstract int getVmMigrateQuantity();

    protected abstract void changeStateHook(HostState current, HostStateEvent stateEvent, HostState next);

    protected void checkConnectConditions(ConnectHostInfo info, Completion complete) {
        complete.success();
    }

    protected abstract void connectHook(ConnectHostInfo info, Completion complete);

    protected abstract void updateOsHook(UpdateHostOSMsg msg, Completion completion);

    protected HostBase(HostVO self) {
        this.self = self;
        id = Host.buildId(self.getUuid());
    }

    @Override
    public String getId() {
        return id;
    }

    protected void checkStatus() {
        if (HostStatus.Connected != self.getStatus()) {
            ErrorCode cause = err(HostErrors.HOST_IS_DISCONNECTED, "host[uuid:%s, name:%s] is in status[%s], cannot perform required operation", self.getUuid(), self.getName(), self.getStatus());
            throw err(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE, "unable to do the operation because the host is in status of Disconnected")
                    .withCause(cause)
                    .toException();
        }
    }

    protected void checkState() {
        if (HostState.PreMaintenance == self.getState() || HostState.Maintenance == self.getState()) {
            throw operr("host[uuid:%s, name:%s] is in state[%s], cannot perform required operation", self.getUuid(), self.getName(), self.getState())
                    .toException();
        }
    }

    protected void checkStateAndStatus() {
        checkState();
        checkStatus();
    }

    protected abstract int getHostSyncLevel();

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeHostStateMsg) {
            handle((APIChangeHostStateMsg) msg);
        } else if (msg instanceof APIDeleteHostMsg) {
            handle((APIDeleteHostMsg) msg);
        } else if (msg instanceof APIReconnectHostMsg) {
            handle((APIReconnectHostMsg) msg);
        } else if (msg instanceof APIUpdateHostMsg) {
            handle((APIUpdateHostMsg) msg);
        } else if (msg instanceof APIUpdateHostIpmiMsg) {
            handle((APIUpdateHostIpmiMsg) msg);
        } else if (msg instanceof APIShutdownHostMsg) {
            handle((APIShutdownHostMsg) msg);
        } else if (msg instanceof APIPowerOnHostMsg ) {
            handle((APIPowerOnHostMsg) msg);
        } else if (msg instanceof APIPowerResetHostMsg) {
            handle((APIPowerResetHostMsg) msg);
        } else if (msg instanceof APIGetHostPowerStatusMsg) {
            handle((APIGetHostPowerStatusMsg) msg);
        } else if (msg instanceof APIUpdateHostNqnMsg) {
            handle((APIUpdateHostNqnMsg) msg);
        } else if (msg instanceof APIUpdateHostnameMsg) {
            handle((APIUpdateHostnameMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateHostnameMsg msg) {
        APIUpdateHostnameEvent event = new APIUpdateHostnameEvent(msg.getId());
        UpdateHostnameMsg umsg = new UpdateHostnameMsg();
        umsg.setUuid(msg.getUuid());
        umsg.setHostname(msg.getHostname());
        bus.makeTargetServiceIdByResourceUuid(umsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(umsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                UpdateHostnameReply r = reply.castReply();
                if (!r.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(r.getError());
                } else {
                    event.setInventory(r.getInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIUpdateHostNqnMsg msg) {
        APIUpdateHostNqnEvent event = new APIUpdateHostNqnEvent(msg.getId());
        UpdateHostNqnMsg umsg = new UpdateHostNqnMsg();
        umsg.setUuid(msg.getUuid());
        umsg.setNqn(msg.getNqn());
        bus.makeTargetServiceIdByResourceUuid(umsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(umsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                UpdateHostNqnReply r = reply.castReply();
                if (!r.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(r.getError());
                } else {
                    event.setInventory(r.getInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetHostPowerStatusMsg msg) {
        APIGetHostPowerStatusEvent event = new APIGetHostPowerStatusEvent(msg.getId());
        GetHostPowerStatusMsg getPowerStatusMsg = new GetHostPowerStatusMsg();
        getPowerStatusMsg.setUuid(msg.getUuid());
        getPowerStatusMsg.setMethod(HostPowerManagementMethod.valueOf(msg.getMethod()));
        bus.makeTargetServiceIdByResourceUuid(getPowerStatusMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(getPowerStatusMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                GetHostPowerStatusReply r = reply.castReply();
                if (!r.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(r.getError());
                } else {
                    event.setInventory(r.getInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIPowerResetHostMsg msg) {
        final APIPowerResetHostEvent event = new APIPowerResetHostEvent(msg.getId());
        RebootHostMsg rebootHostMsg = new RebootHostMsg();
        rebootHostMsg.setUuid(msg.getHostUuid());
        rebootHostMsg.setReturnEarly(msg.isReturnEarly());
        rebootHostMsg.setMethod(HostPowerManagementMethod.valueOf(msg.getMethod()));
        bus.makeTargetServiceIdByResourceUuid(rebootHostMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(rebootHostMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                RebootHostReply rebootReply = reply.castReply();
                if (!rebootReply.isSuccess()) {
                    event.setError(rebootReply.getError());
                    event.setSuccess(rebootReply.isSuccess());
                } else {
                    self = dbf.findByUuid(msg.getUuid(), HostVO.class);
                    event.setInventory(getSelfInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIPowerOnHostMsg msg) {
        final APIPowerOnHostEvent event = new APIPowerOnHostEvent(msg.getId());
        PowerOnHostMsg powerOnHostMsg = new PowerOnHostMsg();
        powerOnHostMsg.setUuid(msg.getHostUuid());
        powerOnHostMsg.setReturnEarly(msg.isReturnEarly());
        bus.makeTargetServiceIdByResourceUuid(powerOnHostMsg, HostConstant.SERVICE_ID, powerOnHostMsg.getHostUuid());
        bus.send(powerOnHostMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                PowerOnHostReply powerOnHostReply = reply.castReply();
                if (null != powerOnHostReply.getError()) {
                    event.setError(powerOnHostReply.getError());
                    event.setSuccess(powerOnHostReply.isSuccess());
                } else {
                    self = dbf.findByUuid(msg.getUuid(), HostVO.class);
                    event.setInventory(getSelfInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIShutdownHostMsg msg) {
        final APIShutdownHostEvent event = new APIShutdownHostEvent(msg.getId());
        ShutdownHostMsg shutdownHostMsg = new ShutdownHostMsg();
        shutdownHostMsg.setMethod(HostPowerManagementMethod.valueOf(msg.getMethod()));
        shutdownHostMsg.setWaitTaskCompleted(!msg.isReturnEarly());
        shutdownHostMsg.setReturnEarly(msg.isReturnEarly());
        shutdownHostMsg.setUuid(msg.getUuid());
        shutdownHostMsg.setForce(msg.isForce());
        bus.makeTargetServiceIdByResourceUuid(shutdownHostMsg, HostConstant.SERVICE_ID, shutdownHostMsg.getUuid());
        bus.send(shutdownHostMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                ShutdownHostReply shutdownHostReply = reply.castReply();
                if (null != shutdownHostReply.getError()) {
                    event.setError(shutdownHostReply.getError());
                    event.setSuccess(false);
                } else {
                    self = dbf.findByUuid(msg.getUuid(), HostVO.class);
                    event.setInventory(getSelfInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIUpdateHostIpmiMsg msg) {
        final APIUpdateHostIpmiEvent event = new APIUpdateHostIpmiEvent(msg.getId());
        HostIpmiVO ipmi = dbf.findByUuid(msg.getHostUuid(), HostIpmiVO.class);
        if (null == ipmi.getIpmiAddress()) {
            event.setSuccess(false);
            event.setError(operr("host[%s] does not have ipmi device or ipmi does not have address." +
                    "After config ipmi address, please reconnect host to refresh " +
                    "host ipmi information", msg.getHostUuid()));
            bus.publish(event);
            return;
        }

        if (null != msg.getIpmiAddress()) {
            ipmi.setIpmiAddress(msg.getIpmiAddress());
        }
        if (null != msg.getIpmiUsername()) {
            ipmi.setIpmiUsername(msg.getIpmiUsername());
        }
        if (null != msg.getIpmiPassword()) {
            ipmi.setIpmiPassword(msg.getIpmiPassword());
        }
        if (0 != msg.getIpmiPort()) {
            ipmi.setIpmiPort(msg.getIpmiPort());
        }

        Pair<HostPowerStatus, ErrorCode> pair = HostIpmiPowerExecutor.getPowerStatusWithErrorCode(ipmi);
        ErrorCode err = pair.second();
        if (err != null) {
            event.setSuccess(false);
            event.setError(err);
            bus.publish(event);
            return;
        }

        HostPowerStatus status = pair.first();
        ipmi.setIpmiPowerStatus(status);
        ipmi = dbf.updateAndRefresh(ipmi);
        event.setHostIpmiInventory(HostIpmiInventory.valueOf(ipmi));
        bus.publish(event);
    }

    protected HostVO updateHost(APIUpdateHostMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getManagementIp() != null) {
            self.setManagementIp(msg.getManagementIp());
            update = true;
        }

        return update ? self : null;
    }

    private void handle(APIUpdateHostMsg msg) {
        APIUpdateHostEvent evt = new APIUpdateHostEvent(msg.getId());
        HostInventory oldHost = getSelfInventory();
        HostVO vo = updateHost(msg);
        if (vo == null) {
            evt.setInventory(getSelfInventory());
            bus.publish(evt);
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("after-host-%s-updated", self.getUuid()));
        chain.allowEmptyFlow();

        for (HostAfterUpdatedExtensionPoint ext : pluginRgty.getExtensionList(HostAfterUpdatedExtensionPoint.class)) {
            chain.then(ext.afterHostUpdated(oldHost, HostInventory.valueOf(vo)));
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                self = dbf.updateAndRefresh(vo);
                evt.setInventory(getSelfInventory());
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

    private String getVmHostUuid(String vmUuid) {
        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
    }

    protected void maintenanceHook(ChangeHostStateMsg changeHostStateMsg, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("maintenance-mode-host-%s-ip-%s", self.getUuid(), self.getManagementIp()));

        List<String> operateVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, self.getUuid())
                .notEq(VmInstanceVO_.state, VmInstanceState.Unknown)
                .listValues();

        for (HostMaintenanceExtensionPoint ext : pluginRgty.getExtensionList(HostMaintenanceExtensionPoint.class)) {
            ext.beforeCheckMaintenancePolicy(operateVmUuids);
        }

        Map<HostMaintenancePolicy, Set<String>> policyVmMap = map(
                e(HostMaintenancePolicy.MigrateVm, new HashSet<>(operateVmUuids)),
                e(HostMaintenancePolicy.StopVm, new HashSet<>()));
        for (HostMaintenancePolicyExtensionPoint ext : pluginRgty.getExtensionList(HostMaintenancePolicyExtensionPoint.class)) {
            Map<String, HostMaintenancePolicy> vmUuidPolicyMap = ext.getHostMaintenanceVmOperationPolicy(getSelfInventory());
            logger.debug(String.format("HostMaintenancePolicyExtensionPoint[%s] preset maintenance policy for host[uuid:%s] to %s",
                    ext.getClass(), self.getUuid(), vmUuidPolicyMap));
            vmUuidPolicyMap.forEach((vmUuid, policy) -> policyVmMap.get(policy).add(vmUuid));
        }

        // default policy is migrate, but stop policy setting is high priority
        policyVmMap.values().forEach(vmUuids -> vmUuids.retainAll(operateVmUuids));
        policyVmMap.get(HostMaintenancePolicy.MigrateVm).removeAll(policyVmMap.get(HostMaintenancePolicy.StopVm));

        final int quantity = getVmMigrateQuantity();
        DebugUtils.Assert(quantity != 0, "getVmMigrateQuantity() cannot return 0");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "try-migrate-vm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<String> vmUuids = new ArrayList<>(policyVmMap.get(HostMaintenancePolicy.MigrateVm));
                        if (vmUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        int migrateQuantity = quantity;
                        HostInventory host = getSelfInventory();
                        Map<String, ErrorCode> vmFailedToMigrate = Collections.synchronizedMap(new HashMap<>());
                        for (OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint ext : pluginRgty.getExtensionList(OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint.class)) {
                            List<String> ordered = ext.orderVmBeforeMigrationDuringHostMaintenance(host, vmUuids);
                            if (ordered != null) {
                                vmUuids = ordered;

                                logger.debug(String.format("%s ordered VMs for host maintenance, to keep the order, we will migrate VMs one by one",
                                        ext.getClass()));
                                migrateQuantity = 1;
                            }
                        }

                        // Migrate applianceVM first.
                        final List<String> applianceVMs = Q.New(VmInstanceVO.class)
                                .select(VmInstanceVO_.uuid)
                                .notEq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                                .in(VmInstanceVO_.uuid, vmUuids)
                                .listValues();
                        if (!applianceVMs.isEmpty()) {
                            vmUuids.removeAll(applianceVMs);
                            vmUuids.addAll(0, applianceVMs);
                        }

                        new While<>(vmUuids).step((vmUuid, compl) -> {
                            VmSchedHistoryRecorder recorder = VmSchedHistoryRecorder.ofHostMaintenance(vmUuid)
                                    .withSchedReason(i18n("Host[%s] is in maintenance state, VM on this host should be migrated", host.getName()))
                                    .begin();
                            MigrateVmMsg msg = new MigrateVmMsg();
                            msg.setVmInstanceUuid(vmUuid);
                            msg.setAllocationScene(AllocationScene.Auto);
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                            bus.send(msg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        vmFailedToMigrate.put(msg.getVmInstanceUuid(), reply.getError());
                                        recorder.withFailReason(reply.getError().getDetails())
                                                .end(null);
                                    } else {
                                        recorder.end(getVmHostUuid(vmUuid));
                                    }
                                    compl.done();
                                }
                            });
                        }, migrateQuantity).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!vmFailedToMigrate.isEmpty()) {
                                    if (HostMaintenancePolicyManager.HostMaintenancePolicy.JustMigrate.equals(hostMaintenancePolicyMgr.getHostMaintenancePolicy(self.getUuid()))) {
                                        trigger.fail(operr("failed to migrate vm[uuids:%s] on host[uuid:%s, name:%s, ip:%s], will try stopping it.",
                                                vmFailedToMigrate.keySet(), self.getUuid(), self.getName(), self.getManagementIp()));
                                        return;
                                    }

                                    vmFailedToMigrate.forEach((vmUuid, errorCode) -> {
                                        VmTracerCanonicalEvents.MigrateVMFailedWithHostMaintainData data = new VmTracerCanonicalEvents.MigrateVMFailedWithHostMaintainData();
                                        data.setVmUuid(vmUuid);
                                        data.setHostUuid(self.getUuid());
                                        data.setReason(String.format("failed to migrate vm[%s] when host[%s] enters the maintenance state, reason: %s", vmUuid, self.getUuid(), errorCode.getReadableDetails()));
                                        evtf.fire(VmTracerCanonicalEvents.MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_PATH, data);
                                    });

                                    logger.warn(String.format("failed to migrate vm[uuids:%s] on host[uuid:%s, name:%s, ip:%s], will try stopping it.",
                                            vmFailedToMigrate.keySet(), self.getUuid(), self.getName(), self.getManagementIp()));
                                    policyVmMap.get(HostMaintenancePolicy.StopVm).addAll(vmFailedToMigrate.keySet());
                                    policyVmMap.get(HostMaintenancePolicy.MigrateVm).removeAll(vmFailedToMigrate.keySet());
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "stop-vm-not-migrated";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (policyVmMap.get(HostMaintenancePolicy.StopVm).isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<String> vmUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid)
                                .in(VmInstanceVO_.uuid, policyVmMap.get(HostMaintenancePolicy.StopVm)).listValues();
                        stopFailedToMigrateVms(vmUuids, trigger);
                    }

                    private void stopFailedToMigrateVms(List<String> vmUuids, final FlowTrigger trigger) {
                        List<ErrorCode> errors = Collections.synchronizedList(new ArrayList<>());
                        List<String> vmFailedToStop = Collections.synchronizedList(new ArrayList<>());
                        new While<>(vmUuids).step((vmUuid, coml) -> {
                            StopVmInstanceMsg msg = new StopVmInstanceMsg();
                            msg.setVmInstanceUuid(vmUuid);
                            if (changeHostStateMsg.isForceChange()) {
                                msg.setGcOnFailure(true);
                                msg.setIgnoreResourceReleaseFailure(true);
                            }
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                            bus.send(msg, new CloudBusCallBack(coml) {
                                @Override
                                public void run(MessageReply reply) {
                                    logger.debug(String.format("stop vm[uuid:%s] after migration failure", vmUuid));
                                    if (!reply.isSuccess() && dbf.isExist(vmUuid, VmInstanceVO.class)) {
                                        errors.add(reply.getError());
                                        vmFailedToStop.add(vmUuid);
                                    }
                                    coml.done();
                                }
                            });
                        }, quantity).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errors.isEmpty() || HostGlobalConfig.IGNORE_ERROR_ON_MAINTENANCE_MODE.value(Boolean.class)) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                                            String.format("failed to stop vm[uuids:%s] on host[uuid:%s, name:%s, ip:%s]",
                                                    vmFailedToStop, self.getUuid(), self.getName(), self.getManagementIp()),
                                            errors));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "run-extension-point-after-maintenance";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        runExtensionPoints(pluginRgty.getExtensionList(HostAfterMaintenanceExtensionPoint.class).iterator(), new Completion(trigger) {
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

                    private void runExtensionPoints(Iterator<HostAfterMaintenanceExtensionPoint> it, Completion completion1) {
                        if (!it.hasNext()) {
                            completion1.success();
                            return;
                        }

                        HostAfterMaintenanceExtensionPoint ext = it.next();
                        ext.afterMaintenanceExtensionPoint(getSelfInventory(), new Completion(completion1) {
                            @Override
                            public void success() {
                                runExtensionPoints(it, completion1);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion1.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIReconnectHostMsg msg) {
        ReconnectHostMsg rmsg = new ReconnectHostMsg();
        rmsg.setHostUuid(self.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, HostConstant.SERVICE_ID, self.getUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIReconnectHostEvent evt = new APIReconnectHostEvent(msg.getId());
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    self = dbf.reload(self);
                    evt.setInventory((getSelfInventory()));
                }
                bus.publish(evt);
            }
        });
    }

    private void deleteHostByApiMessage(APIDeleteHostMsg msg) {
        final APIDeleteHostEvent evt = new APIDeleteHostEvent(msg.getId());

        final String issuer = HostVO.class.getSimpleName();
        final List<HostInventory> ctx = Arrays.asList(HostInventory.valueOf(self));

        if (self.getState() == HostState.Enabled) {
            changeState(HostStateEvent.disable);
        }
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-host-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                deleteTakeOverFlag(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("Failed to delete the takeover flag. The reason is:[%s], please delete the flag manually", errorCode));
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(msg) {
            private void complete() {
                bus.publish(evt);

                HostDeletedData d = new HostDeletedData();
                d.setInventory(HostInventory.valueOf(self));
                d.setHostUuid(self.getUuid());
                evtf.fire(HostCanonicalEvents.HOST_DELETED_PATH, d);
            }

            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new Completion(msg) {
                    @Override
                    public void success() {
                        complete();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        complete();
                    }
                });
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(final APIDeleteHostMsg msg) {
        deleteHostByApiMessage(msg);
    }

    protected HostState changeState(HostStateEvent event) {
        HostState currentState = self.getState();
        HostState next = currentState.nextState(event);
        changeStateHook(currentState, event, next);

        extpEmitter.beforeChange(self, event);
        self.setState(next);
        self = dbf.updateAndRefresh(self);
        extpEmitter.afterChange(self, event, currentState);
        logger.debug(String.format("Host[%s]'s state changed from %s to %s", self.getUuid(), currentState, self.getState()));
        return self.getState();
    }

    private boolean doChangeStateAndCheckHostOutOfMaintenance(HostStateEvent stateEvent) {
        HostState origState = self.getState();
        HostState state = changeState(stateEvent);

        return (origState == HostState.Maintenance || origState == HostState.PreMaintenance) && state != HostState.Maintenance;
    }

    protected void handle(final APIChangeHostStateMsg msg) {
        APIChangeHostStateEvent evt = new APIChangeHostStateEvent(msg.getId());

        HostStateEvent stateEvent = HostStateEvent.valueOf(msg.getStateEvent());
        stateEvent = stateEvent == HostStateEvent.maintain ? HostStateEvent.preMaintain : stateEvent;
        try {
            extpEmitter.preChange(self, stateEvent);
        } catch (HostException he) {
            evt.setError(err(SysErrors.CHANGE_RESOURCE_STATE_ERROR, he.getMessage()));
            bus.publish(evt);
            return;
        }

        ChangeHostStateMsg cmsg = new ChangeHostStateMsg();
        cmsg.setStateEvent(stateEvent.toString());
        cmsg.setUuid(msg.getUuid());
        cmsg.setJustChangeState(false);
        bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, cmsg.getUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }

                evt.setInventory(((ChangeHostStateReply) reply).getInventory());
                bus.publish(evt);
            }
        });
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ChangeHostStateMsg) {
            handle((ChangeHostStateMsg) msg);
        } else if (msg instanceof HostDeletionMsg) {
            handle((HostDeletionMsg) msg);
        } else if (msg instanceof ConnectHostMsg) {
            handle((ConnectHostMsg) msg);
        } else if (msg instanceof ReconnectHostMsg) {
            handle((ReconnectHostMsg) msg);
        } else if (msg instanceof ChangeHostConnectionStateMsg) {
            handle((ChangeHostConnectionStateMsg) msg);
        } else if (msg instanceof PingHostMsg) {
            handle((PingHostMsg) msg);
        } else if (msg instanceof ScanVmPortMsg) {
            handle((ScanVmPortMsg) msg);
        } else if (msg instanceof UpdateHostOSMsg) {
            handle((UpdateHostOSMsg) msg);
        } else if (msg instanceof ChangeHostStatusMsg) {
            handle((ChangeHostStatusMsg) msg);
        } else {
            HostBaseExtensionFactory ext = hostMgr.getHostBaseExtensionFactory(msg);
            if (ext != null) {
                Host h = ext.getHost(self);
                if (h != null) {
                    h.handleMessage(msg);
                    return;
                }
            }

            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ChangeHostStatusMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                ChangeHostStatusReply reply = new ChangeHostStatusReply();
                changeConnectionState(HostStatusEvent.valueOf(msg.getStatusEvent()));
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return String.format("change-host-%s-status", self.getUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(UpdateHostOSMsg msg) {
        UpdateHostOSReply reply = new UpdateHostOSReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "update-host-os-of-cluster-" + msg.getClusterUuid();
            }

            @Override
            public int getSyncLevel() {
                return HostGlobalConfig.HOST_UPDATE_OS_PARALLELISM_DEGREE.value(Integer.class);
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateOsHook(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
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
                return getSyncSignature();
            }
        });
    }

    protected void scanVmPorts(final ScanVmPortMsg msg) {
        ScanVmPortReply reply = new ScanVmPortReply();
        reply.setSupportScan(false);
        bus.reply(msg, reply);
    }

    private void handle(final ScanVmPortMsg msg) {
        scanVmPorts(msg);
    }

    private void doPingHost(final PingHostMsg msg, ReturnValueCompletion<PingHostReply> completion) {
        final PingHostReply reply = new PingHostReply();
        if (self.getStatus() == HostStatus.Connecting) {
            completion.fail(operr("host is connecting, ping failed"));
            return;
        }

        final Integer MAX_PING_CNT = HostGlobalConfig.MAXIMUM_PING_FAILURE.value(Integer.class);

        final List<ErrorCode> errs = new ArrayList<>();
        While.makeRetryWhile(MAX_PING_CNT).each((currentStep, compl) -> pingHook(new Completion(compl) {
            @Override
            public void success() {
                compl.allDone();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("ping host failed (%d/%d): %s",
                        currentStep, MAX_PING_CNT, errorCode.getReadableDetails()));
                errs.add(errorCode);

                if (errs.size() != MAX_PING_CNT) {
                    int sleep = HostGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.value(Integer.class);
                    if (sleep > 0) {
                        try {
                            TimeUnit.SECONDS.sleep(sleep);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                compl.done();
            }
        })).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() == MAX_PING_CNT) {
                    final ErrorCode errorCode = errs.get(0);
                    reply.setConnected(false);
                    reply.setCurrentHostStatus(self.getStatus().toString());
                    reply.setError(errorCode);
                    reply.setSuccess(true);

                    Boolean noReconnect = (Boolean) errorCode.getFromOpaque(Opaque.NO_RECONNECT_AFTER_PING_FAILURE.toString());
                    reply.setNoReconnect(noReconnect != null && noReconnect);

                    if (!Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).isExists()) {
                        reply.setNoReconnect(true);
                        completion.success(reply);
                        return;
                    }

                    if (!self.getStatus().equals(HostStatus.Disconnected)) {
                        new HostDisconnectedCanonicalEvent(self.getUuid(), errorCode).fire();
                    }

                    changeConnectionState(HostStatusEvent.disconnected);

                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(PingHostFailedExtensionPoint.class),
                            extension -> extension.afterPingHostFailed(self.getUuid(), errorCode));

                    completion.success(reply);
                } else {
                    reply.setConnected(true);
                    reply.setCurrentHostStatus(self.getStatus().toString());
                    completion.success(reply);

                    extpEmitter.hostPingTask(HypervisorType.valueOf(self.getHypervisorType()), getSelfInventory());
                }
            }
        });
    }

    private void handle(final PingHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "do-ping-host";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doPingHost(msg, new ReturnValueCompletion<PingHostReply>(msg, chain) {
                    @Override
                    public void success(PingHostReply reply) {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        PingHostReply reply = new PingHostReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("do-ping-host-%s", msg.getHostUuid());
            }

            @Override
            protected int getSyncLevel() {
                return HostGlobalConfig.HOST_TRACK_PARALLELISM_DEGREE.value(Integer.class);
            }
        });
    }

    private void handle(final HostDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                HostInventory hinv = HostInventory.valueOf(self);
                extpEmitter.beforeDelete(hinv);
                deleteHook();
                extpEmitter.afterDelete(hinv);
                bus.reply(msg, new HostDeletionReply());
                tracker.untrackHost(self.getUuid());
                chain.next();
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }

            @Override
            public String getName() {
                return String.format("host-deletion-%s", self.getUuid());
            }
        });
    }

    private boolean skipReconnectHost(final ReconnectHostMsg msg) {
        if (msg.isSkipIfHostConnected() && HostStatus.Connected == self.getStatus()) {
            return true;
        }

        if (skipConnectHost()) {
            return true;
        }

        return false;
    }

    private boolean skipConnectHost() {
        if (UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            AgentVersionVO agentVersionVO = dbf.findByUuid(self.getUuid(), AgentVersionVO.class);
            if (agentVersionVO == null) {
                return true;
            }
        }

        return false;
    }


    private void reconnectHost(final ReconnectHostMsg msg, final NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(msg, completion) {
            @Override
            public String getSyncSignature() {
                return String.format("reconnect-host-%s", self.getUuid());
            }

            private void finish(SyncTaskChain chain) {
                chain.next();
                completion.done();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                checkState();

                if (skipReconnectHost(msg)) {
                    bus.reply(msg, new ReconnectHostReply());
                    finish(chain);
                    return;
                }

                ConnectHostMsg connectMsg = new ConnectHostMsg(self.getUuid());
                connectMsg.setNewAdd(false);
                bus.makeTargetServiceIdByResourceUuid(connectMsg, HostConstant.SERVICE_ID, self.getUuid());
                bus.send(connectMsg, new CloudBusCallBack(msg, chain, completion) {
                    @Override
                    public void run(MessageReply reply) {
                        ReconnectHostReply r = new ReconnectHostReply();
                        if (!reply.isSuccess()) {
                            r.setError(reply.getError());
                        }
                        bus.reply(msg, r);
                    }
                });

                // no need to block the queue, because the ConnectHostMsg will be queued as well
                finish(chain);
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void updateHostConnectedTime(HostStatus hostStatus) {
        boolean tagExists = HostSystemTags.HOST_CONNECTED_TIME.hasTag(self.getUuid());
        if (hostStatus == HostStatus.Connected && !tagExists) {
            long connectedTime = System.currentTimeMillis();
            String token = HostSystemTags.HOST_CONNECTED_TIME_TOKEN;
            SystemTagCreator creator = HostSystemTags.HOST_CONNECTED_TIME.newSystemTagCreator(self.getUuid());
            creator.setTagByTokens(Collections.singletonMap(token, connectedTime));
            creator.inherent = false;
            creator.recreate = false;
            creator.create();
            return;
        }
        if (hostStatus == HostStatus.Disconnected) {
            SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, self.getUuid())
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, "ConnectedTime::%")
                .hardDelete();
        }
    }

    private void handle(final ReconnectHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return "reconnect-host-" + self.getUuid();
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                reconnectHost(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void handle(final ChangeHostConnectionStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return "change-host-connection-state-" + self.getUuid();
            }

            private void reestablishConnection() {
                try {
                    extpEmitter.connectionReestablished(HypervisorType.valueOf(self.getHypervisorType()), getSelfInventory());
                } catch (HostException e) {
                    throw new OperationFailureException(operr(e.getMessage()));
                }
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                HostStatusEvent cevt = HostStatusEvent.valueOf(msg.getConnectionStateEvent());
                HostStatus next = self.getStatus().nextStatus(cevt);
                if (self.getStatus() == HostStatus.Disconnected && next == HostStatus.Connected) {
                    try {
                        reestablishConnection();
                    } catch (OperationFailureException e) {
                        cevt = HostStatusEvent.disconnected;
                        new HostDisconnectedCanonicalEvent(self.getUuid(), e.getErrorCode()).fire();
                    }
                }

                changeConnectionState(cevt);
                bus.reply(msg, new ChangeHostConnectionStateReply());
                chain.next();
            }

            @Override
            public int getSyncLevel() {
                return getHostSyncLevel();
            }

        });
    }

    protected abstract HostInventory getSelfInventory();

    protected boolean changeConnectionState(final HostStatusEvent event) {
        return changeConnectionState(event, null);
    }

    protected boolean changeConnectionState(final HostStatusEvent event, Runnable runnable) {
        String hostUuid = self.getUuid();
        self = dbf.reload(self);
        if (self == null) {
            throw new CloudRuntimeException(String.format("change host connection state fail, can not find the host[%s]", hostUuid));
        }

        HostStatus before = self.getStatus();
        HostStatus next = before.nextStatus(event);
        if (before == next) {
            return false;
        }

        self.setStatus(next);
        Optional.ofNullable(runnable).ifPresent(Runnable::run);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("Host %s [uuid:%s] changed connection state from %s to %s",
                self.getName(), self.getUuid(), before, next));
        updateHostConnectedTime(next);

        HostStatusChangedData data = new HostStatusChangedData();
        data.setHostUuid(self.getUuid());
        data.setNewStatus(next.toString());
        data.setOldStatus(before.toString());
        data.setInventory(HostInventory.valueOf(self));
        evtf.fire(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, data);

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterChangeHostStatusExtensionPoint.class),
                new ForEachFunction<AfterChangeHostStatusExtensionPoint>() {
                    @Override
                    public void run(AfterChangeHostStatusExtensionPoint arg) {
                        arg.afterChangeHostStatus(self.getUuid(), before, next);
                    }
                });
        return true;
    }

    private void handle(final ConnectHostMsg msg) {
        thdf.singleFlightSubmit(new SingleFlightTask(msg)
                .setSyncSignature(String.format("connect-host-%s-single-flight", msg.getHostUuid()))
                .run((completion) -> connect(msg, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success(null);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                }))
                .done(((result) -> {
                    ConnectHostReply reply = new ConnectHostReply();

                    if (!result.isSuccess()) {
                        reply.setError(result.getErrorCode());
                    }

                    bus.reply(msg, reply);
                })));
    }

    private String connectHostSignature() {
        return String.format("connect-host-%s", self.getUuid());
    }

    private void connect(final ConnectHostMsg msg, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return connectHostSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                checkState();

                if (skipConnectHost()) {
                    completion.success();
                    chain.next();
                    return;
                }

                final FlowChain flowChain = FlowChainBuilder.newShareFlowChain();
                flowChain.setName(String.format("connect-host-%s", self.getUuid()));
                flowChain.allowWatch();
                flowChain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        flow(new NoRollbackFlow() {
                            String __name__ = "check-conditions-of-connection";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                checkConnectConditions(ConnectHostInfo.fromConnectHostMsg(msg), new Completion(trigger) {
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

                        flow(new NoRollbackFlow() {
                            String __name__ = "call-pre-connect-extensions";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                FlowChain preConnectChain = FlowChainBuilder.newSimpleFlowChain();
                                preConnectChain.allowEmptyFlow();

                                for (PreHostConnectExtensionPoint p : pluginRgty.getExtensionList(PreHostConnectExtensionPoint.class)) {
                                    Flow flow = p.createPreHostConnectFlow(msg);
                                    if (flow != null) {
                                        preConnectChain.then(flow);
                                    }
                                }

                                preConnectChain.done(new FlowDoneHandler(trigger) {
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
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "call-host-connect-hooks";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                changeConnectionState(HostStatusEvent.connecting);
                                connectHook(ConnectHostInfo.fromConnectHostMsg(msg), new Completion(trigger) {
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

                        flow(new NoRollbackFlow() {
                            String __name__ = "call-post-connect-extensions";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                FlowChain postConnectChain = FlowChainBuilder.newSimpleFlowChain();
                                postConnectChain.allowEmptyFlow();

                                self = dbf.reload(self);
                                HostInventory inv = getSelfInventory();

                                for (PostHostConnectExtensionPoint p : pluginRgty.getExtensionList(PostHostConnectExtensionPoint.class)) {
                                    Flow flow = p.createPostHostConnectFlow(inv);
                                    if (flow != null) {
                                        postConnectChain.then(flow);
                                    }
                                }

                                postConnectChain.done(new FlowDoneHandler(trigger) {
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
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "recalculate-host-capacity";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
                                msg.setHostUuid(self.getUuid());
                                bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
                                bus.send(msg);
                                trigger.next();
                            }
                        });

                        done(new FlowDoneHandler(completion) {
                            @Override
                            public void handle(Map data) {
                                changeConnectionState(HostStatusEvent.connected);
                                tracker.trackHost(self.getUuid());

                                CollectionUtils.safeForEach(pluginRgty.getExtensionList(HostAfterConnectedExtensionPoint.class),
                                        ext -> ext.afterHostConnected(getSelfInventory()));
                                completion.success();
                            }
                        });

                        error(new FlowErrorHandler(completion) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                changeConnectionState(HostStatusEvent.disconnected);
                                if (!msg.isNewAdd()) {
                                    connectHostFailHook(errCode);
                                }
                                completion.fail(errCode);
                            }
                        });

                        Finally(new FlowFinallyHandler(chain) {
                            @Override
                            public void Finally() {
                                chain.next();
                            }
                        });
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "connect-host";
            }

            @Override
            protected String getDeduplicateString() {
                return String.format("connect-host-%s", self.getUuid());
            }
        });
    }

    protected void connectHostFailHook(ErrorCode errorCode) {
        tracker.trackHost(self.getUuid());
        new HostDisconnectedCanonicalEvent(self.getUuid(), errorCode).fire();
    }

    private void handle(final ChangeHostStateMsg msg) {
        ChangeHostStateReply reply = new ChangeHostStateReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("change-host-state-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doHostStateChange(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        reply.setInventory(HostInventory.valueOf(self));
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
                return String.format("change-host-state-%s", self.getUuid());
            }
        });
    }

    private void doHostStateChange(ChangeHostStateMsg msg, Completion completion) {
        HostStateEvent stateEvent = HostStateEvent.valueOf(msg.getStateEvent());

        if (msg.isJustChangeState()) {
            if (self.getState() == HostState.Enabled || self.getState() == HostState.Disabled) {
                changeState(stateEvent);
            }
            completion.success();
        } else if (HostStateEvent.preMaintain == stateEvent) {
            HostState originState = self.getState();
            changeState(HostStateEvent.preMaintain);
            maintenanceHook(msg, new Completion(msg) {
                @Override
                public void success() {
                    changeState(HostStateEvent.maintain);
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    HostStateEvent rollbackEvent = dbf.reload(self).getState().getTargetStateDrivenEvent(originState);
                    DebugUtils.Assert(rollbackEvent != null, "rollbackEvent not found!");
                    changeState(rollbackEvent);
                    completion.fail(err(HostErrors.UNABLE_TO_ENTER_MAINTENANCE_MODE, errorCode.getDetails()).withCause(errorCode));
                }
            });
        } else {
            if (doChangeStateAndCheckHostOutOfMaintenance(stateEvent)) {
                // host is out of maintenance mode, track and reconnect it.
                tracker.trackHost(self.getUuid());

                // change host status to connecting
                // jira: ZSTAC-15944
                changeConnectionState(HostStatusEvent.connecting);
                ReconnectHostMsg rmsg = new ReconnectHostMsg();
                rmsg.setHostUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, HostConstant.SERVICE_ID, self.getUuid());
                bus.send(rmsg);
            }

            completion.success();
        }
    }
}
