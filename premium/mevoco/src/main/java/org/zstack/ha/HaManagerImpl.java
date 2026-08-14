package org.zstack.ha;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.compute.vm.BeforeStopVmOnHypervisorExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.ha.fencers.KvmFencerManager;
import org.zstack.header.core.*;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterUpdateOSExtensionPoint;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagOperationJudger;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmCanonicalEvents.VmStateChangedData;
import org.zstack.kvm.*;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.network.service.virtualrouter.AfterAcquireVirtualRouterExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.*;
import static org.zstack.ha.HaGlobalConfig.*;
import static org.zstack.ha.HaSystemTags.*;
import static org.zstack.ha.VmHaLevel.*;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.toMap;

/**
 * Created by xing5 on 2016/3/28.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HaManagerImpl extends AbstractService implements HaManager, Component, HaCompletionCacher,
        HostAfterConnectedExtensionPoint, OrderVmBeforeMigrationDuringHostMaintenanceExtensionPoint,
        PrimaryStorageAttachExtensionPoint, PrimaryStorageDetachExtensionPoint, PrimaryStorageDeleteExtensionPoint,
        ManagementNodeReadyExtensionPoint, VmHaExtensionPoint, HostScanExtensionPoint,
        UpdatePrimaryStorageExtensionPoint, VmInstanceMigrateExtensionPoint, ClusterUpdateOSExtensionPoint,
        AfterAcquireVirtualRouterExtensionPoint, PrepareDbInitialValueExtensionPoint,
        VmInstanceStartNewCreatedVmExtensionPoint, VmInstanceStartExtensionPoint, VmInstanceStopExtensionPoint,
        KVMDestroyVmExtensionPoint, BeforeStartNewCreatedVmExtensionPoint, BeforeStopVmOnHypervisorExtensionPoint,
        KvmReportVmShutdownEventExtensionPoint, VmInstanceCreateExtensionPoint, ApplianceVmInstanceCreateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HaManagerImpl.class);

    // A list of checker jobs of the same key (the key is usually 'hostUuid')
    final private Map<String, Collection<HaCompletion>> haCompletions = new HashMap<>();

    // A dictionary (vmUuid -> targetHostUuid) of migration failed HA VMs
    final private Map<String, String> migrationFailedVMs = new ConcurrentHashMap<>();

    @Autowired
    private EventFacade evtf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmHAExecutor vmHAExecutor;
    @Autowired
    private KvmFencerManager fencerManager;
    @Autowired
    private TimeHelper timeHelper;

    public static int vmHaBatchLimit = 1000;
    public Future<Void> neverStopVmScanTracker = null;
    private final ArrayList<VmInstanceState> needHaVmStates = new ArrayList<>();
    private final static Map<String, Boolean> haStatus = new HashMap<>();
    private List<String> updateHostStatusPrimaryStorages;

    public static final String ADD_VM_FENCER_RULE_TO_HOST = "/add/vm/fencer/rule/to/host";
    public static final String REMOVE_VM_FENCER_RULE_FROM_HOST = "/remove/vm/fencer/rule/from/host";
    private Cache<String, String> apiStopVmCache = CacheBuilder.newBuilder().
            maximumSize(1000).expireAfterAccess(10, TimeUnit.MINUTES).build();

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        syncHaVmListOnHost(inv.getHostUuid());

        boolean haEnabled = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, inv.getUuid())
                .eq(VmHaVO_.haLevel, VmHaLevel.NeverStop)
                .isExists();
        if (!haEnabled) {
            return;
        }

        addVmFencerRuleToHost(inv, inv.getHostUuid());
        vmHAExecutor.setHALevelForVM(inv.getUuid())
                .clearInhibitHABlocking("After starting VM")
                .update();
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        syncHaVmListOnHost(inv.getHostUuid());
        updateHaSystemTagByClusterConfig(inv);
        addVmFencerRuleToHost(inv, inv.getHostUuid());
    }

    private void updateHaSystemTagByClusterConfig(VmInstanceInventory inventory) {
        boolean haUndefined = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, inventory.getUuid())
                .eq(VmHaVO_.haLevel, Undefined)
                .isExists();
        if (!haUndefined) {
            return;
        }

        String clusterHAConfig = rcf.getResourceConfigValue(VM_HA_LEVEL, inventory.getClusterUuid(), String.class);
        boolean haEnabled = !None.toString().equals(clusterHAConfig);

        if (haEnabled) {
            vmHAExecutor.setHALevelForVM(inventory.getUuid())
                    .toNeverStop()
                    .update();
        } else {
            vmHAExecutor.setHALevelForVM(inventory.getUuid())
                    .toDisabled()
                    .update();
        }
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {
        removeVmFencerRuleFromHost(inv, inv.getHostUuid());
    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        syncHaVmListOnHost(inv.getLastHostUuid());
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {
        addVmFencerRuleToHost(inv, inv.getHostUuid());

        boolean vmRunning = VmInstanceState.Running.toString().equals(inv.getState());
        if (!vmRunning) {
            return;
        }

        boolean errorMatched = !HostErrors.OPERATION_FAILURE_GC_ELIGIBLE.isEqual(reason.getCode());
        if (errorMatched) {
            vmHAExecutor.setHALevelForVM(inv.getUuid())
                    .clearInhibitHABlocking("Failed to stop VM")
                    .update();
        }
    }

    @Override
    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.DestroyVmCmd cmd) throws KVMException {
        removeVmFencerRuleFromHost(vm, vm.getHostUuid());
        vmHAExecutor.setHALevelForVM(vm.getUuid())
                .inhibitHATemporarily("Before destroying VM")
                .update();
    }

    @Override
    public void beforeDirectlyDestroyVmOnKvm(KVMAgentCommands.DestroyVmCmd cmd) {

    }

    @Override
    public void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {

    }

    @Override
    public void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {
        addVmFencerRuleToHost(vm, host.getUuid());
    }

    private void addVmFencerRuleToHost(VmInstanceInventory inv, String hostUuid) {
        vmFencerRuleList.forEach(ext ->
                ext.addVmFencerRuleToHost(Stream.of(inv.getUuid()).collect(Collectors.toList()),
                        hostUuid));
    }

    private void addVmFencerRuleToHost(List<String> vmUuids, String hostUuid) {
        vmFencerRuleList.forEach(ext ->
                ext.addVmFencerRuleToHost(vmUuids, hostUuid));
    }

    private void removeVmFencerRuleFromHost(VmInstanceInventory inv, String hostUuid) {
        vmFencerRuleList.forEach(ext -> ext.removeVmFencerRuleFromHost(inv, hostUuid));
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceSpec spec) {
        final String vmUuid = spec.getVmInventory().getUuid();
        final boolean createdStopped = spec.getStrategy() == VmCreationStrategy.CreateStopped;

        if (createdStopped) {
            vmHAExecutor.setHALevelForVM(vmUuid)
                    .inhibitHATemporarily("Create VM with CreateStopped strategy")
                    .update();
        }
    }

    @Override
    public void beforeStopVmOnHypervisor(VmInstanceSpec spec, StopVmOnHypervisorMsg msg) {
        final Message parentMsg = spec.getMessage();
        if (parentMsg instanceof APIStopVmInstanceMsg) {
            apiStopVmCache.put(spec.getVmInventory().getUuid(), spec.getVmInventory().getUuid());
        }

        boolean stopHa = false;
        if (parentMsg instanceof StopVmInstanceMsg) {
            stopHa = ((StopVmInstanceMsg) parentMsg).isStopHA();
        } else if (parentMsg instanceof APIStopVmInstanceMsg) {
            stopHa = Objects.equals(((APIStopVmInstanceMsg) parentMsg).getStopHA(), Boolean.TRUE.toString());
        }

        if (stopHa) {
            vmHAExecutor.setHALevelForVM(msg.getVmInventory().getUuid())
                    .inhibitHATemporarily("Before VM stopping on hypervisor")
                    .update();
        }
    }

    @Override
    public void kvmReportVmShutdownEvent(ShutdownDetail detail) {
        if (!detail.triggerByGuest) {
            return;
        }

        if (!StringUtils.isEmpty(apiStopVmCache.getIfPresent(detail.vmUuid))) {
            apiStopVmCache.invalidate(detail.vmUuid);
            return;
        }

        vmHAExecutor.setHALevelForVM(detail.vmUuid)
                .inhibitHATemporarily("KVM reporting VM shutdown")
                .update();
    }

    public static class AddVmFencerRuleToHostCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        private List<VmRuleAttachFencer> allowRules;
        private List<VmRuleAttachFencer> blockRules;
    }

    public static class AddVmFencerRuleToHostRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class RemoveVmFencerRuleFromHostCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        private List<VmRuleAttachFencer> allowRules;
        private List<VmRuleAttachFencer> blockRules;
    }

    public static class RemoveVmFencerRuleFromHostRsp extends KVMAgentCommands.AgentResponse {

    }


    public List<String> getUpdateHostStatusPrimaryStorages() {
        return updateHostStatusPrimaryStorages;
    }

    public void setUpdateHostStatusPrimaryStorages(List<String> updateHostStatusPrimaryStorages) {
        this.updateHostStatusPrimaryStorages = updateHostStatusPrimaryStorages;
    }

    {
        needHaVmStates.add(VmInstanceState.Unknown);
        needHaVmStates.add(VmInstanceState.Stopped);
    }

    private final Map<String, HaHypervisorFactory> hypervisorFactories = new ConcurrentHashMap<>();
    private List<VmFencerRuleExtensionPoint> vmFencerRuleList = new ArrayList<>();

    @Override
    public boolean start() {
        populateExtensions();
        setupCanonicalEvents();
        setupGlobalConfig();

        restf.registerSyncHttpCallHandler(SelfFencerKvmBackend.KVM_REPORT_VM_FENCED_EVENT, SelfFencerKvmBackend.ReportVmSelfFencerCmd.class, cmd -> {
            Map<String, SelfFencerKvmBackend.ReportVmSelfFencerTuple> uuidFencerMap =
                    toMap(cmd.values, it -> it.vmUuid, Function.identity());
            if (uuidFencerMap.isEmpty()) {
                return null;
            }

            List<String> vmUuids = Q.New(VmInstanceVO.class)
                    .in(VmInstanceVO_.uuid, uuidFencerMap.keySet())
                    .select(VmInstanceVO_.uuid)
                    .listValues();
            if (vmUuids.isEmpty()) {
                return null;
            }

            logger.info(String.format("host[uuid=%s] report VM%s has been fenced", cmd.hostUuid, vmUuids));
            for (String uuid : vmUuids) {
                fencerManager.createFencedByTag(uuidFencerMap.get(uuid));
            }

            // TODO: start HA (send message or start HA GC job)
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REQUEST_MAINTAIN_HOST, KVMAgentCommands.ReportHostMaintainCmd.class, cmd -> {
            ChangeHostStateMsg cmsg = new ChangeHostStateMsg();
            cmsg.setStateEvent(HostStateEvent.preMaintain.toString());
            cmsg.setUuid(cmd.hostUuid);
            cmsg.setForceChange(true);
            cmsg.setJustChangeState(false);
            bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, cmsg.getUuid());
            bus.send(cmsg);

            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_SELF_FENCER, KVMAgentCommands.ReportSelfFencerCmd.class, new SyncHttpCallHandler<KVMAgentCommands.ReportSelfFencerCmd>() {
            @Override
            public String handleSyncHttpCall(KVMAgentCommands.ReportSelfFencerCmd cmd) {
                List<String> psUuids = Q.New(PrimaryStorageVO.class)
                        .select(PrimaryStorageVO_.uuid)
                        .in(PrimaryStorageVO_.type, updateHostStatusPrimaryStorages)
                        .listValues();
                psUuids.retainAll(cmd.psUuids);

                if(psUuids.isEmpty()) {
                    return null;
                }

                for (String psUuid : psUuids) {
                    VmHaCanonicalEvents.VMHaSelfFencerTriggeredData data = new VmHaCanonicalEvents.VMHaSelfFencerTriggeredData();
                    data.setPrimaryStorageUuid(psUuid);
                    data.setHostUuid(cmd.hostUuid);
                    data.setVmUuids(cmd.vmUuidsString);
                    data.setReason(cmd.reason);
                    evtf.fire(VmHaCanonicalEvents.VM_HA_HOST_SELF_FENCER_TRIGGERED_PATH, data);
                }

                return null;
            }
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_PS_STATUS, KVMAgentCommands.ReportPsStatusCmd.class, new SyncHttpCallHandler<KVMAgentCommands.ReportPsStatusCmd>() {
            @Override
            public String handleSyncHttpCall(KVMAgentCommands.ReportPsStatusCmd cmd) {
                List<String> psUuids = Q.New(PrimaryStorageVO.class)
                        .select(PrimaryStorageVO_.uuid)
                        .in(PrimaryStorageVO_.type, updateHostStatusPrimaryStorages)
                        .listValues();
                psUuids.retainAll(cmd.psUuids);

                if(!psUuids.isEmpty()){
                    List<UpdatePrimaryStorageHostStatusMsg> msgList = new ArrayList<>();
                    if (PrimaryStorageHostStatus.Disconnected.toString().equals(cmd.psStatus)) {
                        List<String> skipUpdatePrimaryStorageUuids = Q.New(PrimaryStorageHostRefVO.class)
                                .select(PrimaryStorageHostRefVO_.primaryStorageUuid)
                                .in(PrimaryStorageHostRefVO_.primaryStorageUuid, psUuids)
                                .eq(PrimaryStorageHostRefVO_.hostUuid, cmd.hostUuid)
                                .in(PrimaryStorageHostRefVO_.status, Arrays.asList(PrimaryStorageHostStatus.Connecting,
                                        PrimaryStorageHostStatus.valueOf(cmd.psStatus)))
                                .listValues();
                        psUuids.removeAll(skipUpdatePrimaryStorageUuids);
                        logger.debug(String.format("primary storages%s host status need to be updated on host %s, primary storages%s " +
                                        "is connecting or state remains unchanged, skip to update",
                                psUuids, cmd.hostUuid, skipUpdatePrimaryStorageUuids));
                    }

                    for (String psUuid : psUuids) {
                        UpdatePrimaryStorageHostStatusMsg msg = new UpdatePrimaryStorageHostStatusMsg();
                        msg.setHostUuid(cmd.hostUuid);
                        msg.setPrimaryStorageUuid(psUuid);
                        msg.setStatus(PrimaryStorageHostStatus.valueOf(cmd.psStatus));

                        if (msg.getStatus().equals(PrimaryStorageHostStatus.Disconnected)) {
                            ErrorCode reason = HaConstants.PRIMARY_STORAGE_HOST_DISCONNECTED_ERROR;
                            reason.setDetails(String.format("primary storage %s disconnect on host %s, details: %s",
                                    cmd.psUuids, cmd.hostUuid, cmd.reason));
                            msg.setReason(reason);
                        }

                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                        msgList.add(msg);
                    }

                    bus.send(msgList);
                }

                if (cmd.psStatus.equals(PrimaryStorageHostStatus.Disconnected.toString())){
                    logger.warn(String.format("From agent[hostUuid:%s] report: it cannot connect primary storage[uuids:%s]" +
                                    " all VMs whose root volume in these primary storages have been killed," +
                                    " HA VMs will be migrated to other available hosts.",
                            cmd.hostUuid, cmd.psUuids));
                } else if (cmd.psStatus.equals(PrimaryStorageHostStatus.Connected.toString())){
                    logger.debug(String.format("From agent[hostUuid:%s] report: it can connect primary storage[uuids:%s] again",
                            cmd.hostUuid, cmd.psUuids));
                }

                return null;
            }
        });

        HaSystemTags.HA.installJudger(new SystemTagOperationJudger() {
            @Override
            public void tagPreCreated(SystemTagInventory tag) {
                VmHaLevel level = validateHaTag(tag);
                SQL.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, tag.getResourceUuid())
                        .set(VmHaVO_.haLevel, level)
                        .set(VmHaVO_.haLevelUpdateTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                        .update();
            }

            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                SQL.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, tag.getResourceUuid())
                        .set(VmHaVO_.haLevel, None)
                        .set(VmHaVO_.haLevelUpdateTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                        .update();
            }

            @Override
            public void tagPreUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                VmHaLevel level = validateHaTag(newTag);
                SQL.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, newTag.getResourceUuid())
                        .set(VmHaVO_.haLevel, level)
                        .set(VmHaVO_.haLevelUpdateTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                        .update();
            }

            private VmHaLevel validateHaTag(SystemTagInventory tag) {
                final String haToken = HA.getTokenByTag(tag.getTag(), HA_TOKEN);
                final VmHaLevel level = VmHaLevel.valueOfOrNull(haToken);
                if (level == null) {
                    throw new OperationFailureException(operr("invalid HA level: %s", haToken));
                }
                return level;
            }
        });

        return true;
    }

    private void setupGlobalConfig() {
        HaGlobalConfig.CHECK_SUCCESS_RATIO.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                float v = Float.parseFloat(newValue);
                if (v < 0 || v > 1) {
                    throw new OperationFailureException(argerr("the value[%s] is lesser than 0 or greater than 1 ", newValue));
                }
            }
        });

        HaGlobalConfig.NEVER_STOP_VM_SCAN_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                setHaScheduler();
            }
        });

        HaGlobalConfig.ALL.installValidateExtension((category, name, oldValue, newValue) -> {
            List<String> hostUuids = getDisconnectedHostsWithVm();
            if (Boolean.parseBoolean(newValue) && !hostUuids.isEmpty()) {
                throw new GlobalConfigException(String.format("host[uuid:%s] is not %s, but still have vm on it," +
                        " please resolve hosts' problems before enable ha", hostUuids, HostStatus.Connected));
            }
        });

        HaGlobalConfig.SELF_FENCER_STRATEGY.installValidateExtension((category, name, oldValue, newValue) -> {
            List<String> hostUuids = getDisconnectedHostsWithVm();
            if (!hostUuids.isEmpty()) {
                throw new GlobalConfigException(String.format("host[uuid:%s] is not %s, but still have vm on it," +
                        " please resolve hosts' problems before update fencer strategy", hostUuids, HostStatus.Connected));
            }
        });

        HaGlobalConfig.ALL.installLocalUpdateExtension((oldConfig, newConfig) -> {
            if (newConfig.value(Boolean.class)) {
                setupSelfFencerOnAllHosts();
            } else {
                UpdateQuery.New(HaStrategyConditionVO.class)
                        .set(HaStrategyConditionVO_.state, HaStrategyState.Disable)
                        .update();
                cancelSelfFencerOnAllHosts();
            }
        });

        HaGlobalConfig.SELF_FENCER_STRATEGY.installLocalUpdateExtension((oldConfig, newConfig) -> {
            if (SelfFencerStrategy.Force.toString().equals(newConfig.value())) {
                updateHostStorageType(HaStrategyState.Enable);
            } else {
                updateHostStorageType(HaStrategyState.Disable);
            }

            setupSelfFencerOnAllHosts();
        });

        HaGlobalConfig.VM_HA_STRATEGY.installLocalUpdateExtension((oldConfig, newConfig) -> {
            if (SelfFencerStrategy.Force.toString().equals(newConfig.value()) && !HaGlobalConfig.ALL.value().equals("true")) {
                HaGlobalConfig.ALL.updateValue("true");
            }
        });
    }

    private void updateHostStorageType(HaStrategyState state) {
        UpdateQuery.New(HaStrategyConditionVO.class)
                .set(HaStrategyConditionVO_.state, state)
                .eq(HaStrategyConditionVO_.fencerName, HaConstants.HOST_STORAGE_STATE)
                .update();
    }

    private List<String> getDisconnectedHostsWithVm() {
        return SQL.New("select distinct host.uuid from VmInstanceVO vm, HostVO host where vm.hostUuid = host.uuid " +
                "and host.status != :hstatus and host.hypervisorType in (:htypes) and vm.state = :vstate", String.class)
                .param("hstatus", HostStatus.Connected)
                .param("vstate", VmInstanceState.Unknown)
                .param("htypes", hypervisorFactories.keySet())
                .limit(10)
                .list();
    }

    private ChangeVmStateMsg prepareMsg( String vmUuid) {
        ChangeVmStateMsg msg = new ChangeVmStateMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setStateEvent(VmInstanceStateEvent.stopped.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
        return msg;
    }

    @Override
    public void onScanResult(String hostUuid, HostCheckResult.HostScanResult stage) {
        if (stage.isAlive()) {
            return;
        }

        List<String> unknownVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, hostUuid)
                .in(VmInstanceVO_.type, Arrays.asList(VmInstanceConstant.USER_VM_TYPE, ApplianceVmConstant.APPLIANCE_VM_TYPE))
                .eq(VmInstanceVO_.state, VmInstanceState.Unknown)
                .listValues();

        logger.info(String.format("Host[uuid: %s] is dead, has %d unknown VMs", hostUuid, unknownVmUuids.size()));

        List<ChangeVmStateMsg> cmsgs = unknownVmUuids.stream()
                .filter(uuid -> {
                    VmHaLevel level = Q.New(VmHaVO.class)
                            .eq(VmHaVO_.uuid, uuid)
                            .select(VmHaVO_.haLevel)
                            .findValue();
                    return level.isDisabled();
                })
                .filter(uuid -> HaStrategyHelper.getVmHaFencerStrategy(uuid) == SelfFencerStrategy.Force)
                .map(this::prepareMsg)
                .collect(Collectors.toList());

        logger.debug(String.format("remaining vm %s", cmsgs.stream().map(ChangeVmStateMsg::getVmInstanceUuid).collect(Collectors.toList())));

        bus.send(cmsgs);
    }

    private void stopSelfFencerForPrimaryStorage(PrimaryStorageInventory inv) {
        if (!isHaEnabled()) {
            return;
        }

        List<String> clusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.clusterUuid).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, inv.getUuid())
                .listValues();

        if (clusterUuids.isEmpty()) {
            return;
        }

        SelfFencerParamGenerator generator = new SelfFencerParamGenerator();
        generator.clusterUuids = clusterUuids;
        generator.operator = param -> {
            CancelStorageHeartbeatFileAndGatewayPing cancel = new CancelStorageHeartbeatFileAndGatewayPing(param);
            cancel.cancel();
        };
        generator.run();
    }

    @Override
    public void preDeletePrimaryStorage(PrimaryStorageInventory inv) {

    }

    @Override
    public void beforeDeletePrimaryStorage(PrimaryStorageInventory inv) {

    }

    @Override
    public void afterDeletePrimaryStorage(PrimaryStorageInventory inv) {
        stopSelfFencerForPrimaryStorage(inv);
    }

    @Override
    public void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        List<HostVO> hosts = Q.New(HostVO.class).eq(HostVO_.clusterUuid, clusterUuid).list();
        for (HostVO h : hosts) {
            SelfFencerStruct param = new SelfFencerStruct();
            param.setHost(HostInventory.valueOf(h));
            param.setPrimaryStorage(list(inventory));
            CancelStorageHeartbeatFileAndGatewayPing cancel = new CancelStorageHeartbeatFileAndGatewayPing(param);
            cancel.cancel();
        }
    }

    public Collection<HaCompletion> dequeCompletions(String key) {
        synchronized (haCompletions) {
            return haCompletions.remove(key);
        }
    }

    public boolean enqueCompletion(final HaCompletion job, final String key) {
        boolean isFirst;

        synchronized (haCompletions) {
            Collection<HaCompletion> hcs = haCompletions.getOrDefault(key, new ArrayList<>());
            isFirst = hcs.isEmpty();
            hcs.add(job);
            if (isFirst) {
                haCompletions.put(key, hcs);
            }

            return isFirst;
        }
    }

    private void setGCJobForNeverStopHaVm() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!isHaEnabled()) {
                    return;
                }

                int offset = 0;
                while (true) {
                    List<VmInstanceVO> vos = sql("select vmVO" +
                            " from VmInstanceVO vmVO" +
                            " where vmVO.state in (:needHaVmStates)", VmInstanceVO.class)
                            .param("needHaVmStates", needHaVmStates)
                            .offset(offset)
                            .limit(vmHaBatchLimit)
                            .list();
                    if (vos.isEmpty()) {
                        break;
                    }
                    offset += vos.size();

                    logger.debug(String.format("Batch:set HA GC job for %d VMs.", vos.size()));

                    List<VmInstanceInventory> invs = VmInstanceInventory.valueOf(vos);
                    for (VmInstanceInventory inv : invs) {
                        if (!destinationMaker.isManagedByUs(inv.getUuid())) {
                            continue;
                        }

                        final VmHaVO ha = Q.New(VmHaVO.class)
                                .eq(VmHaVO_.uuid, inv.getUuid())
                                .find();

                        if (ha.getInhibitionReason() != null) {
                            logger.debug(String.format("Skip VM [%s] GC job for HA", inv.getUuid()));
                            continue;
                        }

                        if (ha.getHaLevel() == NeverStop || ha.getHaLevel() == OnHostFailure) {
                            // if vm's host connected means host is recovered from failure
                            // use startVmWithStateSyncBefore which will check vm state before
                            boolean hostRecovered = false;
                            boolean isUnknownVm = VmInstanceState.Unknown.toString().equals(inv.getState());
                            boolean isStoppedVm = VmInstanceState.Stopped.toString().equals(inv.getState());
                            if (isUnknownVm) {
                                hostRecovered = Q.New(HostVO.class)
                                        .eq(HostVO_.status, HostStatus.Connected)
                                        .eq(HostVO_.uuid, inv.getHostUuid())
                                        .isExists();
                            } else if (isStoppedVm) {
                                hostRecovered = Q.New(HostVO.class)
                                        .eq(HostVO_.status, HostStatus.Connected)
                                        .eq(HostVO_.uuid, inv.getLastHostUuid())
                                        .isExists();
                            }

                            if (hostRecovered) {
                                startVmWithStateSyncBefore(inv);
                            } else if (isStoppedVm) {
                                submitNeverStopVmGCJob(inv);
                            } else if (isUnknownVm) {
                                haOnHostFailure(inv, true);
                            }
                        } else if (ha.getHaLevel() == FaultTolerance) {
                            if (!inv.getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
                                continue;
                            }

                            if (VmInstanceState.Unknown.toString().equals(inv.getState())) {
                                haOnHostFailure(inv, false);
                            }
                        } else {
                            if (VmInstanceState.Unknown.toString().equals(inv.getState())) {
                                haOnHostFailure(inv, false);
                            }
                        }
                    }
                }
            }
        }.execute();
    }

    private void submitNeverStopVmGCJob(VmInstanceInventory inv){
        for (VmHaExtensionPoint ext : pluginRgty.getExtensionList(VmHaExtensionPoint.class)) {
            ext.preHaStartVm(inv.getUuid());
        }

        for (HaHostDeviceExtensionPoint ext : pluginRgty.getExtensionList(HaHostDeviceExtensionPoint.class)) {
            if (!ext.canDoVmHa(inv.getUuid())) {
                logger.warn(String.format("can not ha because device still attached to vm[%s]", inv.getUuid()));
                return;
            }
        }

        setGCJobForOneVm(inv);
    }

    private void setGCJobForOneVm(VmInstanceInventory vmInv) {
        String gcName = NeverStopVmGC.getGCName(vmInv.getUuid());
        if (Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.name, gcName)
                .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                .isExists()) {
            logger.debug(String.format("There is already a NeverStopVmGC on Vm[uuid:%s], skip.", vmInv.getUuid()));
            return;
        }

        NeverStopVmGC gc = new NeverStopVmGC();
        gc.NAME = gcName;
        gc.vm = vmInv;
        gc.submit(HaGlobalConfig.NEVER_STOP_VM_FAILURE_RETRY_DELAY.value(Long.class), TimeUnit.SECONDS);
        logger.debug(i18n("A GC job is submitted to HA the VM[retry delay: %s seconds]", HaGlobalConfig.NEVER_STOP_VM_FAILURE_RETRY_DELAY.value(Long.class)));
    }

    private synchronized void setHaScheduler() {
        if (neverStopVmScanTracker != null) {
            neverStopVmScanTracker.cancel(true);
        }

        final long interval = HaGlobalConfig.NEVER_STOP_VM_SCAN_INTERVAL.value(Long.class);
        neverStopVmScanTracker = thdf.submitPeriodicTask(new PeriodicTask() {

            @Override
            @ExceptionSafe
            public void run() {
                setGCJobForNeverStopHaVm();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "NeverStopVmScanTracker";
            }

        });
    }

    @Override
    public void managementNodeReady() {
        setHaScheduler();
    }

    @Override
    public String preUpdateClusterOS(ClusterVO cls) {
        return null;
    }

    @Override
    public void beforeUpdateClusterOS(ClusterVO cls) {
        // if global ha is enabled, then record and disable it
        if (isHaEnabled()) {
            haStatus.put(cls.getUuid(), true);
            HaGlobalConfig.ALL.updateValue(false);
        } else {
            haStatus.put(cls.getUuid(), false);
        }
    }

    @Override
    public void afterUpdateClusterOS(ClusterVO cls) {
        // if global ha was enabled, then re-enable it
        if (haStatus.containsKey(cls.getUuid())) {
            Boolean isHaEnabled = haStatus.get(cls.getUuid());
            if (isHaEnabled) {
                HaGlobalConfig.ALL.updateValue(true);
            }
        }
    }

    @Override
    public void afterAcquireVirtualRouter(VirtualRouterVmInventory vr, Completion completion) {
        VirtualRouterVmVO vo = dbf.findByUuid(vr.getUuid(), VirtualRouterVmVO.class);

        if (vo.getState().equals(VmInstanceState.Running)) {
            completion.success();
            return;
        }

        if (ApplianceVmStatus.Connected.equals(vo.getStatus())) {
            logger.debug(String.format("virtual router[uuid: %s] is in status[%s], skip HA", vr.getUuid(), vo.getStatus()));
            completion.success();
            return;
        }

        if (HaStrategyHelper.getVmHaFencerStrategy(vr.getUuid()) == SelfFencerStrategy.Permissive && vo.getState().equals(VmInstanceState.Unknown)) {
            logger.debug(String.format("Self fencer strategy if Permissive, skip HA for virtual router[uuid:%s, state:%s]", vr.getUuid(), VmInstanceState.Unknown.toString()));
            completion.success();
            return;
        }

        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(vr.getUuid())
                .withJudgerClass(HaStartVirtualRouterVmJudger.class.getName())
                .withReasonForHA(i18n("VirtualRouterVm is not running"));
        sender.send(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch(){
            @Override
            protected void scripts() {
                if (!q(HaStrategyConditionVO.class)
                        .eq(HaStrategyConditionVO_.fencerName, HaConstants.HOST_STORAGE_STATE)
                        .isExists()) {
                    HaStrategyConditionVO vo = new HaStrategyConditionVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setName(HaStrategyConditionVO.class.getSimpleName());
                    vo.setFencerName(HaConstants.HOST_STORAGE_STATE);
                    vo.setState(HaStrategyState.Enable);
                    persist(vo);

                    flush();
                    logger.debug("Created initial hostStorageState");
                }

                if(!q(HaStrategyConditionVO.class)
                        .eq(HaStrategyConditionVO_.fencerName, HaConstants.HOST_BUSINESS_NIC)
                        .isExists()) {
                    HaStrategyConditionVO hostBusinessNic = new HaStrategyConditionVO();
                    hostBusinessNic.setUuid(Platform.getUuid());
                    hostBusinessNic.setName(HaStrategyConditionVO.class.getSimpleName());
                    hostBusinessNic.setFencerName(HaConstants.HOST_BUSINESS_NIC);
                    hostBusinessNic.setState(HaStrategyState.Disable);
                    persist(hostBusinessNic);
                    flush();
                    logger.debug("Created initial hostBusinessNic");
                }
            }
        }.execute();
    }

    private interface SelfFencerOperator {
        void operate(SelfFencerStruct param);
    }

    private class SelfFencerParamGenerator {
        SelfFencerOperator operator;
        List<String> clusterUuids;

        void run() {
            DebugUtils.Assert(operator != null, "SelfFencerOperator must be set");

            if (clusterUuids == null) {
                SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
                cq.select(ClusterVO_.uuid);
                clusterUuids = cq.listValue();
            }

            if (clusterUuids.isEmpty()) {
                return;
            }

            for (String clusterUuid : clusterUuids) {
                List<PrimaryStorageInventory> ps = new Callable<List<PrimaryStorageInventory>>() {
                    @Override
                    @Transactional(readOnly = true)
                    public List<PrimaryStorageInventory> call() {
                        String sql = "select ps from PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps where" +
                                " ps.uuid = ref.primaryStorageUuid and ref.clusterUuid = :cuuid";
                        TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
                        q.setParameter("cuuid", clusterUuid);
                        List<PrimaryStorageVO> vos = q.getResultList();
                        return PrimaryStorageInventory.valueOf(vos);
                    }
                }.call();

                if (ps.isEmpty()) {
                    continue;
                }

                Iterator<List<HostInventory>> hostIterator = new Iterator<List<HostInventory>>() {
                    final int STEP = 1000;

                    int offset = 0;

                    @Override
                    public boolean hasNext() {
                        // not used
                        return true;
                    }

                    @Override
                    public List<HostInventory> next() {
                        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
                        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
                        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
                        q.setStart(offset);
                        q.setLimit(STEP);
                        List<HostVO> vos = q.list();

                        offset += STEP;

                        return vos.isEmpty() ? null : HostInventory.valueOf(vos);
                    }
                };

                List<HostInventory> hosts;
                do {
                    hosts = hostIterator.next();
                    if (hosts != null) {
                        for (HostInventory host : hosts) {
                            new Runnable() {
                                @Override
                                @ExceptionSafe
                                public void run() {
                                    SelfFencerStruct param = new SelfFencerStruct();
                                    param.setHost(host);
                                    param.setPrimaryStorage(ps);
                                    param.setStrategy(HaGlobalConfig.SELF_FENCER_STRATEGY.value());
                                    param.setFencers(HaHelper.findExecuteFencers());
                                    operator.operate(param);
                                }
                            }.run();
                        }
                    }

                } while (hosts != null);
            }
        }
    }

    private void cancelSelfFencerOnAllHosts() {
        SelfFencerParamGenerator generator = new SelfFencerParamGenerator();
        generator.operator = param -> {
            CancelStorageHeartbeatFileAndGatewayPing cancel = new CancelStorageHeartbeatFileAndGatewayPing(param);
            cancel.cancel();
        };
        generator.run();
    }

    private void setupSelfFencerOnAllHosts() {
        SelfFencerParamGenerator generator = new SelfFencerParamGenerator();
        generator.operator = param -> {
            SetupStorageHeartbeatFileFencer setup = new SetupStorageHeartbeatFileFencer(param);
            setup.setup();
        };
        generator.run();
    }

    private void populateExtensions() {
        for (HaHypervisorFactory f : pluginRgty.getExtensionList(HaHypervisorFactory.class)) {
            HaHypervisorFactory old = hypervisorFactories.get(f.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HaHypervisorFactory for type[%s]", f.getHypervisorType()));
            }

            hypervisorFactories.put(f.getHypervisorType(), f);
        }

        pluginRgty.saveExtensionAsMap(KvmSetupSelfFencerExtensionPoint.class, KvmSetupSelfFencerExtensionPoint::kvmSetupSelfFencerStorageType);

        pluginRgty.getExtensionList(KvmSetupSelfFencerExtensionPoint.class).forEach(ext -> new HaStrategyHelper(ext.kvmSetupSelfFencerStorageType(), ext.storageConsistencySupported()));
        vmFencerRuleList = pluginRgty.getExtensionList(VmFencerRuleExtensionPoint.class);
    }

    private void setupCanonicalEvents() {
        evtf.on(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                VmStateChangedData d = (VmStateChangedData) data;
                if (!destinationMaker.isManagedByUs(d.getVmUuid())) {

                    if (destinationMaker.getManagementNodeCount() > 1) {
                        // we wait two ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL in case the VM is
                        // managed by a node which is dying
                        try {
                            TimeUnit.SECONDS.sleep(2L * ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class));
                        } catch (InterruptedException e) {
                            logger.warn(e.getMessage(), e);
                            Thread.currentThread().interrupt();
                        }
                    }

                    if (!destinationMaker.isManagedByUs(d.getVmUuid())) {
                        // OK, it's really not managed by us
                        return;
                    }
                }

                startHaIfNeeded(d);
            }
        });
    }

    private void startHaIfNeeded(VmStateChangedData d) {
        if (!isHaEnabled()) {
            return;
        }

        VmHaVO ha = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, d.getVmUuid())
                .find();
        if (ha.getHaLevel().isDisabled() || null != ha.getInhibitionReason()) {
            return;
        }

        VmHaLevel haLevel = ha.getHaLevel();
        if (haLevel.equals(VmHaLevel.FaultTolerance)) {
            if (VmInstanceState.Unknown.toString().equals(d.getNewState())) {
                haOnHostFailure(d.getInventory(), false);
            }

            return;
        }

        logger.info(String.format("starting HA for VM[uuid:%s]", d.getVmUuid()));
        if (haLevel == VmHaLevel.OnHostFailure && VmInstanceState.Unknown.toString().equals(d.getNewState())) {
            haOnHostFailure(d.getInventory(), false);
        } else if (haLevel == VmHaLevel.NeverStop) {
            if (VmInstanceState.Stopped.toString().equals(d.getNewState())) {
                haOnStopVm(d.getInventory());
            } else if (VmInstanceState.Unknown.toString().equals(d.getNewState())) {
                haOnHostFailure(d.getInventory(), true);
            }
        } else {
            if (VmInstanceState.Unknown.toString().equals(d.getNewState())) {
                haOnHostFailure(d.getInventory(), false);
            }
        }
    }

    private void doStartVm(final VmInstanceInventory vmInv, String reason) {
        doStartVm(vmInv, reason, null);
    }

    private void doStartVm(final VmInstanceInventory vmInv, String reason, VmHaTaskTracker tracker) {
        for (HaHostDeviceExtensionPoint ext : pluginRgty.getExtensionList(HaHostDeviceExtensionPoint.class)) {
            if (!ext.canDoVmHa(vmInv.getUuid())) {
                logger.warn(String.format("can not ha because device still attached to vm[%s]", vmInv.getUuid()));
                return;
            }
        }

        String hostUuid = vmInv.getHostUuid() == null ? vmInv.getLastHostUuid() : vmInv.getHostUuid();
        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(vmInv.getUuid())
                .withAvoidHost(hostUuid)
                .withReasonForHA(reason)
                .withJudgerClass(GeneralVmStateHaJudger.class.getName());

        sender.send(new Completion(null) {
            @Override
            public void success() {
                trackSuccess(tracker);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trackFailure(tracker, errorCode);
                if (!errorCode.isError(SysErrors.INTERNAL)) {
                    submitNeverStopVmGCJob(vmInv);
                }
            }

            private void trackSuccess(VmHaTaskTracker tracker) {
                if (tracker != null) {
                    tracker.track(VmHaTaskTracker.Process.success, i18n("HA is successfully completed"));
                }
            }

            private void trackFailure(VmHaTaskTracker tracker, ErrorCode errorCode) {
                if (tracker != null) {
                    tracker.error(errorCode);
                    tracker.track(VmHaTaskTracker.Process.failure, i18n("Failed to HA the VM"));
                }
            }
        });
    }


    private void handleMigrationFailure(VmInstanceInventory vmInv, String hostUuid) {
        final String vmUuid = vmInv.getUuid();
        CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
        msg.setVmInstanceUuids(Collections.singletonList(vmUuid));
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("cannot determine the VM[%s] status on host[%s]",
                            vmUuid, hostUuid));

                    // Set the host to migration destination, and HA manager will try to bring it up.
                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Unknown)
                            .update();
                    return;
                }

                CheckVmStateOnHypervisorReply r = reply.castReply();
                String state = r.getStates().get(vmUuid);
                if (state == null) {
                    logger.warn(String.format("VM[%s] not found on migration destination host[%s]",
                            vmUuid, hostUuid));

                    doStartVm(vmInv, "vm is not found on migration destination host after migrate failing");
                    return;
                }

                if (VmInstanceState.Running.toString().equals(state)) {
                    logger.warn(String.format("Migrated VM[%s] is running on host[%s]", vmUuid, hostUuid));

                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Running)
                            .update();
                    return;
                }

                if (VmInstanceState.Paused.toString().equals(state)) {
                    logger.warn(String.format("Migrated VM[%s] is paused on host[%s]", vmUuid, hostUuid));

                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Paused)
                            .update();
                    return;
                }

                if (VmInstanceState.Stopped.toString().equals(state)) {
                    logger.warn(String.format("Migrated VM[%s] is stopped on host[%s]", vmUuid, hostUuid));

                    // A stopped VM will be brought up by HA manager.
                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                            .update();
                }
            }
        });
    }

    // A NeverStop VM is reported as 'Stopped' or
    // host is Connected but NEVER_STOP_VM_SCAN found
    // vm unexpectedly changed to 'Stopped' or 'Unknown'
    private void startVmWithStateSyncBefore(final VmInstanceInventory vmInv) {
        final String vmUuid = vmInv.getUuid();
        final String hostUuid = vmInv.getHostUuid() != null ? vmInv.getHostUuid() : vmInv.getLastHostUuid();

        VmHaTaskTracker tracker = new VmHaTaskTracker(vmUuid);
        tracker.track(VmHaTaskTracker.Process.checking, i18n("vm stopped unexpectedly, double check state"));
        CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
        msg.setVmInstanceUuids(Collections.singletonList(vmUuid));
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    String info = i18n("cannot determine VM[%s] status on host[%s], try to start it", vmUuid, hostUuid);
                    logger.warn(String.format("start HA tracker for VM[%s]: %s", vmInv.getUuid(), info));

                    tracker.track(VmHaTaskTracker.Process.starting, info);
                    doStartVm(vmInv, info, tracker);
                    return;
                }

                CheckVmStateOnHypervisorReply r = reply.castReply();
                String state = r.getStates().get(vmUuid);
                if (state == null || VmInstanceState.Stopped.toString().equals(state)) {
                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                            .set(VmInstanceVO_.hostUuid, null)
                            .set(VmInstanceVO_.lastHostUuid, hostUuid)
                            .update();
                    String info = fencerManager.findVmFencedReasonByTag(vmUuid);
                    info = info == null ? i18n("vm state is stopped, try to start it") : info;
                    tracker.track(VmHaTaskTracker.Process.starting, info);
                    logger.debug(String.format("start HA tracker for stopped VM[%s]: %s", vmInv.getUuid(), info));
                    doStartVm(vmInv, info, tracker);
                    return;
                }

                if (VmInstanceState.Running.toString().equals(state)) {
                    String info = i18n("VM[%s] is running on host[%s]", vmUuid, hostUuid);
                    logger.warn(info);
                    tracker.track(VmHaTaskTracker.Process.noNeed, info);

                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Running)
                            .update();
                    return;
                }

                if (VmInstanceState.Paused.toString().equals(state)) {
                    String info = i18n("VM[%s] is paused on host[%s]", vmUuid, hostUuid);
                    logger.warn(info);
                    tracker.track(VmHaTaskTracker.Process.noNeed, info);

                    UpdateQuery.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .set(VmInstanceVO_.hostUuid, hostUuid)
                            .set(VmInstanceVO_.state, VmInstanceState.Paused)
                            .update();
                }
            }
        });
    }

    private void haOnStopVm(final VmInstanceInventory vmInv) {
        if (migrationFailedVMs.containsKey(vmInv.getUuid())) {
            handleMigrationFailure(vmInv, migrationFailedVMs.remove(vmInv.getUuid()));
            return;
        }

        startVmWithStateSyncBefore(vmInv);
    }

    private void haOnHostFailure(VmInstanceInventory vm, final boolean retryOnFailure) {
        VmHaTaskTracker tracker = new VmHaTaskTracker(vm.getUuid());

        final HaHypervisorFactory f = hypervisorFactories.get(vm.getHypervisorType());
        if (f == null) {
            tracker.track(VmHaTaskTracker.Process.noNeed, i18n("the hypervisor[%s] does not support VM HA", vm.getHypervisorType()));
            return;
        }

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("ha-vm-%s", vm.getUuid());
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
            public void run(final SyncTaskChain chain) {
                HaHypervisorWorker worker = f.createHaWorker(vm);
                worker.start(new HaCompletion(chain) {
                    @Override
                    public void noNeed(String details) {
                        logger.debug(details);
                        tracker.track(VmHaTaskTracker.Process.noNeed, details);
                        chain.next();
                    }

                    @Override
                    public void success() {
                        tracker.track(VmHaTaskTracker.Process.success, i18n("HA is successfully completed"));
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to perform HA to the vm[name:%s, uuid:%s], %s", vm.getName(), vm.getUuid(), errorCode));
                        tracker.error(errorCode);
                        tracker.track(VmHaTaskTracker.Process.failure, i18n("Failed to HA the VM"));
                        if (retryOnFailure) {
                            submitNeverStopVmGCJob(vm);
                        }
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

    @Override
    public boolean stop() {
        if (neverStopVmScanTracker != null) {
            neverStopVmScanTracker.cancel(true);
        }
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CancelStorageHeartbeatFileAndGatewayPingOnHostMsg) {
            handle((CancelStorageHeartbeatFileAndGatewayPingOnHostMsg) msg);
        } else if (msg instanceof SetupStorageHeartbeatFileAndGatewayPingOnHostMsg) {
            handle((SetupStorageHeartbeatFileAndGatewayPingOnHostMsg) msg);
        } else if (msg instanceof AddVmFencerRuleToHostMsg) {
            handle((AddVmFencerRuleToHostMsg) msg);
        } else if (msg instanceof RemoveVmFencerRuleFromHostMsg) {
            handle((RemoveVmFencerRuleFromHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(RemoveVmFencerRuleFromHostMsg msg) {
        RemoveVmFencerRuleFromHostCmd cmd = new RemoveVmFencerRuleFromHostCmd();
        cmd.allowRules = msg.getAllowRules();
        cmd.blockRules = msg.getBlockRules();
        cmd.hostUuid = msg.getHostUuid();

        KVMHostAsyncHttpCallMsg callMsg = new KVMHostAsyncHttpCallMsg();
        callMsg.setCommand(cmd);
        callMsg.setHostUuid(cmd.hostUuid);
        callMsg.setPath(REMOVE_VM_FENCER_RULE_FROM_HOST);
        bus.makeTargetServiceIdByResourceUuid(callMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(callMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug(String.format("remove vm fencer rule on host[uuid:%s]  success!", cmd.hostUuid));
                    bus.reply(msg, reply);
                    return;
                }

                RemoveVmFencerRuleFromHostReply reply1 = new RemoveVmFencerRuleFromHostReply();
                logger.debug(String.format("remove vm fencer rule on host[uuid:%s] fail, error: %s",
                        cmd.hostUuid, reply.getError()));
                reply1.setError(reply.getError());
                bus.reply(msg, reply1);
            }
        });
    }

    private void handle(AddVmFencerRuleToHostMsg msg) {
        AddVmFencerRuleToHostCmd cmd = new AddVmFencerRuleToHostCmd();
        cmd.allowRules = msg.getAllowRules();
        cmd.blockRules = msg.getBlockRules();
        cmd.hostUuid = msg.getHostUuid();

        KVMHostAsyncHttpCallMsg callMsg = new KVMHostAsyncHttpCallMsg();
        callMsg.setCommand(cmd);
        callMsg.setHostUuid(cmd.hostUuid);
        callMsg.setPath(ADD_VM_FENCER_RULE_TO_HOST);
        bus.makeTargetServiceIdByResourceUuid(callMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(callMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug(String.format("add vm rule to fencer on  host[uuid:%s]  success!", cmd.hostUuid));
                    bus.reply(msg, reply);
                    return;
                }

                AddVmFencerRuleToHostReply reply1 = new AddVmFencerRuleToHostReply();
                logger.debug(String.format("add vm rule to fencer on  host[uuid:%s] fail, error: %s",
                        cmd.hostUuid, reply.getError()));
                reply1.setError(reply.getError());
                bus.reply(msg, reply1);
            }
        });
    }

    private void handle(CancelStorageHeartbeatFileAndGatewayPingOnHostMsg msg) {
        SelfFencerStruct param = new SelfFencerStruct();
        param.setHost(msg.getHostInventory());
        param.setPrimaryStorage(Collections.singletonList(msg.getPrimaryStorageInventory()));
        CancelStorageHeartbeatFileAndGatewayPing cancel = new CancelStorageHeartbeatFileAndGatewayPing(param);
        cancel.cancel();
        CancelStorageHeartbeatFileAndGatewayPingOnHostReply reply = new CancelStorageHeartbeatFileAndGatewayPingOnHostReply();
        bus.reply(msg, reply);
    }

    private void handle(SetupStorageHeartbeatFileAndGatewayPingOnHostMsg msg) {
        SelfFencerStruct param = new SelfFencerStruct();
        param.setHost(msg.getHostInventory());
        param.setPrimaryStorage(Collections.singletonList(msg.getPrimaryStorageInventory()));
        param.setFencers(HaHelper.findExecuteFencers());
        SetupStorageHeartbeatFileFencer fencer = new SetupStorageHeartbeatFileFencer(param);
        fencer.setup();
        SetupStorageHeartbeatFileAndGatewayPingOnHostReply reply = new SetupStorageHeartbeatFileAndGatewayPingOnHostReply();
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISetVmInstanceHaLevelMsg) {
            handle((APISetVmInstanceHaLevelMsg) msg);
        } else if (msg instanceof APIDeleteVmInstanceHaLevelMsg) {
            handle((APIDeleteVmInstanceHaLevelMsg) msg);
        } else if (msg instanceof APIGetVmInstanceHaLevelMsg) {
            handle((APIGetVmInstanceHaLevelMsg) msg);
        } else if (msg instanceof APIUpdateHaStrategyConditionMsg) {
            handle((APIUpdateHaStrategyConditionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateHaStrategyConditionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateHaStrategyConditionEvent event = new APIUpdateHaStrategyConditionEvent(msg.getId());
                updateHaStrategyCondition(msg.getUuid(), msg.getState(), new ReturnValueCompletion<HaStrategyConditionInventory>(chain) {
                    @Override
                    public void success(HaStrategyConditionInventory inventory) {
                        event.setInventory(inventory);
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
            public String getSyncSignature() {
                return String.format("update-haStrategyCondition-%s", msg.getUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void updateHaStrategyCondition(String haStrategyConditionUuid, String state, ReturnValueCompletion<HaStrategyConditionInventory> completion) {
        HaStrategyConditionVO vo = dbf.findByUuid(haStrategyConditionUuid, HaStrategyConditionVO.class);
        vo.setState(HaStrategyState.valueOf(state));
        if (vo.getFencerName().equalsIgnoreCase(HaConstants.HOST_STORAGE_STATE)) {
            updateSelfFencerStrategy(vo);
            completion.success(HaStrategyConditionInventory.valueOf(vo));
            return;
        }
        dbf.updateAndRefresh(vo);

        setupSelfFencerOnAllHosts();
        completion.success(HaStrategyConditionInventory.valueOf(vo));
    }

    private void updateSelfFencerStrategy(HaStrategyConditionVO vo) {
        if (vo.getState() == HaStrategyState.Enable) {
            HaGlobalConfig.SELF_FENCER_STRATEGY.updateValue(SelfFencerStrategy.Force);
            return;
        }
        HaGlobalConfig.SELF_FENCER_STRATEGY.updateValue(SelfFencerStrategy.Permissive);
    }

    private void handle(APIGetVmInstanceHaLevelMsg msg) {
        APIGetVmInstanceHaLevelReply reply = new APIGetVmInstanceHaLevelReply();
        VmHaVO ha = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, msg.getUuid())
                .find();
        reply.setLevel(ha.getHaLevel().isEnabled() ? ha.getHaLevel().toString() : null);
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteVmInstanceHaLevelMsg msg) {
        APIDeleteVmInstanceHaLevelEvent event = new APIDeleteVmInstanceHaLevelEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("modify-vm-%s-ha-level", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                vmHAExecutor.setHALevelForVM(msg.getUuid())
                        .toDisabled()
                        .update();


                String hostUuid = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, msg.getUuid())
                        .select(VmInstanceVO_.hostUuid)
                        .findValue();

                if (hostUuid != null) {
                    syncHaVmListOnHost(hostUuid);
                }

                chain.next();
                bus.publish(event);
            }

            @Override
            public String getName() {
                return String.format("modify-vm-%s-ha-level", msg.getUuid());
            }
        });
    }

    private void handle(APISetVmInstanceHaLevelMsg msg) {
        APISetVmInstanceHaLevelEvent event = new APISetVmInstanceHaLevelEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("modify-vm-%s-ha-level", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                Tuple tuple = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, msg.getUuid())
                        .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                        .findTuple();

                VmInstanceState state = tuple.get(0, VmInstanceState.class);
                String hostUuid = tuple.get(1, String.class);

                if (state == VmInstanceState.Stopped && VmHaLevel.valueOf(msg.getLevel()).isEnabled()) {
                    vmHAExecutor.setHALevelForVM(msg.getUuid())
                            .to(msg.getLevel())
                            .inhibitHATemporarily("Enable HA for Stopped VM")
                            .update();
                } else {
                    vmHAExecutor.setHALevelForVM(msg.getUuid())
                            .to(msg.getLevel())
                            .update();
                }

                if (hostUuid != null) {
                    syncHaVmListOnHost(hostUuid);
                }

                chain.next();
                bus.publish(event);
            }

            @Override
            public String getName() {
                return String.format("modify-vm-%s-ha-level", msg.getUuid());
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(HaConstants.SERVICE_ID);
    }

    @Override
    public void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        List<HostVO> hosts = q.list();
        if (hosts.isEmpty()) {
            return;
        }

        List<HostInventory> hostInvs = hosts.stream().map(HostInventory::valueOf).collect(Collectors.toList());
        setupStorageHeartbeatFileFencerOnHosts(list(inventory), hostInvs);
    }

    @Override
    public void beforeUpdatePrimaryStorage(PrimaryStorageInventory ps) {

    }

    @Override
    public void afterUpdatePrimaryStorage(PrimaryStorageInventory ps) {
        List<HostVO> hosts = SQL.New("select host" +
                " from HostVO host, PrimaryStorageClusterRefVO ref" +
                " where ref.primaryStorageUuid = :psUuid" +
                " and host.clusterUuid = ref.clusterUuid", HostVO.class)
                .param("psUuid", ps.getUuid()).list();
        if (hosts.isEmpty()) {
            return;
        }
        List<HostInventory> hostInvs = hosts.stream().map(HostInventory::valueOf).collect(Collectors.toList());
        setupStorageHeartbeatFileFencerOnHosts(list(ps), hostInvs);

    }

    private void setupStorageHeartbeatFileFencerOnHosts(List<PrimaryStorageInventory> ps, List<HostInventory> hosts){
        ps = ps.stream().filter(it -> PrimaryStorageType.valueOf(it.getType()).isSupportHeartbeatFile())
                .collect(Collectors.toList());
        if (ps.isEmpty() || hosts.isEmpty()){
            return;
        }

        for (HostInventory h : hosts) {
            SelfFencerStruct param = new SelfFencerStruct();
            param.setHost(h);
            param.setPrimaryStorage(ps);
            param.setFencers(HaHelper.findExecuteFencers());
            new SetupStorageHeartbeatFileFencer(param).setup();
        }
    }

    @Override
    public List<String> orderVmBeforeMigrationDuringHostMaintenance(HostInventory host, List<String> vmUuids) {
        List<VmHaVO> haList = Q.New(VmHaVO.class)
                .in(VmHaVO_.uuid, vmUuids)
                .in(VmHaVO_.haLevel, list(NeverStop, OnHostFailure))
                .list();
        if (haList.isEmpty()) {
            return vmUuids;
        }

        List<String> neverStopped = new ArrayList<>();
        List<String> onHostFailure = new ArrayList<>();
        List<String> haVmFirst = new ArrayList<>();

        for (VmHaVO ha : haList) {
            if (ha.getHaLevel() == NeverStop) {
                neverStopped.add(ha.getUuid());
            } else if (ha.getHaLevel() == OnHostFailure) {
                onHostFailure.add(ha.getUuid());
            }
        }

        haVmFirst.addAll(neverStopped);
        haVmFirst.addAll(onHostFailure);
        for (String uuid : vmUuids) {
            if (!haVmFirst.contains(uuid)) {
                haVmFirst.add(uuid);
            }
        }

        return haVmFirst;
    }

    class CancelStorageHeartbeatFileAndGatewayPing {
        SelfFencerStruct param;

        public CancelStorageHeartbeatFileAndGatewayPing(SelfFencerStruct param) {
            this.param = param;
        }

        void cancel() {
            final HostInventory host = param.getHost();
            final HaHypervisorFactory f = hypervisorFactories.get(host.getHypervisorType());
            if (f == null) {
                logger.warn(String.format("cannot find HaHypervisorFactory for the hypervisor type[%s], no need to cancel its self-fencer",
                        host.getHypervisorType()));
                return;
            }

            SelfFencerHypervisorBackend bkd = f.createSelfFencerBackend(param);
            bkd.cancel(new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully cancelled the self-fencer on the host[uuid:%s, name:%s]", host.getUuid(), host.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("unable to cancel the self-fencer on the host[uuid:%s, name:%s], %s",
                            host.getUuid(), host.getName(), errorCode));
                }
            });
        }
    }

    class SetupStorageHeartbeatFileFencer {
        SelfFencerStruct param;

        SetupStorageHeartbeatFileFencer(SelfFencerStruct param) {
            this.param = param;
        }

        void setup() {
            if (!isHaEnabled()) {
                logger.debug(String.format("the global config[%s] is set to false, skip setting up self-fencer on the hosts",
                        HaGlobalConfig.ALL.getCanonicalName()));
                return;
            }

            final HostInventory host = param.getHost();
            final HaHypervisorFactory f = hypervisorFactories.get(host.getHypervisorType());
            if (f == null) {
                logger.warn(String.format("cannot find HaHypervisorFactory for the hypervisor type[%s], no way to setup self-fencer",
                        host.getHypervisorType()));
                return;
            }

            SelfFencerHypervisorBackend bkd = f.createSelfFencerBackend(param);
            bkd.setup(new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully setup the self-fencer on the host[uuid:%s, name:%s]", host.getUuid(),
                            host.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to setup the self-fencer on the host[uuid:%s, name:%s], %s", host.getUuid(),
                            host.getName(), errorCode));
                }
            });
        }
    }

    @Transactional(readOnly = true)
    private List<PrimaryStorageInventory> getSelfFencerSupportedPrimaryStorage(HostInventory host) {
        String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref where" +
                " pri.uuid = ref.primaryStorageUuid and ref.clusterUuid = :cuuid";
        TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
        q.setParameter("cuuid", host.getClusterUuid());
        List<PrimaryStorageVO> ps = q.getResultList();
        if (ps.isEmpty()) {
            return new ArrayList<PrimaryStorageInventory>();
        }

        List<PrimaryStorageInventory> invs = new ArrayList<PrimaryStorageInventory>();
        for (PrimaryStorageVO vo : ps) {
            PrimaryStorageType type = PrimaryStorageType.valueOf(vo.getType());
            if (type.isSupportHeartbeatFile()) {
                invs.add(PrimaryStorageInventory.valueOf(vo));
            }
        }

        return invs;
    }

    private boolean isHaEnabled() {
        return HaGlobalConfig.ALL.value(Boolean.class)
                && !MevocoGlobalConfig.PAUSE_THE_WORLD.value(Boolean.class);
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (!isHaEnabled()) {
            return;
        }

        List<PrimaryStorageInventory> ps = getSelfFencerSupportedPrimaryStorage(host);
        if (ps.isEmpty()) {
            return;
        }

        SelfFencerStruct param = new SelfFencerStruct();
        param.setPrimaryStorage(ps);
        param.setHost(host);
        param.setFencers(HaHelper.findExecuteFencers());
        new SetupStorageHeartbeatFileFencer(param).setup();

        List<String> vmUuids = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.hostUuid, host.getUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Unknown, VmInstanceState.Paused))
                .select(VmInstanceVO_.uuid)
                .listValues();

        if (CollectionUtils.isEmpty(vmUuids)) {
            return;
        }

        addVmFencerRuleToHost(vmUuids, host.getUuid());
        syncHaVmListOnHost(host.getUuid());
    }

    private void checkVmAllVolumePrimaryStorageState(String vmUuid) {
        String sql = "select uuid from PrimaryStorageVO where uuid in (" +
                " select distinct(primaryStorageUuid) from VolumeVO" +
                " where vmInstanceUuid = :vmUuid and primaryStorageUuid is not null)" +
                " and state = :psState";
        List<String> result = SQL.New(sql, String.class)
                .param("vmUuid", vmUuid)
                .param("psState", PrimaryStorageState.Maintenance)
                .list();
        if (result != null && !result.isEmpty()) {
            throw new OperationFailureException(argerr("the VM[uuid:%s] volume stored location primary storage is in a state of maintenance", vmUuid));
        }
    }

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {
        // do nothing
    }

    @Override
    public void afterPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        VmHaVO ha = new VmHaVO();
        ha.setUuid(vo.getUuid());
        ha.setHaLevel(Undefined);
        ha.setHaLevelUpdateTime(vo.getCreateDate());
        dbf.persist(ha);
    }

    @Override
    public void afterPersistApplianceVmInstanceVO(VmInstanceVO vo) {
        VmHaVO ha = new VmHaVO();
        ha.setUuid(vo.getUuid());
        ha.setHaLevel(Undefined);
        ha.setHaLevelUpdateTime(vo.getCreateDate());
        dbf.persist(ha);
    }

    @Override
    public void preHaStartVm(String vmUuid) {
        checkVmAllVolumePrimaryStorageState(vmUuid);
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        removeVmFencerRuleFromHost(inv, inv.getHostUuid());
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        addVmFencerRuleToHost(inv, inv.getHostUuid());
        syncHaVmListOnHost(inv.getHostUuid());
        syncHaVmListOnHost(srcHostUuid);
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (destHostUuid == null) {
            return;
        }

        boolean neverStop = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, inv.getUuid())
                .eq(VmHaVO_.haLevel, NeverStop)
                .isExists();
        if (neverStop) {
            migrationFailedVMs.putIfAbsent(inv.getUuid(), destHostUuid);
        }

        addVmFencerRuleToHost(inv, inv.getHostUuid());
    }

    public void syncHaVmListOnHost(String hostUuid) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public void run(SyncTaskChain chain) {
                List<String> haVmList = isHaEnabled() ? findAllHaVmListOnHost(hostUuid) : Collections.emptyList();

                // Even if the haVmList is empty, sync still needs to be executed
                syncHaVmListOnHost(hostUuid, haVmList, new Completion(chain) {
                    @Override
                    public void success() {
                        logger.debug(String.format("sync ha vm list on host[%s] successfully", hostUuid));
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format(
                                "Failed to sync ha vm list on host[%s]: %s", hostUuid, errorCode.getDetails()));
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public String getName() {
                return String.format("sync-ha-vm-list-on-host-%s", hostUuid);
            }

            public List<String> findAllHaVmListOnHost(String hostUuid) {
                List<String> allVmsInThisHost = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.uuid)
                        .eq(VmInstanceVO_.hostUuid, hostUuid)
                        .listValues();
                if (allVmsInThisHost.isEmpty()) {
                    return Collections.emptyList();
                }

                return Q.New(VmHaVO.class)
                        .select(VmHaVO_.uuid)
                        .in(VmHaVO_.uuid, allVmsInThisHost)
                        .eq(VmHaVO_.haLevel, NeverStop)
                        .listValues();
            }
        });
    }

    private void syncHaVmListOnHost(String hostUuid, List<String> vmUuidList, Completion completion) {
        logger.debug(String.format("The following VMs on Host[%s] has HA enabled: %s", hostUuid, vmUuidList));

        HaKvmHostSiblingChecker.SyncHaVmListCmd cmd = new HaKvmHostSiblingChecker.SyncHaVmListCmd();
        cmd.hostUuid = hostUuid;
        cmd.vmUuids = vmUuidList;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setPath(HaKvmHostSiblingChecker.SYNC_HA_VM_LIST_PATH);
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
