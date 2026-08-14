package org.zstack.compute.cluster;

import org.hibernate.engine.jdbc.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.ovs.OvsGlobalConfig;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vdpa.VmVdpaNicConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.kvm.*;
import org.zstack.pciDevice.*;
import org.zstack.resourceconfig.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 10:29 2019/4/29
 */
public class MevocoClusterManagerImpl implements Component, MevocoClusterManager, KVMStartVmExtensionPoint, HostAddExtensionPoint, HostDeleteExtensionPoint, HostAfterConnectedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoClusterManagerImpl.class);

    @Autowired
    private ResourceConfigFacade rcf;

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean start() {
        setupGlobalConfig();
        return true;
    }

    private void setupGlobalConfig() {
        initValidateExtension();
        initUpdateExtension();
    }

    private void initUpdateExtension() {
        ResourceConfig config = rcf.getResourceConfig(MevocoClusterGlobalConfig.HUGEPAGE_ENABLE.getIdentity());
        config.installLocalUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                updateHugePageState(config, resourceUuid, newValue, oldValue);
            }
        });

        ResourceConfig reservedMemConfig = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getIdentity());
        reservedMemConfig.installLocalUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                /* get current HugePageState of the resource(Cluster) */
                ResourceConfig hugePageConfig = rcf.getResourceConfig(MevocoClusterGlobalConfig.HUGEPAGE_ENABLE.getIdentity());
                String current = hugePageConfig.getResourceConfigValue(resourceUuid, String.class);

                /* update HugePage state again while huge page are enabled */
                if (current.equals("true")) {
                    /* the HugePageState has the same resourceUuid with the associated reservedMemory */
                    updateHugePageState(hugePageConfig, resourceUuid, current, current);
                }
            }
        });

        ResourceConfig zeroCopyConfig = rcf.getResourceConfig(MevocoClusterGlobalConfig.HOST_ENABLE_ZERO_COPY.getIdentity());
        zeroCopyConfig.installLocalUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                updateZeroCopyState(config, resourceUuid, resourceType, newValue, oldValue);
            }
        });

        ResourceConfig ovsDpdkSupConfig = rcf.getResourceConfig(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT.getIdentity());
        ovsDpdkSupConfig.installLocalUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                updateOvsDpdkSupState(config, resourceUuid, resourceType, newValue, oldValue);
            }
        });
    }

    private void checkClusterReserveMem(String resourceUuid) {
        ResourceConfig config = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getIdentity());
        String value = config.getResourceConfigValue(resourceUuid, String.class);
        if (SizeUtils.sizeStringToBytes(value) >= SizeUtils.sizeStringToBytes(MevocoClusterConstants.MINIMUM_RESERVED_MEM_SIZE)) {
            return;
        }
        config.updateValue(resourceUuid, MevocoClusterConstants.MINIMUM_RESERVED_MEM_SIZE);
    }

    private void updateZeroCopyState(ResourceConfig config, String resourceUuid, String resourceType, String newValue, String oldValue) {
        List<String> hostUuids = new ArrayList<>();
        if (resourceType.equals(ClusterVO.class.getSimpleName())) {
            hostUuids = Q.New(HostVO.class)
                    .select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, resourceUuid)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .listValues();
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            if (!Q.New(HostVO.class).eq(HostVO_.status, HostStatus.Connected).eq(HostVO_.uuid, resourceUuid).isExists()) {
                return;
            }
            hostUuids.add(resourceUuid);
        }

        if (hostUuids.isEmpty()) {
            return;
        }

        List<ChangeZeroCopyStateMsg> msgs = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            ChangeZeroCopyStateMsg msg = new ChangeZeroCopyStateMsg();
            msg.setHostUuid(hostUuid);
            msg.setValue(config.getResourceConfigValue(hostUuid, String.class));
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            msgs.add(msg);
        }
        FutureCompletion completion = new FutureCompletion(null);
        ErrorCodeList errList = new ErrorCodeList();
        new While<>(msgs).all((msg, complet) -> {
            bus.send(msg, new CloudBusCallBack(complet) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        logger.warn(String.format("failed to change zero copy state on host:%s", msg.getHostUuid()));
                    }
                    complet.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    private void updateOvsDpdkSupState(ResourceConfig config, String resourceUuid, String resourceType, String newValue, String oldValue){
        // Clusters has individual numa resource config, it is split from global config.
        ResourceConfig memAccessConfig = rcf.getResourceConfig(MevocoClusterGlobalConfig.MEMORY_ACCESS.getIdentity());
        ResourceConfig numaConfig = rcf.getResourceConfig(VmGlobalConfig.NUMA.getIdentity());

        ResourceConfig hostReservedMemoryConfig = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getIdentity());
        ResourceConfig ovsReservedMemoryConfig = rcf.getResourceConfig(OvsGlobalConfig.RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK.getIdentity());

        if (newValue.equals(oldValue)) {
            // 0. do nothing
            return;
        } else if (newValue.equalsIgnoreCase("false")) {
            // recover reserve memory
            long hostReservedMemory= SizeUtils.sizeStringToBytes(hostReservedMemoryConfig.getResourceConfigValue(resourceUuid, String.class));
            long ovsReservedMemory= SizeUtils.sizeStringToBytes(ovsReservedMemoryConfig.getResourceConfigValue(resourceUuid, String.class));

            long oldReservedMemory = hostReservedMemory - ovsReservedMemory;
            hostReservedMemoryConfig.updateValue(resourceUuid, String.valueOf(SizeUnit.BYTE.toGigaByte(oldReservedMemory))+"G");

            // 1. set MEMORY_ACCESS to private
            memAccessConfig.updateValue(resourceUuid, "private");
            // 2. close cluster numa
            numaConfig.deleteValue(resourceUuid);
            // 3. keep hugepage status
        } else if (newValue.equalsIgnoreCase("true")) {
            long hostReservedMemory= SizeUtils.sizeStringToBytes(hostReservedMemoryConfig.getResourceConfigValue(resourceUuid, String.class));
            long ovsReservedMemory= SizeUtils.sizeStringToBytes(ovsReservedMemoryConfig.getResourceConfigValue(resourceUuid, String.class));

            long oldReservedMemory = hostReservedMemory + ovsReservedMemory;
            hostReservedMemoryConfig.updateValue(resourceUuid, String.valueOf(SizeUnit.BYTE.toGigaByte(oldReservedMemory))+"G");

            // 1. set MEMORY_ACCESS to shared
            memAccessConfig.updateValue(resourceUuid, "shared");
            // 2. open cluster numa
            numaConfig.updateValue(resourceUuid, "true");
        }
    }

    private void updateHugePageState(ResourceConfig config, String resourceUuid, String newValue, String oldValue) {
        if ("true".equals(newValue)) {
            checkClusterReserveMem(resourceUuid);
        }

        List<String> ips = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, resourceUuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();
        if (ips.isEmpty()) {
            return;
        }
        List<ChangeHugePageStateMsg> msgs = new ArrayList<ChangeHugePageStateMsg>();
        for (String ip : ips) {
            ChangeHugePageStateMsg msg = new ChangeHugePageStateMsg();
            msg.setHostUuid(ip);
            msg.setValue(newValue);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, ip);
            msgs.add(msg);
        }
        FutureCompletion completion = new FutureCompletion(null);
        ErrorCodeList errList = new ErrorCodeList();
        new While<>(msgs).all((msg, complet) -> {
            bus.send(msg, new CloudBusCallBack(complet) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        logger.warn(String.format("failed to change huge page state on host:%s", msg.getHostUuid()));
                    }
                    complet.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    private void initValidateExtension() {
        ResourceConfigValidatorExtensionPoint validatorExtPoint = new ResourceConfigValidatorExtensionPoint() {
            @Override
            public void validateResourceConfig(String resourceUuid, String oldValue, String newValue) throws GlobalConfigException {
                List<String> ips = Q.New(HostVO.class)
                        .select(HostVO_.uuid)
                        .eq(HostVO_.clusterUuid, resourceUuid)
                        .notEq(HostVO_.state, HostState.Maintenance)
                        .listValues();
                if (ips.isEmpty()) {
                    return;
                }
                throw new GlobalConfigException(String.format("hosts:%s are still not entering maintenance mode", ips));
            }
        };

        ResourceConfig config = rcf.getResourceConfig(MevocoClusterGlobalConfig.HUGEPAGE_ENABLE.getIdentity());
        config.installValidatorExtension(validatorExtPoint);

        ResourceConfig memAccessConfig = rcf.getResourceConfig(MevocoClusterGlobalConfig.MEMORY_ACCESS.getIdentity());
        memAccessConfig.installValidatorExtension(validatorExtPoint);

        ResourceConfig ovsDpdkSupConfig = rcf.getResourceConfig(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT.getIdentity());
        ovsDpdkSupConfig.installValidatorExtension(validatorExtPoint);

        ResourceConfig reservedMemConfig = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getIdentity());
        //todo: if kvm reserve mem less than ovs reserved
        OvsGlobalConfig.RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(value)) {
                    throw new GlobalConfigException(String.format("%s only allows a size string." +
                                    " A size string is a number with suffix 'T/t/G/g/M/m/K/k/B/b' or without suffix, but got %s",
                            OvsGlobalConfig.RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK.getCanonicalName(), value));
                }
            }
        });

        reservedMemConfig.installValidatorExtension(new ResourceConfigValidatorExtensionPoint(){
            @Override
            public void validateResourceConfig(String resourceUuid, String oldValue, String newValue) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(newValue)) {
                    return;
                }
                boolean isOvsSup= false;
                String type = Q.New(ResourceVO.class).eq(ResourceVO_.uuid, resourceUuid).select(ResourceVO_.resourceType).findValue();
                if (ClusterVO.class.getSimpleName().equals(type)) {
                    isOvsSup = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT, resourceUuid, Boolean.class);
                } else if (HostVO.class.getSimpleName().equals(type)) {
                    String clusterUuid = Q.New(HostVO.class).eq(HostVO_.uuid, resourceUuid).select(HostVO_.clusterUuid).findValue();
                    isOvsSup = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT, clusterUuid, Boolean.class);
                }

                if (!isOvsSup) {
                    return;
                }

                String ovsReservdMem = rcf.getResourceConfigValue(OvsGlobalConfig.RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK, resourceUuid, String.class);
                long reservdMem = SizeUtils.sizeStringToBytes(newValue);
                long reservdOvsMem = SizeUtils.sizeStringToBytes(ovsReservdMem);
                if (reservdOvsMem > reservdMem) {
                    throw new GlobalConfigException(String.format("%s is less than %s", KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getCanonicalName(), OvsGlobalConfig.RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK.getCanonicalName()));
                }
            }
        });

    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (rcf.getResourceConfigValue(MevocoClusterGlobalConfig.HUGEPAGE_ENABLE, host.getClusterUuid(), Boolean.class)) {
            cmd.setUseHugePage(true);
        }

        String memAccess = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.MEMORY_ACCESS, host.getClusterUuid(), String.class);
        cmd.setMemAccess(memAccess);
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void beforeAddHost(HostInventory host, Completion completion) {
        completion.success();
    }

    @Override
    public void afterAddHost(HostInventory host, Completion completion) {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            completion.success();
            return;
        }
        String hostArch = host.getArchitecture();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("check-host-ability-state-%s", host.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-huge-page-state";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        Boolean hugePageEnabled = isHugePageCluster(host.getClusterUuid());
                        if (Boolean.FALSE.equals(hugePageEnabled)) {
                            trigger.next();
                            return;
                        }
                        ChangeHugePageStateMsg msg = new ChangeHugePageStateMsg();
                        msg.setHostUuid(host.getUuid());
                        msg.setValue(hugePageEnabled.toString());
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to change huge page state on host:%s", msg.getHostUuid()));
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-zero-copy-state";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        Boolean zeroCopyEnabled = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.HOST_ENABLE_ZERO_COPY, host.getUuid(), Boolean.class);
                        if (Boolean.FALSE.equals(zeroCopyEnabled)) {
                            trigger.next();
                            return;
                        }
                        ChangeZeroCopyStateMsg msg = new ChangeZeroCopyStateMsg();
                        msg.setHostUuid(host.getUuid());
                        msg.setValue(zeroCopyEnabled.toString());
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to change zero copy state on host:%s", msg.getHostUuid()));
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "set-iommu-status-to-active-if-needed";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        //IOMMU in bypass mode by default on ARM64
                        if (!CpuArchitecture.aarch64.toString().equals(hostArch)){
                            trigger.next();
                            return;
                        }

                        SystemTagCreator creator = PciDeviceSystemTags.HOST_IOMMU_STATE.newSystemTagCreator(host.getUuid());
                        creator.ignoreIfExisting = false;
                        creator.inherent = false;
                        creator.setTagByTokens(
                                map(
                                        e(PciDeviceSystemTags.HOST_IOMMU_STATE_TOKEN, PciDeviceState.Enabled.toString())
                                )
                        );
                        creator.create();
                        SyncPciDeviceMsg msg = new SyncPciDeviceMsg();
                        msg.setHostUuid(host.getUuid());
                        msg.setHostIommuState(HostIommuStateType.Enabled.toString());
                        msg.setSkipGrubConfig(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, PciDeviceConstants.SERVICE_ID, host.getUuid());
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to sync pciDeviceInfo on host:%s", msg.getHostUuid()));
                                }
                                trigger.next();
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

    private Boolean isHugePageCluster(String clusterUuid) {
        return rcf.getResourceConfigValue(MevocoClusterGlobalConfig.HUGEPAGE_ENABLE, clusterUuid, Boolean.class);
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        Boolean hugePageEnabled = isHugePageCluster(inventory.getClusterUuid());

        if (!hugePageEnabled) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        ChangeHugePageStateMsg msg = new ChangeHugePageStateMsg();
        msg.setHostUuid(inventory.getUuid());
        msg.setValue(Boolean.FALSE.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, inventory.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to change hugepage state on host:%s", msg.getHostUuid()));
                }
                completion.success();
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(60));
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }

        Boolean zeroCopyEnabled = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.HOST_ENABLE_ZERO_COPY, host.getUuid(), Boolean.class);
        ChangeZeroCopyStateMsg msg = new ChangeZeroCopyStateMsg();
        msg.setHostUuid(host.getUuid());
        msg.setValue(zeroCopyEnabled.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to change zero copy state on host:%s", msg.getHostUuid()));
                }
            }
        });
    }
}
