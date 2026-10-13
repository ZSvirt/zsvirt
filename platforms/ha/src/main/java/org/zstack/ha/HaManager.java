package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.ha.entity.HaCommands;
import org.zstack.ha.entity.HaStrategyConditionInventory;
import org.zstack.ha.entity.HaStrategyConditionVO;
import org.zstack.ha.entity.HaStrategyConditionVO_;
import org.zstack.ha.entity.HostHaStateVO;
import org.zstack.ha.entity.HostHaStateVO_;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.fencers.HaStrategyHelper;
import org.zstack.ha.spi.VmHaVOUpdater;
import org.zstack.ha.header.*;
import org.zstack.ha.message.*;
import org.zstack.ha.spi.*;
import org.zstack.header.core.*;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.exception.CloudStateMachineException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagInventoryDefinition;
import org.zstack.header.tag.SystemTagOperationJudger;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.ha.fencers.SelfFencers.*;
import static org.zstack.ha.header.HaSystemTags.*;
import static org.zstack.ha.header.VmHaLevel.*;
import static org.zstack.utils.CollectionUtils.toMap;

/**
 * Created by xing5 on 2016/3/28.
 */
public class HaManager extends AbstractService implements Component, HaCompletionCacher,
        PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HaManager.class);

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
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private VmHaVOUpdater vmHAExecutor;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private VmHaService vmHaService;

    private List<String> updateHostStatusPrimaryStorages;

    private void addVmFencerRuleToHost(List<String> vmUuids, String hostUuid) {
        vmFencerRuleList.forEach(ext ->
                ext.addVmFencerRuleToHost(vmUuids, hostUuid));
    }

    public List<String> getUpdateHostStatusPrimaryStorages() {
        return updateHostStatusPrimaryStorages;
    }

    public void setUpdateHostStatusPrimaryStorages(List<String> updateHostStatusPrimaryStorages) {
        this.updateHostStatusPrimaryStorages = updateHostStatusPrimaryStorages;
    }

    private final Map<String, HaHypervisorFactory> hypervisorFactories = new ConcurrentHashMap<>();
    private List<VmFencerRuleExtensionPoint> vmFencerRuleList = new ArrayList<>();

    @Override
    public boolean start() {
        populateExtensions();
        setupGlobalConfig();

        restf.registerSyncHttpCallHandler(HaCommands.ReportVmSelfFencerCmd.PATH, HaCommands.ReportVmSelfFencerCmd.class, cmd -> {
            Map<String, HaCommands.ReportVmSelfFencerTuple> uuidFencerMap =
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
                HaCommands.ReportVmSelfFencerTuple tuple = uuidFencerMap.get(uuid);
                vmHAExecutor.fenceForVM(uuid).record(tuple.fencerName, cmd.hostUuid).update();
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
                            msg.setReason(err(HaErrors.PRIMARY_STORAGE_HOST_DISCONNECTED,
                                    "primary storage %s disconnect on host %s, details: %s",
                                    cmd.psUuids, cmd.hostUuid, cmd.reason));
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
            public void tagPreCreated(SystemTagInventoryDefinition tag) {
                VmHaLevel level = validateHaTag(tag);
                SQL.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, tag.getResourceUuid())
                        .set(VmHaVO_.haLevel, level)
                        .set(VmHaVO_.haLevelUpdateTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                        .update();
            }

            @Override
            public void tagPreDeleted(SystemTagInventoryDefinition tag) {
                SQL.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, tag.getResourceUuid())
                        .set(VmHaVO_.haLevel, None)
                        .set(VmHaVO_.haLevelUpdateTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                        .update();
            }

            @Override
            public void tagPreUpdated(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag) {
                VmHaLevel level = validateHaTag(newTag);
                SQL.New(VmHaVO.class)
                        .eq(VmHaVO_.uuid, newTag.getResourceUuid())
                        .set(VmHaVO_.haLevel, level)
                        .set(VmHaVO_.haLevelUpdateTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                        .update();
            }

            private VmHaLevel validateHaTag(SystemTagInventoryDefinition tag) {
                final String haToken = HA.getTokenByTag(tag.getTag(), HA_TOKEN);
                final VmHaLevel level = VmHaLevel.valueOfOrNull(haToken);
                if (level == null) {
                    throw err(HaErrors.INVALID_HA_LEVEL, "invalid HA level: %s", haToken).toException();
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
                    throw err(HaErrors.INVALID_CHECK_SUCCESS_RATIO,
                            "the value[%s] is lesser than 0 or greater than 1 ", newValue).toException();
                }
            }
        });

        HaGlobalConfig.ALL.installValidateExtension((category, name, oldValue, newValue) -> {
            List<String> hostUuids = getDisconnectedHostsWithVm();
            if (Boolean.parseBoolean(newValue) && !hostUuids.isEmpty()) {
                throw err(HaErrors.DISCONNECTED_HOST_HAS_UNKNOWN_VM,
                        "host[uuid:%s] is not %s, but still have vm on it," +
                        " please resolve hosts' problems before enable ha", hostUuids, HostStatus.Connected).toException();
            }
        });

        HaGlobalConfig.SELF_FENCER_STRATEGY.installValidateExtension((category, name, oldValue, newValue) -> {
            List<String> hostUuids = getDisconnectedHostsWithVm();
            if (!hostUuids.isEmpty()) {
                throw err(HaErrors.DISCONNECTED_HOST_HAS_UNKNOWN_VM,
                        "host[uuid:%s] is not %s, but still have vm on it," +
                        " please resolve hosts' problems before update fencer strategy", hostUuids, HostStatus.Connected).toException();
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

        pluginRgty.getExtensionList(KvmSetupSelfFencerExtensionPoint.class).forEach(ext ->
                HaStrategyHelper.putConsistencyOffered(ext.kvmSetupSelfFencerStorageType(), ext.storageConsistencySupported()));
        vmFencerRuleList = pluginRgty.getExtensionList(VmFencerRuleExtensionPoint.class);
    }

    @Override
    public boolean stop() {
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
        } else if (msg instanceof RetrySetupSelfFencerOnHostMsg) {
            handle((RetrySetupSelfFencerOnHostMsg) msg);
        } else if (msg instanceof RetryCancelSelfFencerOnHostMsg) {
            handle((RetryCancelSelfFencerOnHostMsg) msg);
        } else if (msg instanceof GetFencerStateFromHostMsg) {
            handle((GetFencerStateFromHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(RemoveVmFencerRuleFromHostMsg msg) {
        HaCommands.RemoveVmFencerRuleFromHostCmd cmd = new HaCommands.RemoveVmFencerRuleFromHostCmd();
        cmd.allowRules = msg.getAllowRules();
        cmd.blockRules = msg.getBlockRules();
        cmd.hostUuid = msg.getHostUuid();

        KVMHostAsyncHttpCallMsg callMsg = new KVMHostAsyncHttpCallMsg();
        callMsg.setCommand(cmd);
        callMsg.setHostUuid(cmd.hostUuid);
        callMsg.setPath(HaCommands.RemoveVmFencerRuleFromHostCmd.PATH);
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
        HaCommands.AddVmFencerRuleToHostCmd cmd = new HaCommands.AddVmFencerRuleToHostCmd();
        cmd.allowRules = msg.getAllowRules();
        cmd.blockRules = msg.getBlockRules();
        cmd.hostUuid = msg.getHostUuid();

        KVMHostAsyncHttpCallMsg callMsg = new KVMHostAsyncHttpCallMsg();
        callMsg.setCommand(cmd);
        callMsg.setHostUuid(cmd.hostUuid);
        callMsg.setPath(HaCommands.AddVmFencerRuleToHostCmd.PATH);
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
        param.host = msg.getHostInventory();
        param.primaryStorage = Collections.singletonList(msg.getPrimaryStorageInventory());
        CancelStorageHeartbeatFileAndGatewayPing cancel = new CancelStorageHeartbeatFileAndGatewayPing(param);
        cancel.cancel();
        CancelStorageHeartbeatFileAndGatewayPingOnHostReply reply = new CancelStorageHeartbeatFileAndGatewayPingOnHostReply();
        bus.reply(msg, reply);
    }

    private void handle(SetupStorageHeartbeatFileAndGatewayPingOnHostMsg msg) {
        SelfFencerStruct param = new SelfFencerStruct();
        param.host = msg.getHostInventory();
        param.primaryStorage = Collections.singletonList(msg.getPrimaryStorageInventory());
        param.fencers = HaHelper.findExecuteFencers();
        SetupStorageHeartbeatFileFencer fencer = new SetupStorageHeartbeatFileFencer(param);
        fencer.setup();
        SetupStorageHeartbeatFileAndGatewayPingOnHostReply reply = new SetupStorageHeartbeatFileAndGatewayPingOnHostReply();
        bus.reply(msg, reply);
    }

    private void handle(RetrySetupSelfFencerOnHostMsg msg) {
        List<PrimaryStorageInventory> ps = HaHelper.getSelfFencerSupportedPrimaryStorage(msg.getHostInventory())
                .stream().filter(p -> msg.getPrimaryStorageUuids().contains(p.getUuid())).collect(Collectors.toList());
        if (!ps.isEmpty()) {
            SelfFencerStruct param = new SelfFencerStruct();
            param.host = msg.getHostInventory();
            param.primaryStorage = ps;
            param.fencers = HaHelper.findExecuteFencers();
            new SetupStorageHeartbeatFileFencer(param).setup();
        }
        bus.reply(msg, new RetrySetupSelfFencerOnHostReply());
    }

    private void handle(RetryCancelSelfFencerOnHostMsg msg) {
        List<String> uuids = msg.getPrimaryStorageUuids() == null ? Collections.emptyList() : msg.getPrimaryStorageUuids();
        List<PrimaryStorageVO> vos = uuids.isEmpty() ? Collections.emptyList() : Q.New(PrimaryStorageVO.class)
                .in(PrimaryStorageVO_.uuid, uuids)
                .list();
        Set<String> found = vos.stream().map(PrimaryStorageVO::getUuid).collect(Collectors.toSet());
        String hostUuid = msg.getHostInventory().getUuid();
        for (String psUuid : uuids) {
            if (!found.contains(psUuid)) {
                completeCancelWithoutPrimaryStorage(hostUuid, psUuid);
            }
        }
        if (!vos.isEmpty()) {
            SelfFencerStruct param = new SelfFencerStruct();
            param.host = msg.getHostInventory();
            param.primaryStorage = PrimaryStorageInventory.valueOf(vos);
            new CancelStorageHeartbeatFileAndGatewayPing(param).cancel();
        }
        bus.reply(msg, new RetryCancelSelfFencerOnHostReply());
    }

    private void completeCancelWithoutPrimaryStorage(String hostUuid, String psUuid) {
        HostHaStateVO vo = Q.New(HostHaStateVO.class)
                .eq(HostHaStateVO_.hostUuid, hostUuid)
                .eq(HostHaStateVO_.primaryStorageUuid, psUuid)
                .find();
        if (vo == null) {
            return;
        }
        try {
            vo.setState(HostHaStateMachine.getState(vo.getState(), HostHaStateEvent.cancelDone));
            dbf.update(vo);
        } catch (CloudStateMachineException e) {
            logger.warn(String.format("unable to finish cancel for missing primary storage[uuid:%s] on host[uuid:%s], current state[%s], %s",
                    psUuid, hostUuid, vo.getState(), e.getMessage()));
        }
    }

    private void handle(GetFencerStateFromHostMsg msg) {
        SelfFencerStruct param = new SelfFencerStruct();
        param.host = msg.getHostInventory();
        param.primaryStorage = new ArrayList<>();
        new SyncStorageFencerState(param).sync();
        bus.reply(msg, new GetFencerStateFromHostReply());
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

    public void syncHaVmListOnHost(String hostUuid) {
        vmHaService.syncHaVmListOnHost(hostUuid, new NopeCompletion());
    }
}
