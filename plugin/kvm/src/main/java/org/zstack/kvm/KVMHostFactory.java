package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.compute.vm.CrashStrategy;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmNicManager;
import org.zstack.compute.vm.VmBootTimeUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.config.schema.GuestOsCategory;
import org.zstack.core.config.schema.GuestOsCharacter;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.agent.ProxyHardware;
import org.zstack.header.agent.ProxyHardwareFactory;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.GuestOsCategoryVO;
import org.zstack.header.image.GuestOsCategoryVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.header.tag.FormTagExtensionPoint;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.tag.SystemTagOperationJudger;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO_;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.form.Form;
import org.zstack.utils.function.Function;
import org.zstack.utils.function.ValidateFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMConstant.CPU_MODE_NONE;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transform;

public class KVMHostFactory extends AbstractService implements HypervisorFactory, Component,
        ManagementNodeReadyExtensionPoint, MaxDataVolumeNumberExtensionPoint, HypervisorMessageFactory, ProxyHardwareFactory {
    private static final CLogger logger = Utils.getLogger(KVMHostFactory.class);
    private static final VmBootTimeUtils vmBootTimeUtils = new VmBootTimeUtils();

    public static final HypervisorType hypervisorType = new HypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
    public static final VolumeFormat QCOW2_FORMAT = new VolumeFormat(VolumeConstant.VOLUME_FORMAT_QCOW2, hypervisorType);
    public static final VolumeFormat RAW_FORMAT = new VolumeFormat(VolumeConstant.VOLUME_FORMAT_RAW, hypervisorType);
    public static final VolumeFormat VMDK_FORMAT = new VolumeFormat(VolumeConstant.VOLUME_FORMAT_VMDK, hypervisorType);
    private List<KVMHostConnectExtensionPoint> connectExtensions = new ArrayList<>();
    private final Map<L2NetworkType, KVMCompleteNicInformationExtensionPoint> completeNicInfoExtensions = new HashMap<>();
    private int maxDataVolumeNum;
    private static final String GUEST_OS_CATEGORY_FILE = "guestOs/guestOsCategory.xml";
    private static final String GUEST_OS_CHARACTER_FILE = "guestOs/guestOsCharacter.xml";
    public static Map<String, GuestOsCategory.Config> allGuestOsCategory = new ConcurrentHashMap<>();
    public static Map<String, GuestOsCharacter.Config> allGuestOsCharacter = new ConcurrentHashMap<>();

    private final Map<SocketChannel, Long> socketTimeoutMap = new ConcurrentHashMap<>();

    static {
        RAW_FORMAT.newFormatInputOutputMapping(hypervisorType, QCOW2_FORMAT.toString());
        VMDK_FORMAT.newFormatInputOutputMapping(hypervisorType, QCOW2_FORMAT.toString());
        QCOW2_FORMAT.setFirstChoice(hypervisorType);
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private EventFacade evf;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private VmNicManager vmNicManager;

    private Future<Void> checkSocketChannelTimeoutThread;

    @Override
    public HostVO createHost(HostVO vo, AddHostMessage msg) {
        if (!(msg instanceof AddKVMHostMessage)) {
            throw new OperationFailureException(operr("cluster[uuid:%s] hypervisorType is not %s", msg.getClusterUuid(), KVMConstant.KVM_HYPERVISOR_TYPE));
        }

        AddKVMHostMessage amsg = (AddKVMHostMessage) msg;
        KVMHostVO kvo = new KVMHostVO(vo);
        kvo.setUsername(amsg.getUsername());
        kvo.setPassword(amsg.getPassword());
        kvo.setPort(amsg.getSshPort());
        kvo = dbf.persistAndRefresh(kvo);
        return kvo;
    }

    @Override
    public List<AddHostMsg> buildMessageFromFile(String content, ValidateFunction<AddHostMsg> validator) {
        try {
            List<AddKVMHostMsg> msgs = loadMsgFromFile(content, validator);
            return prepareMsgHostName(msgs);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationFailureException(operr("fail to load host info from file. because\n%s", e.getMessage()));
        }
    }

    private List<AddHostMsg> prepareMsgHostName(List<AddKVMHostMsg> msgs) {
        Map<String, List<AddKVMHostMsg>> nameMsgsMap = new HashMap<>();
        msgs.forEach(it -> nameMsgsMap.computeIfAbsent(it.getName(), k -> new ArrayList<>()).add(it));

        List<AddHostMsg> renamed = nameMsgsMap.entrySet().stream()
                .filter(e -> e.getValue().size() > 1 || StringUtils.isEmpty(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .peek(msg -> msg.setName((StringUtils.isEmpty(msg.getName()) ? "HOST" : msg.getName()) + "-" + msg.getManagementIp()))
                .collect(Collectors.toList());

        List<AddHostMsg> origins = nameMsgsMap.entrySet().stream()
                .filter(e -> e.getValue().size() == 1 && !StringUtils.isEmpty(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());

        renamed.addAll(origins);
        return renamed;
    }

    private List<AddKVMHostMsg> loadMsgFromFile(String content, ValidateFunction<? super AddKVMHostMsg> validator) throws IOException {
        int limit = HostGlobalConfig.BATCH_ADD_HOST_LIMIT.value(Integer.class);
        Map<String, Function<String, String>> extensionTagMappers = new HashMap<>();
        pluginRgty.getExtensionList(FormTagExtensionPoint.class).forEach(it -> extensionTagMappers.putAll(it.getTagMappers(AddKVMHostMsg.class)));

        Form<AddKVMHostMsg> form = Form.New(AddKVMHostMsg.class, content, limit)
                .addHeaderConverter(head -> (head.matches(".*\\(.*\\).*") ? head.split("[()]")[1] : head)
                        .replaceAll("\\*", ""))
                .addColumnConverter("managementIps", it -> IpRangeSet.listAllIps(it, limit), AddHostMsg::setManagementIp);

        extensionTagMappers.forEach((columnName, builder) ->
                form.addColumnConverter(columnName, (it, value) -> {
                    String tag = builder.call(value);
                    if (tag != null) {
                        it.addSystemTag(tag);
                    }
                }));

        return form.withValidator(validator).load();
    }

    @Override
    public Host getHost(HostVO vo) {
        KVMHostVO kvo = dbf.findByUuid(vo.getUuid(), KVMHostVO.class);
        KVMHostContext context = getHostContext(vo.getUuid());
        if (context == null) {
            context = createHostContext(kvo);
        }
        return new KVMHost(kvo, context);
    }

    private List<String> getHostManagedByUs() {
        int qun = 10000;
        long amount = dbf.count(HostVO.class);
        int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
        List<String> hostUuids = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < times; i++) {
            List<String> lst = Q.New(KVMHostVO.class)
                    .select(HostVO_.uuid)
                    // disconnected host will be handled by HostManager
                    .eq(HostVO_.status, HostStatus.Connected)
                    .limit(qun)
                    .start(start)
                    .listValues();
            start += qun;
            for (String huuid : lst) {
                if (!destMaker.isManagedByUs(huuid)) {
                    continue;
                }
                hostUuids.add(huuid);
            }
        }

        return hostUuids;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public HostInventory getHostInventory(HostVO vo) {
        KVMHostVO kvo = vo instanceof KVMHostVO ? (KVMHostVO) vo : dbf.findByUuid(vo.getUuid(), KVMHostVO.class);
        return KVMHostInventory.valueOf(kvo);
    }

    @Override
    public HostInventory getHostInventory(String uuid) {
        KVMHostVO vo = dbf.findByUuid(uuid, KVMHostVO.class);
        return vo == null ? null : KVMHostInventory.valueOf(vo);
    }

    @Override
    public boolean supportGetHostOs() {
        return true;
    }

    @Override
    public HostOperationSystem getHostOS(String uuid) {
        Tuple tuple = Q.New(KVMHostVO.class)
                .select(KVMHostVO_.osDistribution, KVMHostVO_.osRelease, KVMHostVO_.osVersion)
                .eq(KVMHostVO_.uuid, uuid)
                .findTuple();
        return HostOperationSystem.of(tuple.get(0, String.class), tuple.get(1, String.class), tuple.get(2, String.class));
    }

    @Override
    public Map<String, HostOperationSystem> getHostOsMap(Collection<String> hostUuidList) {
        if (CollectionUtils.isEmpty(hostUuidList)) {
            return Collections.emptyMap();
        }

        List<Tuple> tuples = Q.New(KVMHostVO.class)
                .select(KVMHostVO_.osDistribution, KVMHostVO_.osRelease, KVMHostVO_.osVersion, KVMHostVO_.uuid)
                .in(KVMHostVO_.uuid, hostUuidList)
                .listTuple();
        return tuples.stream().collect(Collectors.toMap(
                tuple -> tuple.get(3, String.class),
                tuple -> HostOperationSystem.of(
                        tuple.get(0, String.class), tuple.get(1, String.class), tuple.get(2, String.class))));
    }

    @Override
    public ErrorCode checkNewAddedHost(HostVO vo) {
        final HostOperationSystem os = getHostOS(vo.getUuid());
        if (!os.isValid()) {
            return operr("the operation system[%s] of host[name:%s, ip:%s] is invalid",
                    os, vo.getName(), vo.getManagementIp());
        }

        String otherHostUuid = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, vo.getClusterUuid())
                .notEq(HostVO_.uuid, vo.getUuid())
                .notEq(HostVO_.status, HostStatus.Connecting)
                .limit(1)
                .findValue();
        if (otherHostUuid == null) {
            // this the first host in cluster
            return null;
        }

        final HostOperationSystem otherOs = getHostOS(otherHostUuid);
        if (!otherOs.isValid()) {
            // this the first host in cluster
            return null;
        }

        if (os.equals(otherOs)) {
            return null;
        }

        return operr("cluster[uuid:%s] already has host with os version[%s], but new added host[name:%s ip:%s] has different host os version[%s]",
                vo.getClusterUuid(), otherOs, vo.getName(), vo.getManagementIp(), os);
    }

    protected void populateExtensions() {
        connectExtensions = pluginRgty.getExtensionList(KVMHostConnectExtensionPoint.class);
        for (KVMCompleteNicInformationExtensionPoint ext : pluginRgty.getExtensionList(KVMCompleteNicInformationExtensionPoint.class)) {
            KVMCompleteNicInformationExtensionPoint old = completeNicInfoExtensions.get(ext.getL2NetworkTypeVmNicOn());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate KVMCompleteNicInformationExtensionPoint[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getL2NetworkTypeVmNicOn()));
            }
            completeNicInfoExtensions.put(ext.getL2NetworkTypeVmNicOn(), ext);
        }
    }

    public KVMCompleteNicInformationExtensionPoint getCompleteNicInfoExtension(L2NetworkType type) {
        KVMCompleteNicInformationExtensionPoint extp = completeNicInfoExtensions.get(type);
        if (extp == null) {
            throw new IllegalArgumentException(String.format("unble to fine KVMCompleteNicInformationExtensionPoint supporting L2NetworkType[%s]", type));
        }
        return extp;
    }

    private void deployAnsibleModule() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(KVMConstant.ANSIBLE_MODULE_PATH, KVMConstant.ANSIBLE_PLAYBOOK_NAME);
    }

    void physicalCpuStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalCpuStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalCpuStatusAbnormalData();
        cdata.setCpuName(cmd.getAdditionalProperties().get(KVMConstant.CPU_NAME).toString());
        cdata.setHostUuid(cmd.getHost());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.PHSICAL_DEVICE_STATUS_NAME).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_CPU_STATUS_ABNORMAL, cdata);
    }

    void physicalMemoryStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalMemoryStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalMemoryStatusAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setLocator(cmd.getAdditionalProperties().get(KVMConstant.MEMORY_LOCATOR_NAME).toString());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.PHSICAL_DEVICE_STATUS_NAME).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL, cdata);
    }

    void physicalGpuStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setPcideviceAddress(cmd.getAdditionalProperties().get(KVMConstant.PCI_DEVICE_ADDRESS).toString());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.PHSICAL_DEVICE_STATUS_NAME).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_GPU_STATUS_ABNORMAL, cdata);
    }

    void physicalPowerSupplyStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalPowerSupplyStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalPowerSupplyStatusAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.PHSICAL_DEVICE_STATUS_NAME).toString());
        cdata.setName(cmd.getAdditionalProperties().get(KVMConstant.DEVICE_NAME).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL, cdata);
    }

    void physicalFanStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalFanStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalFanStatusAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setFanName(cmd.getAdditionalProperties().get(KVMConstant.DEVICE_NAME).toString());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.PHSICAL_DEVICE_STATUS_NAME).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_FAN_STATUS_ABNORMAL, cdata);
    }

    void physicalDiskStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalDiskStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalDiskStatusAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setSerialNumber(cmd.getAdditionalProperties().get(KVMConstant.DEVICE_SERIAL_NUMBER).toString());
        cdata.setEnclosureId(cmd.getAdditionalProperties().get(KVMConstant.ENCLOSURE_DEVICE_ID).toString());
        cdata.setSlotNumber(cmd.getAdditionalProperties().get(KVMConstant.SLOT_NUMBER).toString());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.DRIVE_STATE).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_DISK_STATUS_ABNORMAL, cdata);
    }

    void physicalRaidStatusAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalRaidStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalRaidStatusAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setStatus(cmd.getAdditionalProperties().get(KVMConstant.PHSICAL_DEVICE_STATUS_NAME).toString());
        cdata.setTargetId(cmd.getAdditionalProperties().get(KVMConstant.TARGET_ID).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_RAID_STATUS_ABNORMAL, cdata);
    }

    void physicalVolumeStateAlarmEvent(HostPhysicalDeviceStatusAlarmEventCmd cmd) {
        HostCanonicalEvents.HostPhysicalVolumeStateAbnormalData cdata = new HostCanonicalEvents.HostPhysicalVolumeStateAbnormalData();
        cdata.setHostUuid(cmd.getHost());
        cdata.setState(cmd.getAdditionalProperties().get(KVMConstant.PHYSICAL_DEVICE_STATE_NAME).toString());
        cdata.setDiskUuids(cmd.getAdditionalProperties().get(KVMConstant.PHYSICAL_DEVICE_DISK_UUIDS).toString());
        cdata.setDiskName(cmd.getAdditionalProperties().get(KVMConstant.DEVICE_NAME).toString());
        cdata.setVgName(cmd.getAdditionalProperties().get(KVMConstant.VOLUME_GROUP_NAME).toString());
        evf.fire(HostCanonicalEvents.HOST_PHYSICAL_VOLUME_STATE_ABNORMAL, cdata);
    }

    @Override
    public boolean start() {
        deployAnsibleModule();
        populateExtensions();
        configKVMDeviceType();
        initGuestOsCategory();
        initGuestOsCharacter();

        if (KVMGlobalConfig.ENABLE_HOST_TCP_CONNECTION_CHECK.value(Boolean.class)) {
            try {
                startTcpServer();
                startTcpChannelTimeoutChecker();
            } catch (IOException e) {
                throw new CloudRuntimeException("Failed to start tcp server on management node");
            }
        }

        maxDataVolumeNum = KVMGlobalConfig.MAX_DATA_VOLUME_NUM.value(int.class);
        KVMGlobalConfig.MAX_DATA_VOLUME_NUM.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                maxDataVolumeNum = newConfig.value(int.class);
            }
        });
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(value)) {
                    throw new GlobalConfigException(String.format("%s only allows a size string." +
                                    " A size string is a number with suffix 'T/t/G/g/M/m/K/k/B/b' or without suffix, but got %s",
                            KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getCanonicalName(), value));
                }
            }
        });
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                long valueLong = SizeUtils.sizeStringToBytes(value);
                long _1t = SizeUtils.sizeStringToBytes("1T");
                if (valueLong > _1t || valueLong < 0) {
                    throw new GlobalConfigException(String.format("Value %s  cannot be greater than the 1TB" + " but got %s",
                            KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getCanonicalName(), value));
                }
            }
        });
        ResourceConfig resourceConfig = rcf.getResourceConfig(KVMGlobalConfig.VM_CPU_HYPERVISOR_FEATURE.getIdentity());
        resourceConfig.installValidatorExtension((resourceUuid, oldValue, newValue) -> {
            if (Boolean.TRUE.toString().equals(newValue)) {
                return;
            }

            ResourceConfig cpuMode = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
            if (CPU_MODE_NONE.equals(cpuMode.getResourceConfigValue(resourceUuid, String.class))) {
                throw new GlobalConfigException("Can not disable cpu hypervisor feature with vm.cpuMode none");
            }
        });

        resourceConfig = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        resourceConfig.installValidatorExtension((resourceUuid, oldValue, newValue) -> {
            if (oldValue.equals(newValue)) {
                return;
            }

            VmInstanceState vmState = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state)
                    .eq(VmInstanceVO_.uuid, resourceUuid)
                    .findValue();
            if (vmState != null
                    && vmState != VmInstanceState.Starting  // some configs are set while vm is starting
                    && !VmInstanceState.offlineStates.contains(vmState)) {
                throw new GlobalConfigException("Can not change vm.cpuMode while VM is living.");
            }
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_RECONNECT_ME, ReconnectMeCmd.class, new SyncHttpCallHandler<ReconnectMeCmd>() {
            @Override
            public String handleSyncHttpCall(ReconnectMeCmd cmd) {
                logger.debug(String.format("the kvm host[uuid:%s] asks the management server to reconnect it for %s", cmd.hostUuid, cmd.reason));
                ReconnectHostMsg msg = new ReconnectHostMsg();
                msg.setHostUuid(cmd.hostUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, cmd.hostUuid);
                bus.send(msg);
                return null;
            }
        });
        restf.registerSyncHttpCallHandler(KVMConstant.KVM_TRANSMIT_VM_OPERATION_TO_MN, TransmitVmOperationToMnCmd.class, new SyncHttpCallHandler<TransmitVmOperationToMnCmd>() {
            @Override
            public String handleSyncHttpCall(TransmitVmOperationToMnCmd cmd) {
                logger.debug(String.format("handle vm operation [uuid:%s, operation:%s] transmitted by kvmagent", cmd.uuid, cmd.operation));
                switch (cmd.operation) {
                    case "start":
                        StartVmInstanceMsg smsg = new StartVmInstanceMsg();
                        smsg.setVmInstanceUuid(cmd.uuid);
                        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, cmd.uuid);
                        bus.send(smsg);
                        break;
                    case "stop":
                        StopVmInstanceMsg xmsg = new StopVmInstanceMsg();
                        xmsg.setVmInstanceUuid(cmd.uuid);
                        bus.makeTargetServiceIdByResourceUuid(xmsg, VmInstanceConstant.SERVICE_ID, cmd.uuid);
                        bus.send(xmsg);
                        break;
                    case "reboot":
                        RebootVmInstanceMsg rmsg = new RebootVmInstanceMsg();
                        rmsg.setVmInstanceUuid(cmd.uuid);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, cmd.uuid);
                        bus.send(rmsg);
                        break;
                    default:
                        logger.warn(String.format("vm operation %s transmitted by kvmagent is not supported", cmd.operation));
                }
                return null;
            }
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_REBOOT_EVENT, ReportVmRebootEventCmd.class, cmd -> {
            vmBootTimeUtils.resetBootTime(cmd.vmUuid);
            evf.fire(VmCanonicalEvents.VM_LIBVIRT_REPORT_REBOOT, cmd.vmUuid);

            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_SHUTDOWN_EVENT, ReportVmShutdownEventCmd.class, cmd -> {
            KvmReportVmShutdownEventMsg msg = new KvmReportVmShutdownEventMsg();
            msg.setVmInstanceUuid(cmd.vmUuid);
            msg.setDetail(cmd.detail);
            msg.setOpaque(cmd.opaque);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, cmd.vmUuid);
            bus.send(msg);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_START_EVENT, ReportVmStartEventCmd.class, cmd -> {
            evf.fire(VmCanonicalEvents.VM_LIBVIRT_REPORT_START, cmd.vmUuid);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_CRASH_EVENT, ReportVmCrashEventCmd.class, cmd -> {
            if (!CrashStrategy.valueOf(rcf.getResourceConfigValue(VmGlobalConfig.VM_CRASH_STRATEGY, cmd.vmUuid, String.class)).isCrashStrategyEnable()) {
                return null;
            }
            VmCanonicalEvents.VmCrashReportData cData = new VmCanonicalEvents.VmCrashReportData();
            cData.setVmUuid(cmd.vmUuid);
            cData.setReason(operr("vm[uuid:%s] crashes due to kernel error", cmd.vmUuid));
            evf.fire(VmCanonicalEvents.VM_LIBVIRT_REPORT_CRASH, cData);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_HOST_STOP_EVENT, ReportHostStopEventCmd.class, cmd -> {
            ChangeHostStatusMsg cmsg = new ChangeHostStatusMsg();
            HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.managementIp, cmd.hostIp).find();

            if (hostVO == null) {
                return null;
            }
            cmsg.setUuid(hostVO.getUuid());
            cmsg.setStatusEvent(HostStatusEvent.disconnected.toString());
            bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, cmsg.getHostUuid());
            bus.send(cmsg);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_HOST_FILE_CHANGED, ReportVmHostFileChangedCmd.class, cmd -> {
            VmCanonicalEvents.VmHostFileChangedData data = new VmCanonicalEvents.VmHostFileChangedData();
            data.setHostUuid(cmd.hostUuid);
            data.setVmUuid(cmd.vmUuid);
            data.setTypes(cmd.types);
            evf.fire(VmCanonicalEvents.VM_HOST_FILE_CHANGED_PATH, data);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.KVM_HOST_PHYSICAL_NIC_ALARM_EVENT, KVMAgentCommands.PhysicalNicAlarmEventCmd.class, cmd -> {
            HostCanonicalEvents.HostPhysicalNicStatusData cData = new HostCanonicalEvents.HostPhysicalNicStatusData();
            cData.setHostUuid(cmd.host);
            cData.setInterfaceName(cmd.nic);
            cData.setFromBond(cmd.bond);
            cData.setIpAddress(cmd.ip);
            cData.setInterfaceStatus(cmd.status);
            if("up".equalsIgnoreCase(cmd.status.replaceAll("\\s*", ""))){
                evf.fire(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_UP, cData);
            } else {
                evf.fire(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_DOWN, cData);
            }
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.HOST_PHYSICAL_HARD_STATUS_ALARM_EVENT, HostPhysicalDeviceStatusAlarmEventCmd.class, cmd -> {
            switch (HostHardware.fromString(cmd.getType())) {
                case CPU:
                    physicalCpuStatusAlarmEvent(cmd);
                    break;
                case MEMORY:
                    physicalMemoryStatusAlarmEvent(cmd);
                    break;
                case GPU:
                    physicalGpuStatusAlarmEvent(cmd);
                    break;
                case POWERSUPPLY:
                    physicalPowerSupplyStatusAlarmEvent(cmd);
                    break;
                case FAN:
                    physicalFanStatusAlarmEvent(cmd);
                    break;
                case DISK:
                    physicalDiskStatusAlarmEvent(cmd);
                    break;
                case RAID:
                    physicalRaidStatusAlarmEvent(cmd);
                    break;
                case PHYSICAL_VOLUME:
                    physicalVolumeStateAlarmEvent(cmd);
                    break;

                default:
                    logger.debug(String.format("unknown physical device type[%s] in host[uuid:%s]", cmd.getType(), cmd.getHost()));

            }
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.HOST_PHYSICAL_DISK_INSERT_ALARM_EVENT, HostPhysicalDiskInsertAlarmEventCmd.class, cmd -> {
            HostCanonicalEvents.HostPhysicalDiskData cdata = new HostCanonicalEvents.HostPhysicalDiskData();
            cdata.setHostUuid(cmd.host);
            Object serialNumber = cmd.additionalProperties.get(KVMConstant.DEVICE_SERIAL_NUMBER);
            Object enclosureDeviceId = cmd.additionalProperties.get(KVMConstant.ENCLOSURE_DEVICE_ID);
            Object slotNumber = cmd.additionalProperties.get(KVMConstant.SLOT_NUMBER);
            Object name = cmd.additionalProperties.get(KVMConstant.DEVICE_NAME);
            cdata.setSerialNumber(serialNumber == null ? "" : serialNumber.toString());
            cdata.setEnclosureId(enclosureDeviceId == null ? "" : enclosureDeviceId.toString());
            cdata.setSlotNumber(slotNumber == null ? "" : slotNumber.toString());
            cdata.setName(name == null ? "" : name.toString());
            evf.fire(HostCanonicalEvents.HOST_PHYSICAL_DISK_INSERT_TRIGGERED, cdata);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.HOST_PHYSICAL_DISK_REMOVE_ALARM_EVENT, HostPhysicalDiskRemoveAlarmEventCmd.class, cmd -> {
            HostCanonicalEvents.HostPhysicalDiskData cdata = new HostCanonicalEvents.HostPhysicalDiskData();
            cdata.setHostUuid(cmd.host);
            Object serialNumber = cmd.additionalProperties.get(KVMConstant.DEVICE_SERIAL_NUMBER);
            Object enclosureDeviceId = cmd.additionalProperties.get(KVMConstant.ENCLOSURE_DEVICE_ID);
            Object slotNumber = cmd.additionalProperties.get(KVMConstant.SLOT_NUMBER);
            Object name = cmd.additionalProperties.get(KVMConstant.DEVICE_NAME);
            cdata.setSerialNumber(serialNumber == null ? "" : serialNumber.toString());
            cdata.setEnclosureId(enclosureDeviceId == null ? "" : enclosureDeviceId.toString());
            cdata.setSlotNumber(slotNumber == null ? "" : slotNumber.toString());
            cdata.setName(name == null ? "" : name.toString());
            evf.fire(HostCanonicalEvents.HOST_PHYSICAL_DISK_REMOVE_TRIGGERED, cdata);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.HOST_PHYSICAL_MEMORY_ECC_ERROR_ALARM_EVENT, PhysicalMemoryEccErrorAlarmEventCmd.class, cmd -> {
            HostCanonicalEvents.HostPhysicalMemoryEccErrorData cdata = new HostCanonicalEvents.HostPhysicalMemoryEccErrorData();
            cdata.setDetail(operr("host[uuid: %s] memory ecc triggered, detail: %s", cmd.host, cmd.detail));
            cdata.setHostUuid(cmd.host);
            evf.fire(HostCanonicalEvents.HOST_PHYSICAL_MEMORY_ECC_ERROR_TRIGGERED, cdata);
            return null;
        });

        restf.registerSyncHttpCallHandler(KVMConstant.HOST_PHYSICAL_GPU_REMOVE_ALARM_EVENT, PhysicalGpuRemoveAlarmEventCmd.class, cmd -> {
            HostCanonicalEvents.HostPhysicalGpuRemoveTriggeredData cdata = new HostCanonicalEvents.HostPhysicalGpuRemoveTriggeredData();
            cdata.setHostUuid(cmd.host);
            cdata.setPcideviceAddress(cmd.pcideviceAddress);
            evf.fire(HostCanonicalEvents.HOST_PHYSICAL_GPU_REMOVE_TRIGGERED, cdata);
            return null;
        });

        KVMSystemTags.CHECK_CLUSTER_CPU_MODEL.installValidator(((resourceUuid, resourceType, systemTag) -> {
            String check = KVMSystemTags.CHECK_CLUSTER_CPU_MODEL.getTokenByTag(systemTag, KVMSystemTags.CHECK_CLUSTER_CPU_MODEL_TOKEN);

            if (!Boolean.parseBoolean(check)) {
                return;
            }

            Map<String, String> hostModelMap = getHostsWithDiffModel(resourceUuid);

            if (hostModelMap.isEmpty()) {
                return;
            }

            if (hostModelMap.values().stream().distinct().count() != 1) {
                StringBuilder str = new StringBuilder();
                for (Map.Entry<String, String> entry : hostModelMap.entrySet()) {
                    str.append(String.format("host[uuid:%s]'s cpu model is %s ;\n", entry.getKey(), entry.getValue()));
                }

                throw new OperationFailureException(operr("there are still hosts not have the same cpu model, details: %s", str.toString()));
            }
        }));

        KVMSystemTags.VM_PREDEFINED_PCI_BRIDGE_NUM.installValidator(((resourceUuid, resourceType, systemTag) -> {
            String check = KVMSystemTags.VM_PREDEFINED_PCI_BRIDGE_NUM.getTokenByTag(systemTag, KVMSystemTags.VM_PREDEFINED_PCI_BRIDGE_NUM_TOKEN);
            int num;
            try {
                num = Integer.parseInt(check);
            } catch (Exception e) {
                throw new ApiMessageInterceptionException(argerr("%s must be a number", KVMSystemTags.VM_PREDEFINED_PCI_BRIDGE_NUM_TOKEN));
            }

            if (num <= 0 || num > 31) {
                throw new ApiMessageInterceptionException(argerr("pci bridge need a value greater than 0 and lower than 32", KVMSystemTags.VM_PREDEFINED_PCI_BRIDGE_NUM_TOKEN));
            }
        }));

        KVMGlobalConfig.ENABLE_HOST_TCP_CONNECTION_CHECK.installUpdateExtension((oldConfig, newConfig) -> {
            if (newConfig.value(Boolean.class)) {
                try {
                    startTcpServer();
                    startTcpChannelTimeoutChecker();
                } catch (IOException e) {
                    logger.debug(String.format("Failed to start tcp server, because %s", e));
                }
            } else {
                checkSocketChannelTimeoutThread.cancel(true);
            }
        });

        KVMSystemTags.VOLUME_VIRTIO_SCSI.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                cleanDeviceAddress(tag);
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                cleanDeviceAddress(tag);
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }
        });

        KVMSystemTags.VOLUME_VIRTIO_SCSI.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                VmInstanceVO vm = SQL.New("select vm from VmInstanceVO vm, VolumeVO volume " +
                        "where vm.uuid = volume.vmInstanceUuid and volume.uuid = :uuid", VmInstanceVO.class)
                        .param("uuid", resourceUuid)
                        .find();

                if (vm != null && (vm.getState() == VmInstanceState.Running || vm.getState() == VmInstanceState.Unknown)) {
                    throw new OperationFailureException(argerr("vm current state[%s], " +
                            "modify bus type requires the vm state[%s]", vm.getState(), VmInstanceState.Stopped));
                }

                if (vm != null ) {
                    boolean hasScsiTag = !vm.getAllVolumes(volume -> !Objects.equals(volume.getUuid(), resourceUuid)
                            && KVMSystemTags.VOLUME_SCSI.hasTag(volume.getUuid())).isEmpty();

                    if (hasScsiTag) {
                        throw new OperationFailureException(argerr("vm do not support having both SCSI and Virtio-SCSI bus type volumes simultaneously."));
                    }
                }
            }
        });

        KVMSystemTags.VOLUME_SCSI.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                VmInstanceVO vm = SQL.New("select vm from VmInstanceVO vm, VolumeVO volume " +
                                "where vm.uuid = volume.vmInstanceUuid and volume.uuid = :uuid", VmInstanceVO.class)
                        .param("uuid", resourceUuid)
                        .find();

                if (vm != null && (vm.getState() == VmInstanceState.Running || vm.getState() == VmInstanceState.Unknown)) {
                    throw new OperationFailureException(argerr("vm current state[%s], " +
                            "modify bus type requires the vm state[%s]", vm.getState(), VmInstanceState.Stopped));
                }

                if (vm != null ) {
                    boolean hasVirtioScsiTag = !vm.getAllVolumes(volume -> !Objects.equals(volume.getUuid(), resourceUuid) &&
                            KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volume.getUuid())).isEmpty();

                    if (hasVirtioScsiTag) {
                        throw new OperationFailureException(argerr("vm do not support having both SCSI and Virtio-SCSI bus type volumes simultaneously."));
                    }
                }
            }
        });

        KVMSystemTags.VOLUME_SCSI.installJudger(new SystemTagOperationJudger() {
            @Override
            public void tagPreCreated(SystemTagInventory tag) {
                cleanUpVolumeAddressOnVolumeCapabilityTypeUpdate(tag.getResourceUuid());
            }

            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                cleanUpVolumeAddressOnVolumeCapabilityTypeUpdate(tag.getResourceUuid());
            }

            @Override
            public void tagPreUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                // do-nothing
            }
        });

        KVMSystemTags.VOLUME_VIRTIO_SCSI.installJudger(new SystemTagOperationJudger() {
            @Override
            public void tagPreCreated(SystemTagInventory tag) {
                cleanUpVolumeAddressOnVolumeCapabilityTypeUpdate(tag.getResourceUuid());
            }

            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                cleanUpVolumeAddressOnVolumeCapabilityTypeUpdate(tag.getResourceUuid());
            }

            @Override
            public void tagPreUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                // do-nothing
            }
        });

        return true;
    }

    private void cleanUpVolumeAddressOnVolumeCapabilityTypeUpdate(String relatedVolumeUuid) {
        Tuple vmTuple = Q.New(VolumeVO.class, VmInstanceVO.class)
                .table0()
                    .eq(VolumeVO_.uuid, relatedVolumeUuid)
                    .eq(VolumeVO_.vmInstanceUuid).table1(VmInstanceVO_.uuid)
                .table1()
                    .select(VmInstanceVO_.uuid, VmInstanceVO_.rootVolumeUuid)
                .findTuple();
        if (vmTuple == null) {
            return;
        }

        String vmUuid = vmTuple.get(0, String.class);
        String rootVolumeUuid = vmTuple.get(1, String.class);

        logger.info(String.format(
                "All VmInstanceResourceMetadataVO related VM[uuid:%s] except for the root volume will be cleaned up",
                vmUuid));

        SQL.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmUuid)
                .notIn(VmInstanceResourceMetadataVO_.resourceUuid, list(rootVolumeUuid))
                .delete();
    }

    private void cleanDeviceAddress(SystemTagInventory tag) {
        VolumeVO volume = dbf.findByUuid(tag.getResourceUuid(), VolumeVO.class);
        vidm.deleteVmResourceMetadata(volume.getUuid(), volume.getVmInstanceUuid());
    }

    private void configKVMDeviceType() {
        KVMVmDeviceType.New(VolumeVO.class.getSimpleName(), Collections.singletonList("disk"), (inventories, host) -> {
            List tos = new ArrayList();
            for (Object inventory : inventories) {
                tos.add(VolumeTO.valueOf((VolumeInventory) inventory, host));
            }
            return tos;
        });
    }

    private void initGuestOsCategory() {
        GuestOsCategory configs;
        File guestOsCategoryFile = PathUtil.findFileOnClassPath(GUEST_OS_CATEGORY_FILE);
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.core.config.schema");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            configs = (GuestOsCategory) unmarshaller.unmarshal(guestOsCategoryFile);
        } catch (Exception e){
            throw new CloudRuntimeException(e);
        }
        for (GuestOsCategory.Config config : configs.getOsInfo()) {
            allGuestOsCategory.put(config.getOsRelease(), config);
            if (!Q.New(GuestOsCategoryVO.class).eq(GuestOsCategoryVO_.osRelease, config.getOsRelease()).isExists()) {
                GuestOsCategoryVO vo = new GuestOsCategoryVO();
                vo.setPlatform(config.getPlatform());
                vo.setName(config.getName());
                vo.setVersion(config.getVersion());
                vo.setOsRelease(config.getOsRelease());
                vo.setUuid(Platform.getUuid());
                dbf.persist(vo);
            }
        }

        //delete release not in config
        SQL.New(GuestOsCategoryVO.class).notIn(GuestOsCategoryVO_.osRelease, allGuestOsCategory.keySet()).delete();
    }

    private void initGuestOsCharacter() {
        GuestOsCharacter configs;
        File guestOsCharacterFile = PathUtil.findFileOnClassPath(GUEST_OS_CHARACTER_FILE);
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.core.config.schema");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            configs = (GuestOsCharacter) unmarshaller.unmarshal(guestOsCharacterFile);
        } catch (Exception e){
            throw new CloudRuntimeException(e);
        }
        for (GuestOsCharacter.Config config : configs.getOsInfo()) {
            validateGuestOsCharacter(config);
            allGuestOsCharacter.put(String.format("%s_%s_%s", config.getArchitecture(), config.getPlatform(), config.getOsRelease()), config);
        }
    }

    private void validateGuestOsCharacter(GuestOsCharacter.Config config) {
        if (config.getCpuModel() != null) {
            try {
                KVMGlobalConfig.NESTED_VIRTUALIZATION.validate(config.getCpuModel());
            } catch (GlobalConfigException e) {
                throw new CloudRuntimeException(String.format("Invalid cpu model of config %s", JSONObjectUtil.toJsonString(config)));
            }
        }

        if (config.getNicDriver() == null) {
            return;
        }

        if (!vmNicManager.getSupportNicDriverTypes().contains(config.getNicDriver())) {
            throw new CloudRuntimeException(String.format("Invalid nic driver of config %s, valid values are %s", JSONObjectUtil.toJsonString(config), vmNicManager.getSupportNicDriverTypes()));
        }
    }

    private synchronized void startTcpChannelTimeoutChecker() {
        if (checkSocketChannelTimeoutThread != null) {
            checkSocketChannelTimeoutThread.cancel(true);
        }

        checkSocketChannelTimeoutThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return KVMGlobalConfig.HOST_CONNECTION_CHECK_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "host-tcp-channel-timeout-checker";
            }

            @Override
            public void run() {
                long currentTime = timeHelper.getCurrentTimeMillis();
                Predicate<Map.Entry<SocketChannel, Long>> isQualified = entry -> isTimeout(entry.getValue(), currentTime);
                socketTimeoutMap.entrySet().stream().filter(isQualified)
                .forEach(entry -> {
                    try {
                        checkHostConnection(entry.getKey());
                    } catch (Exception ignore) {
                        logger.debug("ignore exception for socket timeout check");
                    }
                });

                socketTimeoutMap.entrySet().removeIf(isQualified);
            }

            private boolean isTimeout(Long timeInMap, long currentTime) {
                return timeInMap + 5000 < currentTime;
            }
        });
    }

    @AsyncThread
    private void startTcpServer() throws IOException {
        try (Selector selector = Selector.open(); ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            serverSocket.bind(new InetSocketAddress("0.0.0.0", KVMGlobalProperty.TCP_SERVER_PORT));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            ByteBuffer buffer = ByteBuffer.allocate(256);

            Integer interval = KVMGlobalConfig.CONNECTION_SERVER_UPDATE_INTERVAL.value(Integer.class);
            while (KVMGlobalConfig.ENABLE_HOST_TCP_CONNECTION_CHECK.value(Boolean.class)) {
                selector.select(interval);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        register(selector, serverSocket);
                    }

                    if (key.isReadable()) {
                        tryToRead(buffer, key);
                    }

                    iter.remove();
                }
            }
        }
    }

    private void tryToRead(ByteBuffer buffer, SelectionKey key)
            throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        int return_code = client.read(buffer);
        if (return_code != -1) {
            socketTimeoutMap.put(client, timeHelper.getCurrentTimeMillis());
            buffer.clear();
            return;
        }

        checkHostConnection(client);
    }

    private void checkHostConnection(SocketChannel client) throws IOException {
        SocketAddress remoteAddress = client.getRemoteAddress();
        logger.debug("Closed socket remote ip is " + remoteAddress);
        client.close();
        socketTimeoutMap.remove(client, timeHelper.getCurrentTimeMillis());

        String managementIp = remoteAddress.toString().split("/")[1].split(":")[0];
        String hostUuid = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.managementIp, managementIp)
                .findValue();

        if (hostUuid == null) {
            return;
        }

        PingHostMsg pingHostMsg = new PingHostMsg();
        pingHostMsg.setHostUuid(hostUuid);
        bus.makeLocalServiceId(pingHostMsg, HostConstant.SERVICE_ID);
        bus.send(pingHostMsg);
    }

    private void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException {
        SocketChannel client = serverSocket.accept();

        logger.debug("Accepting new client connection from " + client.getRemoteAddress());
        client.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        socketTimeoutMap.put(client, timeHelper.getCurrentTimeMillis());
    }

    private Map<String, String> getHostsWithDiffModel(String clusterUuid) {
        List<String> hostUuidsInCluster = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .listValues();
        if (hostUuidsInCluster.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> diffMap = new HashMap<>();
        for (String hostUuid : hostUuidsInCluster) {
            String hostCpuModel = KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(hostUuid, KVMSystemTags.CPU_MODEL_NAME_TOKEN);

            if (hostCpuModel == null) {
                throw new OperationFailureException(operr("host[uuid:%s] does not have cpu model information, you can reconnect the host to fix it", hostUuid));
            }

            if (diffMap.values().stream().distinct().noneMatch(model -> model.equals(hostCpuModel))) {
                diffMap.put(hostUuid, hostCpuModel);
            }
        }

        return diffMap;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public List<KVMHostConnectExtensionPoint> getConnectExtensions() {
        return connectExtensions;
    }

    public KVMHostContext createHostContext(KVMHostVO vo) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(KVMGlobalProperty.AGENT_URL_SCHEME);
        ub.host(vo.getManagementIp());
        ub.port(KVMGlobalProperty.AGENT_PORT);
        if (!"".equals(KVMGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(KVMGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        String baseUrl = ub.build().toUriString();

        KVMHostContext context = new KVMHostContext();
        context.setInventory(KVMHostInventory.valueOf(vo));
        context.setBaseUrl(baseUrl);
        return context;
    }

    public KVMHostContext getHostContext(String hostUuid) {
        KVMHostVO kvo = dbf.findByUuid(hostUuid, KVMHostVO.class);
        return createHostContext(kvo);
    }

    @Override
    public String getHypervisorTypeForMaxDataVolumeNumberExtension() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public int getMaxDataVolumeNumber() {
        return maxDataVolumeNum;
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (!asf.isModuleChanged(KVMConstant.ANSIBLE_PLAYBOOK_NAME)) {
            return;
        }

        // KVM hosts need to deploy new agent
        // connect hosts even if they are ConnectionState is Connected

        List<String> hostUuids = getHostManagedByUs();
        if (hostUuids.isEmpty()) {
            return;
        }

        logger.debug(String.format("need to connect kvm hosts because kvm agent changed, uuids:%s", hostUuids));

        List<ConnectHostMsg> msgs = new ArrayList<ConnectHostMsg>();
        for (String huuid : hostUuids) {
            ConnectHostMsg msg = new ConnectHostMsg();
            msg.setNewAdd(false);
            msg.setUuid(huuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
            msgs.add(msg);
        }

        bus.send(msgs, HostGlobalConfig.HOST_LOAD_PARALLELISM_DEGREE.value(Integer.class), new CloudBusSteppingCallback(null) {
            @Override
            public void run(NeedReplyMessage msg, MessageReply reply) {
                ConnectHostMsg cmsg = (ConnectHostMsg) msg;
                if (reply.isSuccess()) {
                    logger.debug(String.format("successfully to connect kvm host[uuid:%s]", cmsg.getHostUuid()));
                } else if (reply.isCanceled()) {
                    logger.warn(String.format("canceled connect kvm host[uuid:%s], because it connecting now", cmsg.getHostUuid()));
                } else {
                    logger.warn(String.format("failed to connect kvm host[uuid:%s], %s", cmsg.getHostUuid(), reply.getError()));
                }
            }
        });
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIKvmRunShellMsg) {
            handle((APIKvmRunShellMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIKvmRunShellMsg msg) {
        final APIKvmRunShellEvent evt = new APIKvmRunShellEvent(msg.getId());

        final List<KvmRunShellMsg> kmsgs = transform(msg.getHostUuids(), arg -> {
            KvmRunShellMsg kmsg = new KvmRunShellMsg();
            kmsg.setHostUuid(arg);
            kmsg.setScript(msg.getScript());
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, arg);
            return kmsg;
        });

        bus.send(kmsgs, new CloudBusListCallBack(msg) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    String hostUuid = kmsgs.get(replies.indexOf(r)).getHostUuid();

                    APIKvmRunShellEvent.ShellResult result = new APIKvmRunShellEvent.ShellResult();
                    if (!r.isSuccess()) {
                        result.setErrorCode(r.getError());
                    } else {
                        KvmRunShellReply kr = r.castReply();
                        result.setReturnCode(kr.getReturnCode());
                        result.setStderr(kr.getStderr());
                        result.setStdout(kr.getStdout());
                    }

                    evt.getInventory().put(hostUuid, result);
                }

                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(KVMConstant.SERVICE_ID);
    }

    @Override
    public ProxyHardware getProxyHardware(String hostName) {
        String sql = "select kvm from HostVO host, KVMHostVO kvm " +
                "where host.uuid = kvm.uuid " +
                "and host.managementIp = :hostname";
        return SQL.New(sql, KVMHostVO.class).param("hostname", hostName).find();
    }

    @Override
    public String getClusterUuid(String hostName) {
        return Q.New(HostVO.class)
                .select(HostVO_.clusterUuid)
                .eq(HostVO_.managementIp, hostName)
                .findValue();
    }
}
