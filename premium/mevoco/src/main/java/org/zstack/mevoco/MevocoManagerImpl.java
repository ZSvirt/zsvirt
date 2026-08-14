package org.zstack.mevoco;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.bootstrap.BootstrapConstants;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.compute.cluster.MiniClusterSystemTags;
import org.zstack.compute.host.HostManager;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.vm.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginDSL.PluginDefinition;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.thread.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.expon.ExponConstants;
import org.zstack.ha.*;
import org.zstack.header.APIIsOpensourceVersionMsg;
import org.zstack.header.APIIsOpensourceVersionReply;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.bootstrap.*;
import org.zstack.header.cluster.*;
import org.zstack.header.configuration.APICreateInstanceOfferingMsg;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.identity.ForceLogoutSessionExtensionPoint;
import org.zstack.header.identity.IdentityCanonicalEvents;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.managementnode.*;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.securityLevel.SecurityLevel;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.APICreateVolumesSnapshotMsg;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.ChangeVmPasswordMsg;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.*;
import org.zstack.header.volume.block.*;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.image.ImageQuotaConstant;
import org.zstack.image.MevocoImageFactory;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.hypervisor.events.ClusterHostHypervisorMismatchData;
import org.zstack.mevoco.KVMAddOns.NicQos;
import org.zstack.nas.*;
import org.zstack.pciDevice.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigValidatorExtensionPoint;
import org.zstack.scheduler.*;
import org.zstack.sns.SNSConstants;
import org.zstack.sns.SNSPublishMsg;
import org.zstack.sns.platform.http.SNSSystemHttpEndpointFactory;
import org.zstack.sns.system.SNSSystemAlarmTopicManager;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.migration.StorageMigrationBase;
import org.zstack.storage.migration.StorageMigrationMessage;
import org.zstack.storage.migration.primary.*;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.storage.primary.local.LocalStorageCanonicalEvents;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVO_;
import org.zstack.storage.primary.preallocation.PreallocationFactory;
import org.zstack.storage.snapshot.VolumeSnapshotQuotaConstant;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupOperationValidator;
import org.zstack.storage.volume.MevocoVolumeSystemTags;
import org.zstack.storage.volume.VolumeBase;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.storage.volume.block.BlockVolumeFactory;
import org.zstack.storage.volume.block.BlockVolumeMessage;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.data.NumberUtils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.vrouterRoute.*;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.zstack.compute.host.HostSystemTags.SYSTEM_SERIAL_NUMBER;
import static org.zstack.compute.host.HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN;
import static org.zstack.core.Platform.*;
import static org.zstack.kvm.KVMGlobalConfig.LIBVIRT_CACHE_MODE;
import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by frank on 9/16/2015.
 */
public class MevocoManagerImpl extends AbstractService implements MevocoManager, AddImageExtensionPoint,
        HostAfterConnectedExtensionPoint, KVMStartVmExtensionPoint, GlobalApiMessageInterceptor,
        KVMAttachVolumeExtensionPoint, ChangeInstanceOfferingExtensionPoint,
        KvmPreAttachNicExtensionPoint, ReportQuotaExtensionPoint, KvmHostAgentDeploymentExtensionPoint, KVMHostConnectExtensionPoint,
        VmAttachVolumeExtensionPoint, BeforeGetNextVolumeDeviceIdExtensionPoint, VmDetachVolumeExtensionPoint,
        KVMDetachVolumeExtensionPoint, BuildVolumeSpecExtensionPoint, VmReleaseResourceExtensionPoint,
        MarshalReplyMessageExtensionPoint, PreInstantiateVolumeExtensionPoint, CreateDataVolumeExtensionPoint,
        KVMBeforeAsyncJsonPostExtensionPoint, KVMPreUpdateNicExtensionPoint,
        KVMHostAddSshFileMd5CheckerExtensionPoint, GetVmUuidFromShareableVolumeExtensionPoint,
        ForceLogoutSessionExtensionPoint, CreateQcow2VolumeProvisioningStrategyExtensionPoint,
        ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    private HostCapacityOverProvisioningManager hostRatioMgr;
    @Autowired
    private PrimaryStoragePhysicalCapacityManager psPhysicalCapacityMgr;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private HostAllocatorManager hostAllocatorMgr;
    @Autowired
    MevocoVmInstanceBaseFactory factory;
    @Autowired
    MevocoVolumeFactoryImpl volumeFactory;
    @Autowired
    MevocoImageFactory imageFactory;
    @Autowired
    MevocoManagerImpl impl;
    @Autowired
    StorageMigrationBase storageMigrationService;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceManager vmMgr;
    @Autowired
    private HostManager hostManager;

    private List<String> supportSharedVolumePrimaryStorage;
    private List<QemuConfigItemOperator> qemuConfigItems = new ArrayList<>();
    private Future<Void> clusterHypervisorPeriodicTask;

    private Map<String, PreallocationFactory> preallocationFactories = Collections.synchronizedMap(new HashMap<>());
    private Map<String, PremiumVmInstanceFactory> premiumVmInstanceFactories = Collections.synchronizedMap(new HashMap<>());

    // handle block volume apis
    private Map<String, BlockVolumeFactory> blockVolumeFactories = Collections.synchronizedMap(new HashMap<>());

    public List<String> getSupportSharedVolumePrimaryStorage() {
        return supportSharedVolumePrimaryStorage;
    }

    public void setSupportSharedVolumePrimaryStorage(List<String> supportSharedVolumePrimaryStorage) {
        this.supportSharedVolumePrimaryStorage = supportSharedVolumePrimaryStorage;
    }

    public BlockVolumeFactory getBlockVolumeFactory(String type) {
        return blockVolumeFactories.get(type);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {
        if (!volume.isShareable()) {
            return;
        }
        VolumeTO to = cmd.getVolume();
        Integer deviceId = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .select(ShareableVolumeVmInstanceRefVO_.deviceId)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vm.getUuid())
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, volume.getUuid())
                .findValue();
        to.setDeviceId(deviceId);
        cmd.setVolume(to);
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {
    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public List<VolumeInventory> supplyAdditionalVolumesForVmInstance(String VmInstanceUuid) {
        List<Tuple> ts = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .select(ShareableVolumeVmInstanceRefVO_.volumeUuid, ShareableVolumeVmInstanceRefVO_.deviceId)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, VmInstanceUuid)
                .orderBy(ShareableVolumeVmInstanceRefVO_.volumeUuid, SimpleQuery.Od.ASC)
                .listTuple();
        if (ts == null || ts.isEmpty()) {
            return new ArrayList<>();
        }
        List<VolumeVO> voList = Q.New(VolumeVO.class)
                .in(VolumeVO_.uuid, ts.stream().map(p -> p.get(0, String.class)).collect(Collectors.toList()))
                .eq(VolumeVO_.isShareable, true)
                .orderBy(VolumeVO_.uuid, SimpleQuery.Od.ASC)
                .list();
        List<VolumeInventory> invList = VolumeInventory.valueOf(voList);

        IntStream.range(0, invList.size()).forEach(i -> invList.get(i).setDeviceId(ts.get(i).get(1, Integer.class)));

        return invList;
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        VmInstanceInventory inv = spec.getVmInventory();
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.Destroy) {
            completion.success();
            return;
        }
        impl.detachShareableDataVolumeOnVmDestroyed(inv.getUuid());
        completion.success();
    }

    @Transactional
    public void detachShareableDataVolumeOnVmDestroyed(String vmInstanceUuid) {
        String sql = "delete from ShareableVolumeVmInstanceRefVO ref" +
                " where ref.vmInstanceUuid = :vmUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("vmUuid", vmInstanceUuid);
        q.executeUpdate();
    }

    @Override
    public void preInstantiateVolume(InstantiateVolumeMsg msg) {
        VolumeVO volumeVO = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        // expon block volume can be shareable
        if (Q.New(ExponBlockVolumeVO.class)
                .eq(ExponBlockVolumeVO_.uuid, volumeVO.getUuid()).isExists()) {
            return;
        }
        if (volumeVO.isShareable()) {
            String psType = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.type)
                    .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                    .findValue();
            if (!supportSharedVolumePrimaryStorage.contains(psType)) {
                throw new OperationFailureException(operr("for shareable volume, the only supported primary storage type is %s, current is %s",
                        supportSharedVolumePrimaryStorage, psType));
            }
        }
    }

    final private List<Class> premiumAPIs = new ArrayList<>();
    final private Set<String> premiumAPIPackages = new HashSet<>();
    final private HashMap<String, String> addOnAPIPackages = new HashMap<>();

    final private Set<String> blockZSVBasicPackages = new HashSet<>();
    final private Set<String> blockZSVProPackages = new HashSet<>();
    final private Set<String> blockZSVAdvancedPackages = new HashSet<>();

    {
        PluginDefinition definition = new PluginDefinition(MevocoManagerImpl.class);
        definition.newExtension().extensionClass(GlobalApiMessageInterceptor.class);
    }

    public boolean start() {
        populateExtensions();
        if(MevocoGlobalConfig.ENABLE_SPICE_CHANNEL_SUPPORT_TLS.value(Integer.class) > 0){
            configSpiceCertificates();
        }
        configureGlobalConfig();
        setupOverProvisioningRatio();
        configureSystemTags();
        installSystemTagValidator();
        return true;
    }

    @Override
    public void managementNodeReady() {
        installClusterHypervisorConfigListener();
    }

    private void populateExtensions() {
        for (PreallocationFactory f : pluginRgty.getExtensionList(PreallocationFactory.class)) {
            PreallocationFactory old = preallocationFactories.get(f.getPrimaryStorageType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PreallocationFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getPrimaryStorageType()));
            }
            preallocationFactories.put(f.getPrimaryStorageType(), f);
        }

        for (PremiumVmInstanceFactory f : pluginRgty.getExtensionList(PremiumVmInstanceFactory.class)) {
            PremiumVmInstanceFactory old = premiumVmInstanceFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PreallocationFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getType()));
            }
            premiumVmInstanceFactories.put(f.getType().toString(), f);
        }

        for (BlockVolumeFactory f : pluginRgty.getExtensionList(BlockVolumeFactory.class)) {
            BlockVolumeFactory old = blockVolumeFactories.get(f.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BlockVolumeFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getType()));
            }
            blockVolumeFactories.put(f.getType(), f);
        }

        qemuConfigItems.addAll(pluginRgty.getExtensionList(QemuConfigItemOperator.class));
    }

    private void installSystemTagValidator() {
        installCustomMacValidator();
    }

    private void installCustomMacValidator() {
        class CustomMacValidator implements SystemTagCreateMessageValidator {
            private void check(String systemTag) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.CUSTOM_MAC.getTagFormat(), systemTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new OperationFailureException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of static IP",
                            l3Uuid, systemTag));
                }
                token = TagUtils.parse(VmSystemTags.CUSTOM_MAC.getTagFormat(), systemTag);
                String mac = token.get(VmSystemTags.MAC_TOKEN);
                new MacOperator().validateAvailableMac(mac);
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                for (String sysTag : msg.getSystemTags()) {
                    if (VmSystemTags.CUSTOM_MAC.isMatch(sysTag)) {
                        check(sysTag);
                    }
                }
            }
        }

        CustomMacValidator customMacValidator = new CustomMacValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), customMacValidator);
    }

    private void setupOverProvisioningRatio() {
        ratioMgr.setGlobalConfig(MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.getCategory(),
                MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.getName());
        hostRatioMgr.setGlobalConfig(MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.getCategory(),
                MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.getName());
        psPhysicalCapacityMgr.setGlobalConfig(MevocoGlobalConfig.PRIMARY_STORAGE_PHYSICAL_CAPACITY_THRESHOLD.getCategory(), MevocoGlobalConfig.PRIMARY_STORAGE_PHYSICAL_CAPACITY_THRESHOLD.getName());
    }

    private void configureSystemTags() {
        MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String bandwidth = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByTag(systemTag,
                        MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
                try {
                    long val = new BigInteger(bandwidth).longValueExact();
                    if (val < 8192 && val != -1) {
                        throw new OperationFailureException(argerr("invalid network bandwidth[%s], it must be greater than or equal to 8192", bandwidth));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid network bandwidth[%s], it is not a number", bandwidth));
                } catch (ArithmeticException e) {
                    throw new OperationFailureException(argerr("invalid volume bandwidth[%s] is larger than %d", bandwidth, Long.MAX_VALUE));
                }
            }
        });

        MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String bandwidth = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByTag(systemTag,
                        MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
                try {
                    long val = new BigInteger(bandwidth).longValueExact();
                    if (val < 8192 && val != -1) {
                        throw new OperationFailureException(argerr("invalid network bandwidth[%s], it must be greater than or equal to 8192", bandwidth));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid network bandwidth[%s], it is not a number", bandwidth));
                } catch (ArithmeticException e) {
                    throw new OperationFailureException(argerr("invalid volume bandwidth[%s] is larger than %d", bandwidth, Long.MAX_VALUE));
                }
            }
        });

        for (Map.Entry<String, PatternedSystemTag> entry : MevocoSystemTags._volumeSystemTagMap.entrySet()) {
            String patternedSystemTagToken = entry.getKey();
            PatternedSystemTag patternedSystemTag = entry.getValue();
            patternedSystemTag.installValidator(new SystemTagValidator() {
                @Override
                public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                    String bandwidth = patternedSystemTag.getTokenByTag(systemTag, patternedSystemTagToken);
                    try {
                        long val = new BigInteger(bandwidth).longValueExact();
                        if (val < 1024 && val != -1) {
                            throw new OperationFailureException(argerr("invalid volume bandwidth[%s], it must be greater than 1024 (include 1024)", bandwidth));
                        }
                    } catch (NumberFormatException e) {
                        throw new OperationFailureException(argerr("invalid volume bandwidth[%s] is not a number", bandwidth));
                    } catch (ArithmeticException e) {
                        throw new OperationFailureException(argerr("invalid volume bandwidth[%s] is larger than %d", bandwidth, Long.MAX_VALUE));
                    }
                }
            });
        }

        for (Map.Entry<String, PatternedSystemTag> entry : MevocoVolumeSystemTags.volumeSystemTagMap.entrySet()) {
            String patternedSystemTagToken = entry.getKey();
            PatternedSystemTag patternedSystemTag = entry.getValue();
            patternedSystemTag.installValidator(new SystemTagValidator() {
                @Override
                public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                    String bandwidth = patternedSystemTag.getTokenByTag(systemTag, patternedSystemTagToken);
                    try {
                        long val = new BigInteger(bandwidth).longValueExact();
                        if (val < 1024 && val != -1) {
                            throw new OperationFailureException(argerr("invalid volume bandwidth[%s], it must be greater than 1024 (include 1024)", bandwidth));
                        }
                    } catch (NumberFormatException e) {
                        throw new OperationFailureException(argerr("invalid volume bandwidth[%s] is not a number", bandwidth));
                    } catch (ArithmeticException e) {
                        throw new OperationFailureException(argerr("invalid volume bandwidth[%s] is larger than %d", bandwidth, Long.MAX_VALUE));
                    }
                }
            });
        }

        for (Map.Entry<String, PatternedSystemTag> entry : MevocoSystemTags._iopsSystemTagMap.entrySet()) {
            String token = entry.getKey();
            PatternedSystemTag tag = entry.getValue();
            tag.installValidator(((resourceUuid, resourceType, systemTag) -> {
                String iops = tag.getTokenByTag(systemTag, token);
                try {
                    long val = new BigInteger(iops).longValueExact();
                    if (val < 1 && val != -1) {
                        throw new OperationFailureException(argerr("invalid volume IOPS[%s], it must be greater than 1 (include 1)", iops));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid volume IOPS[%s] is not a number", iops));
                } catch (ArithmeticException e) {
                    throw new OperationFailureException(argerr("invalid volume IOPS[%s] is larger than %d", iops, Long.MAX_VALUE));
                }
            }));
        }

        for (Map.Entry<String, PatternedSystemTag> entry : MevocoVolumeSystemTags.iopsSystemTagMap.entrySet()) {
            String token = entry.getKey();
            PatternedSystemTag tag = entry.getValue();
            tag.installValidator((resourceUuid, resourceType, systemTag) -> {
                String iops = tag.getTokenByTag(systemTag, token);
                try {
                    long val = new BigInteger(iops).longValueExact();
                    if (val < 1 && val != -1) {
                        throw new OperationFailureException(argerr("invalid volume IOPS[%s], it must be greater than 1 (include 1)", iops));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid volume IOPS[%s] is not a number", iops));
                } catch (ArithmeticException e) {
                    throw new OperationFailureException(argerr("invalid volume IOPS[%s] is larger than %d", iops, Long.MAX_VALUE));
                }
            });
        }

        VmSystemTags.CUSTOM_MAC.installValidator((resourceUuid, resourceType, systemTag) -> {
            Map<String, String> token = TagUtils.parse(VmSystemTags.CUSTOM_MAC.getTagFormat(), systemTag);
            String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                throw new OperationFailureException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of static IP",
                        l3Uuid, systemTag));
            }
            token = TagUtils.parse(VmSystemTags.CUSTOM_MAC.getTagFormat(), systemTag);
            String mac = token.get(VmSystemTags.MAC_TOKEN);
            new MacOperator().validateAvailableMac(mac);
        });

        MevocoVmSystemTags.SECURITY_LEVEL.installValidator((resourceUuid, resourceType, systemTag) -> {
            Map<String, String> token = TagUtils.parse(MevocoVmSystemTags.SECURITY_LEVEL.getTagFormat(), systemTag);
            String level = token.get(MevocoVmSystemTags.SECURITY_LEVEL_TOKEN);

            SecurityLevel sl = SecurityLevel.fromCode(level);

            if (sl == null) {
                throw new OperationFailureException(operr("Unknown code[%s] of Security Level", level));
            }
        });

        MevocoImageSystemTags.SECURITY_LEVEL.installValidator((resourceUuid, resourceType, systemTag) -> {
            Map<String, String> token = TagUtils.parse(MevocoVmSystemTags.SECURITY_LEVEL.getTagFormat(), systemTag);
            String level = token.get(MevocoVmSystemTags.SECURITY_LEVEL_TOKEN);

            SecurityLevel sl = SecurityLevel.fromCode(level);

            if (sl == null) {
                throw new OperationFailureException(operr("Unknown code[%s] of Security Level", level));
            }
        });

        MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.installValidator((resourceUuid, resourceType, systemTag) -> {
            String cidr = MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTokenByTag(systemTag,
                    MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN);
            try {
                String fmtCidr = NetworkUtils.fmtCidr(cidr);
                if (!fmtCidr.equals(cidr)) {//NOTE(siying): NetworkUtils.fmtCidr(cidr) will throw a RuntimeException when input is an illegal CIDR, convert exception to ApiMessageInterceptionException
                    throw new ApiMessageInterceptionException(argerr("[%s] is not a standard cidr, do you mean [%s]?", cidr, fmtCidr));
                }
            } catch (ApiMessageInterceptionException apiException) {
                throw apiException;
            } catch (RuntimeException e) {
                        ApiMessageInterceptionException apiMesException = new ApiMessageInterceptionException(argerr("[%s] is not a standard cidr", cidr));
                        apiMesException.initCause(e);
                        throw apiMesException;
            }
        });
    }

    public void exceptionIfQosNotSupportedOnKvm(String hostUuid) {
        String hypervisorType = Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .select(HostVO_.hypervisorType)
                .findValue();
        final HostOperationSystem os = hostManager.getHypervisorFactory(HypervisorType.valueOf(hypervisorType))
                .getHostOS(hostUuid);
        if (!"CentOS".equalsIgnoreCase(os.distribution) && !"RedHat".equalsIgnoreCase(os.distribution)) {
            return;
        }

        if ("6.5".compareTo(os.version) >= 0) {
            throw new OperationFailureException(operr("the host[uuid:%s]'s operating system %s %s is too old, the QEMU doesn't support QoS of network" +
                            " or disk IO. Please choose another instance offering with no QoS configuration", hostUuid, os.distribution, os.version));
        }
    }


    private void recalculateAllPrimaryStorageCapacity() {
        SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
        q.select(ZoneVO_.uuid);
        List<String> uuids = q.listValue();
        uuids.forEach(it -> recalculatePrimaryStorageCapacity(it, ZoneVO.class.getSimpleName()));
    }

    private void recalculatePrimaryStorageCapacity(String resourceUuid, String resourceType) {
        RecalculatePrimaryStorageCapacityMsg msg = new RecalculatePrimaryStorageCapacityMsg();
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, resourceUuid);
        if (resourceType.equals(ZoneVO.class.getSimpleName())) {
            msg.setZoneUuid(resourceUuid);
        } else if (resourceType.equals(PrimaryStorageVO.class.getSimpleName())) {
            msg.setPrimaryStorageUuid(resourceUuid);
        }
        bus.send(msg);
    }

    private void configureGlobalConfig() {
        configureMemoryOverProvisioningRatio();
        configurePsOverProvisioningRatio();
        configurePsPhysicalCapacityThreshold();
        configureSpiceChannelTls();
        configureCgroupDeviceAcl();
        configureRestConfig();
        configureSecurityLevelConfig();
        configureConsolePasswordConfig();
        configureVmPasswordConfig();
        configureVmDomainPasswordConfig();
    }

    private void configureVmPasswordConfig() {
        MevocoGlobalConfig.VM_PASSWORD_STRENGTH_CHECK_CONFIG.installValidateExtension((category, name, oldValue, newValue) -> {
            VmPasswordStrengthConfig.validatePasswordStrengthConfigStr(newValue);
        });
    }

    private void configureConsolePasswordConfig() {
        MevocoGlobalConfig.VM_CONSOLE_PASSWORD_STRENGTH_CHECK_CONFIG.installValidateExtension((category, name, oldValue, newValue) -> {
            VmPasswordStrengthConfig.validatePasswordStrengthConfigStr(newValue);
        });
    }

    private void configureVmDomainPasswordConfig() {
        MevocoGlobalConfig.VM_DOMAIN_PASSWORD_STRENGTH_CHECK_CONFIG.installValidateExtension((category, name, oldValue, newValue) -> {
            VmPasswordStrengthConfig.validatePasswordStrengthConfigStr(newValue);
        });
    }

    private void configureSecurityLevelConfig() {
        MevocoGlobalConfig.ENABLE_SECURITY_LEVEL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                // disable security level need to clean up all the levels
                if (!newConfig.value(Boolean.class)) {
                    SQL.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName()).like(SystemTagVO_.tag, String.format("%%%s%%", MevocoVmSystemTags.SECURITY_LEVEL_TOKEN)).hardDelete();
                    SQL.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, ImageVO.class.getSimpleName()).like(SystemTagVO_.tag,String.format("%%%s%%", MevocoImageSystemTags.SECURITY_LEVEL_TOKEN)).hardDelete();
                }
             }
        });
    }

    private void configureSpiceChannelTls() {
        MevocoGlobalConfig.ENABLE_SPICE_CHANNEL_SUPPORT_TLS.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.value(Boolean.class)) {
                    throw new GlobalConfigException("The global config [reconnect.host.restart.libvirtd.service] must be" +
                            " set to true when enabling spice channel support tls");
                }
            }
        });

        MevocoGlobalConfig.ENABLE_SPICE_CHANNEL_SUPPORT_TLS.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (newConfig.value(Integer.class) > 0) {
                    configSpiceCertificates();
                }
            }
        });
    }

    private void configSpiceCertificates() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }
        //create spice Certificates
        String kvmAnsibleDir = PathUtil.join(PathUtil.getZStackHomeFolder(), "ansible", "files", "kvm");

        try {
            File dst = new File(kvmAnsibleDir);
            if (!dst.exists()) {
                dst.mkdirs();
            }

            File caFile = new File(kvmAnsibleDir + "/spice-certs/ca-cert.pem");
            if (!caFile.exists()) {
                File scriptPath = PathUtil.findFileOnClassPath("scripts/spice-tls.sh");
                ShellUtils.run("bash " + scriptPath.getAbsolutePath(), kvmAnsibleDir, false);
                caFile = new File(kvmAnsibleDir + "/spice-certs/ca-cert.pem");
            }
            String ca = FileUtils.readFileToString(caFile);
            ca = ca.trim();
            ca = StringDSL.stripEnd(ca, "\n");

            File caKeyFile = new File(kvmAnsibleDir + "/spice-certs/ca-key.pem");
            String privateKey = FileUtils.readFileToString(caKeyFile);
            privateKey = privateKey.trim();
            privateKey = StringDSL.stripEnd(privateKey, "\n");

            File serverFile = new File(kvmAnsibleDir + "/spice-certs/server-cert.pem");
            String server = FileUtils.readFileToString(serverFile);
            server = server.trim();
            server = StringDSL.stripEnd(server, "\n");

            File serverKeyFile = new File(kvmAnsibleDir + "/spice-certs/server-key.pem");
            String serverKey = FileUtils.readFileToString(serverKeyFile);
            serverKey = serverKey.trim();
            serverKey = StringDSL.stripEnd(serverKey, "\n");

            JsonLabelInventory spiceCAInventory = new JsonLabel().createIfAbsent("spiceCA", ca);
            JsonLabelInventory spiceCAKeyInventory = new JsonLabel().createIfAbsent("spiceCAKey", privateKey);
            JsonLabelInventory spiceServerInventory = new JsonLabel().createIfAbsent("spiceServer", server);
            JsonLabelInventory spiceServerKeyInventory = new JsonLabel().createIfAbsent("spiceServerKey", serverKey);

            //old version didn't generate key in database, we will write back key for HA environment
            FileUtils.writeStringToFile(caFile, spiceCAInventory.getLabelValue());
            ShellUtils.run(String.format("chmod 600 %s", caKeyFile.getAbsolutePath()));
            FileUtils.writeStringToFile(caKeyFile, spiceCAKeyInventory.getLabelValue());
            ShellUtils.run(String.format("chmod 400 %s", caKeyFile.getAbsolutePath()));
            FileUtils.writeStringToFile(serverFile, spiceServerInventory.getLabelValue());
            ShellUtils.run(String.format("chmod 600 %s", serverKeyFile.getAbsolutePath()));
            FileUtils.writeStringToFile(serverKeyFile, spiceServerKeyInventory.getLabelValue());
            ShellUtils.run(String.format("chmod 400 %s", serverKeyFile.getAbsolutePath()));
        } catch (Exception e) {
            logger.warn("failed to create directory: " + kvmAnsibleDir, e);
        }
    }

    private void configureCgroupDeviceAcl() {
        MevocoGlobalConfig.ENABLE_CGROUP_DEVICE_ACL.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.value(Boolean.class)) {
                    throw new GlobalConfigException("The global config [reconnect.host.restart.libvirtd.service] must be" +
                            " set to true when enabling cgroup device acl");
                }
            }
        });
    }

    private void configureMemoryOverProvisioningRatio() {
        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(final GlobalConfig oldConfig, final GlobalConfig newConfig) {
                hostRatioMgr.setMemoryGlobalRatio(newConfig.value(Double.class));

                SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
                q.select(ZoneVO_.uuid);
                List<String> uuids = q.listValue();
                uuids.forEach(it -> recalculateHostCapacity(it, ZoneVO.class.getSimpleName()));
            }
        });

        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(final GlobalConfig oldConfig, final GlobalConfig newConfig) {
                hostRatioMgr.setMemoryGlobalRatio(newConfig.value(Double.class));
            }
        });

        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                try {
                    double val = Double.parseDouble(newValue);
                    if (val <= 0) {
                        throw new OperationFailureException(argerr("invalid value[%s], it must be a double greater than 0", newValue));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid value[%s], it's not a double", newValue));
                }
            }
        });

        ResourceConfig ratioConfig = rcf.getResourceConfig(MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.getIdentity());
        ratioConfig.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
        ratioConfig.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
    }

    private void recalculateHostCapacity(String resourceUuid, String resourceType) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
        bus.makeTargetServiceIdByResourceUuid(msg, HostAllocatorConstant.SERVICE_ID, resourceUuid);
        if (resourceType.equals(ZoneVO.class.getSimpleName())) {
            msg.setZoneUuid(resourceUuid);
        } else if (resourceType.equals(ClusterVO.class.getSimpleName())) {
            msg.setClusterUuid(resourceUuid);
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            msg.setHostUuid(resourceUuid);
        }
        bus.send(msg);
    }

    private void configurePsOverProvisioningRatio() {
        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(final GlobalConfig oldConfig, final GlobalConfig newConfig) {
                double oldGlobalRatio = ratioMgr.getGlobalRatio();
                double newGlobalRatio = newConfig.value(Double.class);
                ratioMgr.setGlobalRatio(newGlobalRatio);

                if (oldGlobalRatio == newGlobalRatio) {
                    return;
                }
                recalculateAllPrimaryStorageCapacity();
            }
        });

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(final GlobalConfig oldConfig, final GlobalConfig newConfig) {
                ratioMgr.setGlobalRatio(newConfig.value(Double.class));
                recalculateAllPrimaryStorageCapacity();
            }
        });

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                try {
                    double val = Double.parseDouble(newValue);
                    if (val <= 0) {
                        throw new OperationFailureException(argerr("invalid value[%s], it must be a double greater than 0", newValue));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid value[%s], it's not a double", newValue));
                }
            }
        });

        ResourceConfig ratioConfig = rcf.getResourceConfig(MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.getIdentity());
        ratioConfig.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                recalculatePrimaryStorageCapacity(resourceUuid, resourceType));
        ratioConfig.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) ->
                recalculatePrimaryStorageCapacity(resourceUuid, resourceType));
    }

    private void configurePsPhysicalCapacityThreshold() {
        MevocoGlobalConfig.PRIMARY_STORAGE_PHYSICAL_CAPACITY_THRESHOLD.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                psPhysicalCapacityMgr.setGlobalRatio(newConfig.value(Double.class));
            }
        });

        MevocoGlobalConfig.PRIMARY_STORAGE_PHYSICAL_CAPACITY_THRESHOLD.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                try {
                    double val = Double.parseDouble(newValue);
                    if (val <= 0 || val > 1) {
                        throw new OperationFailureException(argerr("invalid value[%s], it must be a double between (0, 1]", newValue));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid value[%s], it's not a double", newValue));
                }
            }
        });
    }

    private void configureRestConfig() {
        MevocoGlobalConfig.HOST_ALLOCATOR_STRATEGY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!HostAllocatorStrategyType.hasType(newValue)) {
                    throw new OperationFailureException(argerr("invalid value[%s], ZStack doesn't have such host allocator type", newValue));
                }
            }
        });

        MevocoGlobalConfig.AIO_NATIVE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (newValue.equals("true") && !LIBVIRT_CACHE_MODE.value().equals("none")) {
                    throw new OperationFailureException(argerr("%s value is[%s], which is conflict with %s value [%s]",
                            LIBVIRT_CACHE_MODE.getCanonicalName(), LIBVIRT_CACHE_MODE.value(), MevocoGlobalConfig.AIO_NATIVE.getCanonicalName(), newValue));
                }
            }
        });

        KVMGlobalConfig.LIBVIRT_CACHE_MODE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (MevocoGlobalConfig.AIO_NATIVE.value(Boolean.class) && !newValue.equals("none")) {
                    throw new OperationFailureException(argerr("%s value is[%s], which is conflict with %s value [%s]",
                            MevocoGlobalConfig.AIO_NATIVE.getCanonicalName(), MevocoGlobalConfig.AIO_NATIVE.value(), KVMGlobalConfig.LIBVIRT_CACHE_MODE.getCanonicalName(), KVMGlobalConfig.LIBVIRT_CACHE_MODE.value()));
                }
            }
        });

        ResourceConfig aioConfig = rcf.getResourceConfig(MevocoGlobalConfig.AIO_NATIVE.getIdentity());
        aioConfig.installValidatorExtension(new ResourceConfigValidatorExtensionPoint() {
            @Override
            public void validateResourceConfig(String resourceUuid, String oldValue, String newValue) throws GlobalConfigException {
                String resourceCacheMode = rcf.getResourceConfigValue(LIBVIRT_CACHE_MODE, resourceUuid, String.class);
                if (newValue.equals("true") && !resourceCacheMode.equals("none")) {
                    throw new OperationFailureException(argerr("%s value is[%s], which is conflict with %s value [%s]",
                            LIBVIRT_CACHE_MODE.getCanonicalName(), resourceCacheMode, MevocoGlobalConfig.AIO_NATIVE.getCanonicalName(), newValue));
                }
            }
        });

        ResourceConfig cacheModeConfig = rcf.getResourceConfig(LIBVIRT_CACHE_MODE.getIdentity());
        cacheModeConfig.installValidatorExtension(new ResourceConfigValidatorExtensionPoint() {
            @Override
            public void validateResourceConfig(String resourceUuid, String oldValue, String newValue) throws GlobalConfigException {
                Boolean resourceAioConfig = rcf.getResourceConfigValue(MevocoGlobalConfig.AIO_NATIVE, resourceUuid, Boolean.class);
                if (resourceAioConfig && !newValue.equals("none")) {
                    throw new OperationFailureException(argerr("%s value is[%s], which is conflict with %s value [%s]",
                            MevocoGlobalConfig.AIO_NATIVE.getCanonicalName(), resourceAioConfig, KVMGlobalConfig.LIBVIRT_CACHE_MODE.getCanonicalName(), KVMGlobalConfig.LIBVIRT_CACHE_MODE.value()));
                }
            }
        });

        MevocoGlobalConfig.QCOW2_CLUSTER_SIZE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                int number = Integer.parseInt(newValue);
                if (!NumberUtils.isPowerOf2(number)) {
                    throw new OperationFailureException(argerr("the value[%s] is not power of 2", newValue));
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void validateAddImage(List<String> bsUuids) {

    }

    @Override
    public void preAddImage(ImageInventory img) {
    }

    @Override
    public void beforeAddImage(ImageInventory img) {
    }


    @Override
    @AsyncThread
    public void afterAddImage(final ImageInventory img) {
        if (!MevocoGlobalConfig.DISTRIBUTE_IMAGE.value(Boolean.class)) {
            return;
        }

        downloadImageToLocalStorageCache(img);
    }

    @Override
    public void failedToAddImage(ImageInventory img, ErrorCode err) {
    }

    private boolean checkImageNeedDownloadToLocalStorageCache(ImageInventory img) {
        // if any bs both contains the image and meets the type requirement, the result is true
        List<String> bsTypesNeedCache = hostAllocatorMgr.getBackupStorageTypesByPrimaryStorageTypeFromMetrics(
                LocalStorageConstants.LOCAL_STORAGE_TYPE);
        List<String> bsUuids = img.getBackupStorageRefs()
                .stream()
                .map(ImageBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toList());
        return Q.New(BackupStorageVO.class)
                .in(BackupStorageVO_.uuid, bsUuids)
                .in(BackupStorageVO_.type, bsTypesNeedCache)
                .isExists();
    }

    private void downloadImageToHostCache(final String hostUuid) {
        SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
        q.select(LocalStorageHostRefVO_.primaryStorageUuid);
        q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, hostUuid);
        List<String> localStorageUuids = q.listValue();
        if (localStorageUuids.isEmpty()) {
            return;
        }

        // if any local storage of target host contains target image, the image would be skipped
        List<ImageInventory> images = new Callable<List<ImageInventory>>() {
            @Override
            @Transactional(readOnly = true)
            public List<ImageInventory> call() {
                String sql = "select img" +
                        " from ImageVO img" +
                        " where img.status = :imgStatus" +
                        " and img.mediaType in (:mtypes)" +
                        " and img.uuid not in (select c.imageUuid from ImageCacheVO c where c.installUrl like :url)";
                TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
                q.setParameter("imgStatus", ImageStatus.Ready);
                q.setParameter("url", String.format("%%hostUuid://%s%%", hostUuid));
                q.setParameter("mtypes", list(ImageMediaType.RootVolumeTemplate, ImageMediaType.ISO));
                List<ImageVO> vos = q.getResultList();
                return ImageInventory.valueOf(vos);
            }
        }.call();

        if (images.isEmpty()) {
            return;
        }

        logger.debug(String.format("images[%s] are not found in the cache on the host[uuid:%s], download them",
                images.stream().map(ImageInventory::getName).collect(Collectors.toList()), hostUuid));

        List<DownloadImageToPrimaryStorageCacheMsg> msgs = new ArrayList<>();
        for (ImageInventory img : images) {
            List<String> reachablePsUuids = getReachableLocalStoragePrimaryStorageForCacheOperation(img);
            List<String> intersect = localStorageUuids.stream()
                    .filter(reachablePsUuids::contains)
                    .collect(Collectors.toList());

            for (String priuuid : intersect) {
                if (checkImageNeedDownloadToLocalStorageCache(img)) {
                    DownloadImageToPrimaryStorageCacheMsg msg = new DownloadImageToPrimaryStorageCacheMsg();
                    msg.setImage(img);
                    msg.setPrimaryStorageUuid(priuuid);
                    msg.setHostUuid(hostUuid);
                    msg.setTimeout(TimeUnit.HOURS.toMillis(72));
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priuuid);
                    msgs.add(msg);
                }
            }
        }

        if (!msgs.isEmpty()) {
            bus.send(msgs, MevocoGlobalConfig.DISTRIBUTE_IMAGE_PARA_LEVEL.value(Integer.class), new CloudBusSteppingCallback(null) {
                @Override
                public void run(NeedReplyMessage msg, MessageReply reply) {
                    DownloadImageToPrimaryStorageCacheMsg dmsg = (DownloadImageToPrimaryStorageCacheMsg) msg;
                    if (reply.isSuccess()) {
                        logger.debug(String.format("successfully downloaded the image[uuid:%s] to the host[uuid:%s] of primary storage[uuid:%s]",
                                dmsg.getImage().getUuid(), dmsg.getHostUuid(), dmsg.getPrimaryStorageUuid()));

                        LocalStorageCanonicalEvents.LocalStorageImageDistributedToImageCacheEvent evt = new LocalStorageCanonicalEvents.LocalStorageImageDistributedToImageCacheEvent();
                        evt.image = dmsg.getImage();
                        evt.localStorageUuid = dmsg.getPrimaryStorageUuid();
                        evt.hostUuid = dmsg.getHostUuid();
                        evt.fire();

                    } else {
                        logger.debug(String.format("failed to download the image[uuid:%s] to the host[uuid:%s] of the primary storage[uuid:%s], %s",
                                dmsg.getImage().getUuid(), dmsg.getHostUuid(), dmsg.getPrimaryStorageUuid(), reply.getError()));
                    }
                }
            });
        }
    }

    @Override
    @AsyncThread
    public void afterHostConnected(HostInventory host) {
        if (!MevocoGlobalConfig.DISTRIBUTE_IMAGE.value(Boolean.class)) {
            return;
        }

        downloadImageToHostCache(host.getUuid());
    }

    private List<String> getReachableLocalStoragePrimaryStorageForCacheOperation(final ImageInventory img) {
        List<String> bsUuids = img.getBackupStorageRefs()
                .stream()
                .map(ImageBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toList());

        return SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, BackupStorageZoneRefVO ref" +
                " where pri.zoneUuid = ref.zoneUuid" +
                " and ref.backupStorageUuid in (:bsUuids)" +
                " and pri.state = :pstate" +
                " and pri.status = :pstatus" +
                " and pri.type = :pstype")
                .param("bsUuids", bsUuids)
                .param("pstate", PrimaryStorageState.Enabled)
                .param("pstatus", PrimaryStorageStatus.Connected)
                .param("pstype", LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .list();
    }

    private void downloadImageToLocalStorageCache(final ImageInventory img) {
        if (!checkImageNeedDownloadToLocalStorageCache(img)) {
            return;
        }

        long imageActualSize = img.getActualSize() != null ? img.getActualSize() : 0;

        List<String> priUuids = getReachableLocalStoragePrimaryStorageForCacheOperation(img);
        if (priUuids.isEmpty()) {
            return;
        }

        List<DownloadImageToPrimaryStorageCacheMsg> msgs = new ArrayList<>();
        priUuids.forEach(priUuid -> {
            List<String> hostUuids = SQL.New("select ref.hostUuid" +
                    " from LocalStorageHostRefVO ref, HostVO host, PrimaryStorageVO ps" +
                    " where ref.primaryStorageUuid = :psUuid" +
                    " and host.uuid = ref.hostUuid" +
                    " and host.status = :hostStatus" +
                    " and host.state = :hostState" +
                    " and ref.availablePhysicalCapacity > :size" +
                    " and ps.uuid = ref.primaryStorageUuid" +
                    " and ps.state = :psState" +
                    " and ps.status = :psStatus")
                    .param("psUuid", priUuid)
                    .param("hostStatus", HostStatus.Connected)
                    .param("hostState", HostState.Enabled)
                    .param("size", imageActualSize)
                    .param("psState", PrimaryStorageState.Enabled)
                    .param("psStatus", PrimaryStorageStatus.Connected)
                    .list();

            for (String hostUuid : hostUuids) {
                DownloadImageToPrimaryStorageCacheMsg msg = new DownloadImageToPrimaryStorageCacheMsg();
                msg.setPrimaryStorageUuid(priUuid);
                msg.setImage(img);
                msg.setHostUuid(hostUuid);
                msg.setTimeout(TimeUnit.HOURS.toMillis(72));
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
                msgs.add(msg);
            }
        });
        if (msgs.isEmpty()) {
            return;
        }

        bus.send(msgs, MevocoGlobalConfig.DISTRIBUTE_IMAGE_PARA_LEVEL.value(Integer.class),
                new CloudBusSteppingCallback(null) {
                    @Override
                    public void run(NeedReplyMessage msg, MessageReply reply) {
                        DownloadImageToPrimaryStorageCacheMsg dmsg = (DownloadImageToPrimaryStorageCacheMsg) msg;
                        if (reply.isSuccess()) {
                            logger.debug(String.format("successfully downloaded the image[uuid:%s] to the primary storage[uuid:%s]",
                                    dmsg.getImage().getUuid(), dmsg.getPrimaryStorageUuid()));

                            LocalStorageCanonicalEvents.LocalStorageImageDistributedToImageCacheEvent evt = new LocalStorageCanonicalEvents.LocalStorageImageDistributedToImageCacheEvent();
                            evt.image = dmsg.getImage();
                            evt.localStorageUuid = dmsg.getPrimaryStorageUuid();
                            evt.hostUuid = dmsg.getHostUuid();
                            evt.fire();

                        } else {
                            logger.debug(String.format("failed to download the image[uuid:%s] to the primary storage[uuid:%s], %s",
                                    dmsg.getImage().getUuid(), dmsg.getPrimaryStorageUuid(), reply.getError()));
                        }
                    }
                });
    }

    private Object setNicOut(Object obj, String qos) {
        NicQos ob = new NicQos();
        if (obj != null) {
            if (obj instanceof NicQos) {
                ob = (NicQos) obj;
            } else {
                inerr("obj is not instanceof NicQos!");
            }
        }
        ob.outboundBandwidth = Long.valueOf(qos);
        return ob;
    }

    private Object setNicIn(Object obj, String qos) {
        NicQos ob = new NicQos();
        if (obj != null) {
            if (obj instanceof NicQos) {
                ob = (NicQos) obj;
            } else {
                operr("obj is not instanceof NicQos!");
            }
        }
        ob.inboundBandwidth = Long.valueOf(qos);
        return ob;
    }

    private HashMap<String, Object> objectToMap(Object obj) {
        return (HashMap)obj;
    }

    private void setAddons(String hostUuid, String key1, String key2, String type, Map<String, Object> addons, String qos) {
        exceptionIfQosNotSupportedOnKvm(hostUuid);
        Object obj = addons.get(key1);
        if (obj == null) {
            obj = new HashMap<String, Object>();
        }
        Object obj1 = objectToMap(obj).get(key2);
        switch (type) {
            case "in":
                obj1 = setNicIn(obj1, qos);
                break;
            case "out":
                obj1 = setNicOut(obj1, qos);
                break;
        }
        objectToMap(obj).put(key2, obj1);
        addons.put(key1, obj);
    }

    private void setNicQos(KVMHostInventory host, String nicUuid, VmAddOnsCmd cmd) {
        VmNicVO nicVO = dbf.findByUuid(nicUuid, VmNicVO.class);
        String vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, nicVO.getVmInstanceUuid()).select(VmInstanceVO_.type).findValue();
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(vmType);
        VmNicQosStruct struct = backend.getNicQos(cmd.getVmInstanceUuid(), nicUuid);
        if (struct.outboundBandwidth != -1L) {
            setAddons(host.getUuid(), MevocoConstants.KVM_NIC_QOS, nicUuid, "out", cmd.getAddons(), struct.outboundBandwidth.toString());
        }
        if (struct.inboundBandwidth != -1L) {
            setAddons(host.getUuid(), MevocoConstants.KVM_NIC_QOS, nicUuid, "in", cmd.getAddons(), struct.inboundBandwidth.toString());
        }
    }

    private void setVmUserDefinedXml(KVMHostInventory host, String vmUuid, Map<String, Object> addons, StartVmCmd cmd) {
        JsonLabelInventory label = UserDefinedXmlHelper.getUserDefinedVmXmlBase64(vmUuid);
        if (label == null) {
            return;
        }

        String xmlBase64 = label.getLabelValue();
        checkUserDefinedXmlVolumePath(cmd, new String (Base64.decodeBase64(xmlBase64)));
        addons.put(MevocoConstants.KVM_USER_DEFINED_XML, xmlBase64);
    }

    private void setVmUserDefinedXmlHookScript(KVMHostInventory host, String vmUuid, Map<String, Object> addons, StartVmCmd cmd) {
        JsonLabelInventory label = UserDefinedXmlHookScriptHelper.getUserDefinedVmXmlHookScriptBase64(vmUuid);
        if (label == null) {
            return;
        }

        String xmlHookScriptBase64 = label.getLabelValue();
        addons.put(MevocoConstants.KVM_USER_DEFINED_XML_HOOK_SCRIPT, xmlHookScriptBase64);
    }

    private boolean checkUserDefinedXmlVolumePath(StartVmCmd cmd, String userDefinedXml) {
        try {
            Document xmlDocument = DocumentHelper.parseText(userDefinedXml);
            Element rootElt = xmlDocument.getRootElement();
            Element devices = rootElt.element("devices");
            Iterator iterator = devices.elementIterator("disk");

            Set<String> installPaths = new HashSet<>();
            while (iterator.hasNext()) {
                Element recordEle = (Element) iterator.next();
                Element source = recordEle.element("source");
                if (source != null) {
                    if (source.attributeValue("name") != null) {
                        installPaths.add(source.attributeValue("name").trim());
                    } else if (source.attributeValue("file") != null) {
                        installPaths.add(source.attributeValue("file").trim());
                    }
                }
            }

            Set<String> dbInstallPaths = cmd.getDataVolumes().stream()
                    .map(VolumeTO::getInstallPath)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            dbInstallPaths.add(cmd.getRootVolume().getInstallPath());
            if (cmd.getCdRoms() != null) {
                dbInstallPaths.addAll(cmd.getCdRoms().stream()
                        .map(CdRomTO::getPath)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
            }
            if (dbInstallPaths.size() != installPaths.size()) {
                throw new CloudRuntimeException(String.format(
                        "number of volumes from database [%s] not match with xml [%s]" +
                                "volumes defined in database:\n%s \n" +
                                "volumes defined in xml: \n%s",
                        dbInstallPaths.size(), installPaths.size(), dbInstallPaths, installPaths));
            }

            int matches = 0;
            for(String installPath : installPaths) {
                if (dbInstallPaths.parallelStream().anyMatch(d -> d.contains(installPath))) {
                    matches += 1;
                }
            }

            if (matches != installPaths.size()) {
                throw new CloudRuntimeException(String.format(
                        "there are %s volumes defined in user defined xml, but only %s can match with database\n" +
                                "volumes defined in database:\n%s \n" +
                                "volumes defined in xml: \n%s",
                        installPaths.size(), matches, dbInstallPaths, installPaths));
            }
        } catch (DocumentException e) {
            throw new CloudRuntimeException(e);
        }

        return true;
    }

    private void setVolumeQos(KVMHostInventory host, String volumeUuid, Map<String, Object> addons) {
        VolumeVO vvo = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (vvo.isShareable()) {
            // do nothing with SharedVolume
            return;
        }

        exceptionIfQosNotSupportedOnKvm(host.getUuid());
        VolumeInventory volume = VolumeInventory.valueOf(vvo);
        VolumeProtocolCapability capability = VolumeProtocolCapability.get(volume.getProtocol(), KVMConstant.KVM_HYPERVISOR_TYPE);
        if (capability == null || capability.isSupportQosOnHypervisor()) {
            VolumeQos qos = VolumeQosHelper.getVolumeQos(volume.getVolumeQos());
            VolumeQosHelper.setVolumeQosAddons(qos, volumeUuid, addons);
        }
    }

    private void setPhysicalBlockSize(final VolumeTO to) {
        Integer sz = rcf.getResourceConfigValue(VolumeGlobalConfig.VOLUME_PHYSICAL_BLOCK_SIZE, to.getVolumeUuid(), Integer.class);
        if (sz != null) {
            to.setPhysicalBlockSize(sz);
        }
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) {
        for (NicTO nic : cmd.getNics()) {
            setNicQos(host, nic.getUuid(), cmd);
        }

        for (VolumeTO volume: cmd.getDataVolumes()) {
            setVolumeNativeAio(volume.getVolumeUuid(), cmd.getAddons());
            setVolumeQos(host, volume.getVolumeUuid(), cmd.getAddons());
            setPhysicalBlockSize(volume);
        }

        setVmUserDefinedXml(host, spec.getVmInventory().getUuid(), cmd.getAddons(), cmd);
        setVmUserDefinedXmlHookScript(host, spec.getVmInventory().getUuid(), cmd.getAddons(), cmd);
        setVolumeNativeAio(cmd.getRootVolume().getVolumeUuid(), cmd.getAddons());
        setVolumeQos(host, cmd.getRootVolume().getVolumeUuid(), cmd.getAddons());
        setPhysicalBlockSize(cmd.getRootVolume());

        if (MevocoSystemTags.VM_CONSOLE_MODE.hasTag(spec.getVmInventory().getUuid())) {
            String vmConsoleMode = MevocoSystemTags.VM_CONSOLE_MODE.getTokenByResourceUuid(spec.getVmInventory().getUuid(), MevocoSystemTags.VM_CONSOLE_MODE_TOKEN);
            if (vmConsoleMode.equals(MevocoConstants.VM_CONSOLE_SPICE)) {
                cmd.setConsoleMode(MevocoConstants.VM_CONSOLE_SPICE);
            } else if (vmConsoleMode.equals(MevocoConstants.VM_CONSOLE_VNC)) {
                cmd.setConsoleMode(MevocoConstants.VM_CONSOLE_VNC);
            } else if (vmConsoleMode.equals(MevocoConstants.VM_CONSOLE_VNCANDSPICE)) {
                cmd.setConsoleMode(MevocoConstants.VM_CONSOLE_VNCANDSPICE);
            } else {
                cmd.setConsoleMode(MevocoGlobalConfig.VM_CONSOLE_MODE.value());
            }
        } else {
            cmd.setConsoleMode(MevocoGlobalConfig.VM_CONSOLE_MODE.value());
        }

        setSpiceChannel(cmd);
    }

    private void setSpiceChannel(StartVmCmd cmd) {
        int mask = MevocoGlobalConfig.ENABLE_SPICE_CHANNEL_SUPPORT_TLS.value(Integer.class);
        if (mask > 0) {
            List<String> channels = SpiceChannelEnum.getSpiceChannels(mask);
            if (!channels.isEmpty()) {
                cmd.setSpiceChannels(channels);
            }
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APICreateTemplatedVmInstanceFromVmInstanceMsg) {
            handle((APICreateTemplatedVmInstanceFromVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            handle((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APICloneVmInstanceMsg) {
            handle((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APIChangeVmPasswordMsg) {
            handle((APIChangeVmPasswordMsg) msg);
        } else if (msg instanceof APIDeleteNicQosMsg) {
            handle((APIDeleteNicQosMsg) msg);
        } else if (msg instanceof APISetNicQosMsg) {
            handle((APISetNicQosMsg) msg);
        } else if (msg instanceof APIDeleteVolumeQosMsg) {
            handle((APIDeleteVolumeQosMsg) msg);
        } else if (msg instanceof APISetVolumeQosMsg) {
            handle((APISetVolumeQosMsg) msg);
        } else if (msg instanceof APIValidateVolumeSnapshotChainMsg) {
            handle((APIValidateVolumeSnapshotChainMsg) msg);
        } else if (msg instanceof APIResizeRootVolumeMsg) {
            handle((APIResizeRootVolumeMsg) msg);
        } else if (msg instanceof APIGetNicQosMsg) {
            handle((APIGetNicQosMsg) msg);
        } else if (msg instanceof APIGetVolumeQosMsg) {
            handle((APIGetVolumeQosMsg) msg);
        } else if (msg instanceof APIGetVmQgaMsg) {
            handle((APIGetVmQgaMsg) msg);
        } else if (msg instanceof APISetVmQgaMsg) {
            handle((APISetVmQgaMsg) msg);
        } else if (msg instanceof APIGetImageQgaMsg) {
            handle((APIGetImageQgaMsg) msg);
        } else if (msg instanceof APISetImageQgaMsg) {
            handle((APISetImageQgaMsg) msg);
        } else if (msg instanceof APISetImageSecurityLevelMsg) {
            handle((APISetImageSecurityLevelMsg) msg);
        } else if (msg instanceof APISetVmUsbRedirectMsg) {
            handle((APISetVmUsbRedirectMsg) msg);
        } else if (msg instanceof APIGetVmUsbRedirectMsg) {
            handle((APIGetVmUsbRedirectMsg) msg);
        } else if (msg instanceof APISetVmMonitorNumberMsg) {
            handle((APISetVmMonitorNumberMsg) msg);
        } else if (msg instanceof APIGetVmMonitorNumberMsg) {
            handle((APIGetVmMonitorNumberMsg) msg);
        } else if (msg instanceof APISetVmRDPMsg) {
            handle((APISetVmRDPMsg) msg);
        } else if (msg instanceof APIGetVmRDPMsg) {
            handle((APIGetVmRDPMsg) msg);
        } else if (msg instanceof APIChangeVmImageMsg) {
            handle((APIChangeVmImageMsg) msg);
        } else if (msg instanceof APIGetImageCandidatesForVmToChangeMsg) {
            handle((APIGetImageCandidatesForVmToChangeMsg) msg);
        } else if (msg instanceof StorageMigrationMessage) {
            handle((StorageMigrationMessage) msg);
        } else if (msg instanceof APIResizeDataVolumeMsg) {
            handle((APIResizeDataVolumeMsg) msg);
        } else if (msg instanceof APIUpdateVmNicMacMsg) {
            handle((APIUpdateVmNicMacMsg) msg);
        } else if (msg instanceof APISetVmConsoleModeMsg) {
            handle((APISetVmConsoleModeMsg) msg);
        } else if (msg instanceof APISetVmSecurityLevelMsg) {
            handle((APISetVmSecurityLevelMsg) msg);
        } else if (msg instanceof APISetVmCleanTrafficMsg) {
            handle((APISetVmCleanTrafficMsg) msg);
        } else if (msg instanceof APICreateVolumesSnapshotMsg) {
            handle((APICreateVolumesSnapshotMsg) msg);
        } else if (msg instanceof APICreateMiniClusterMsg) {
            handle((APICreateMiniClusterMsg) msg);
        } else if (msg instanceof APIGetCandidateMiniHostsMsg) {
            handle((APIGetCandidateMiniHostsMsg) msg);
        } else if (msg instanceof APIBootstrapMiniHostMsg) {
            handle((APIBootstrapMiniHostMsg) msg);
        } else if (msg instanceof APIGetVmInstanceFirstBootDeviceMsg) {
            handle((APIGetVmInstanceFirstBootDeviceMsg) msg);
        } else if (msg instanceof APIUpdateFactoryModeStateMsg) {
            handle((APIUpdateFactoryModeStateMsg) msg);
        } else if (msg instanceof UpdateFactoryModeStateMsg) {
            handle((UpdateFactoryModeStateMsg) msg);
        } else if (msg instanceof APIGetFactoryModeStateMsg) {
            handle((APIGetFactoryModeStateMsg) msg);
        } else if (msg instanceof GetFactoryModeStateMsg) {
            handle((GetFactoryModeStateMsg) msg);
        } else if (msg instanceof ChangeVmPasswordMsg) {
            handle((ChangeVmPasswordMsg) msg);
        } else if (msg instanceof APIDeleteVmUserDefinedXmlMsg) {
            handle((APIDeleteVmUserDefinedXmlMsg) msg);
        } else if (msg instanceof APISetVmUserDefinedXmlMsg) {
            handle((APISetVmUserDefinedXmlMsg) msg);
        } else if (msg instanceof APIGetVmXmlMsg) {
            handle((APIGetVmXmlMsg) msg);
        } else if (msg instanceof APIDeleteVmUserDefinedXmlHookScriptMsg) {
            handle((APIDeleteVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APISetVmUserDefinedXmlHookScriptMsg) {
            handle((APISetVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APIGetVmXmlHookScriptMsg) {
            handle((APIGetVmXmlHookScriptMsg) msg);
        } else if (msg instanceof ResizeVolumeMsg) {
            handle((ResizeVolumeMsg) msg);
        } else if (msg instanceof APISetVmNumaMsg) {
            handle((APISetVmNumaMsg) msg);
        } else if (msg instanceof APIGetVmNumaMsg) {
            handle((APIGetVmNumaMsg) msg);
        } else if (msg instanceof APISetVmEmulatorPinningMsg) {
            handle((APISetVmEmulatorPinningMsg) msg);
        } else if (msg instanceof APIGetVmEmulatorPinningMsg) {
            handle((APIGetVmEmulatorPinningMsg) msg);
        } else if (msg instanceof APIGetVmvNUMATopologyMsg) {
            handle((APIGetVmvNUMATopologyMsg) msg);
        } else if (msg instanceof APISyncVmClockMsg) {
            handle((APISyncVmClockMsg) msg);
        } else if (msg instanceof SyncVmClockMsg) {
            handle((SyncVmClockMsg) msg);
        } else if (msg instanceof SetVmQgaSyncClockTaskMsg) {
            handle((SetVmQgaSyncClockTaskMsg) msg);
        } else if (msg instanceof APIGetVirtualizerInfoMsg) {
            handle((APIGetVirtualizerInfoMsg) msg);
        } else if (msg instanceof APIGetVolumeIoThreadPinMsg){
            handle((APIGetVolumeIoThreadPinMsg) msg);
        } else if (msg instanceof APISetVolumeIoThreadPinMsg){
            handle((APISetVolumeIoThreadPinMsg) msg);
        } else if (msg instanceof APICreateBlockVolumeMsg) {
            handle((APICreateBlockVolumeMsg) msg);
        } else if (msg instanceof APIGetAccessPathMsg) {
            handle((APIGetAccessPathMsg) msg);
        } else if (msg instanceof BlockVolumeMessage) {
            passToBlockVolume(msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passToBlockVolume(Message msg) {
        String uuid = ((BlockVolumeMessage) msg).getBlockVolumeUuid();
        BlockVolumeVO vo = Q.New(BlockVolumeVO.class).eq(BlockVolumeVO_.uuid, uuid).find();
        if (vo == null) {
            throw new CloudRuntimeException(String.format("cannot find BlockVolume[uuid:%s], it may have been deleted", uuid));
        }

        Volume blockVolume = blockVolumeFactories.get(vo.getVendor()).getBlockVolume(vo);
        blockVolume.handleMessage(msg);
    }

    private void handle(APIGetAccessPathMsg msg) {
        APIGetAccessPathReply pathReply = new APIGetAccessPathReply();
        GetAccessPathMsg pathMsg = new GetAccessPathMsg();
        pathMsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(pathMsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());

        bus.send(pathMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if(reply.isSuccess()) {
                    pathReply.setPathInfos(((GetAccessPathReply)reply).getInfos());
                } else {
                    pathReply.setError(reply.getError());
                }
                bus.reply(msg, pathReply);
            }
        });
    }

    private void handle(APICreateBlockVolumeMsg msg) {
        APICreateBlockVolumeEvent event = new APICreateBlockVolumeEvent(msg.getId());
        VolumeVO vo = new VolumeVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setSize(msg.getSize());
        vo.setActualSize(0L);
        vo.setType(VolumeType.Data);
        vo.setStatus(VolumeStatus.NotInstantiated);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setProtocol(msg.getProtocol());

        if (msg.hasSystemTag(VolumeSystemTags.SHAREABLE.getTagFormat())) {
            vo.setShareable(true);
        }

        String manufacture = getManufactureByPrimaryStorage(msg.getPrimaryStorageUuid());
        BlockVolumeVO blockVolumeVO = blockVolumeFactories.get(manufacture).createBlockVolume(vo, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), VolumeVO.class.getSimpleName());
        InstantiateVolumeMsg imsg = new InstantiateVolumeMsg();
        imsg.setVolumeUuid(blockVolumeVO.getUuid());
        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        imsg.setSystemTags(msg.getSystemTags());
        imsg.setUserTags(msg.getUserTags());
        bus.makeTargetServiceIdByResourceUuid(imsg, VolumeConstant.SERVICE_ID, blockVolumeVO.getUuid());
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    dbf.remove(blockVolumeVO);
                    event.setError(reply.getError());
                } else {
                    event.setInventory(BlockVolumeInventory.valueOf(blockVolumeVO));
                }

                bus.publish(event);
            }
        });
    }

    private String getManufactureByPrimaryStorage(String primaryStorageUuid) {
        PrimaryStorageVO vo = dbf.findByUuid(primaryStorageUuid, PrimaryStorageVO.class);
        switch (vo.getType()) {
            case CephConstants.CEPH_PRIMARY_STORAGE_TYPE:
                return CephSystemTags.CEPH_MANUFACTURER.getTokenByResourceUuid(primaryStorageUuid,
                                CephSystemTags.CEPH_MANUFACTURER_TOKEN);
            case PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE:
                return ExponConstants.EXPON_MANUFACTURER;
        }

        return null;
    }

    private void handle(final APIGetVolumeIoThreadPinMsg msg) {
        passToVolume(msg, msg.getVolumeUuid());
    }

    private void handle(final APISetVolumeIoThreadPinMsg msg) {
        passToVolume(msg, msg.getVolumeUuid());
    }

    private void handle(APIGetVirtualizerInfoMsg msg) {
        APIGetVirtualizerInfoReply reply = new APIGetVirtualizerInfoReply();
        reply.setInventories(VirtualizerVersionChecker.check(msg.getUuids()));
        bus.reply(msg, reply);
    }

    private void installClusterHypervisorConfigListener() {
        MevocoGlobalConfig.HYPERVISOR_VERSION_ALARM_INTERVAL.installUpdateExtension(
                (oldConfig, newConfig) -> installClusterHypervisorPeriodicChecker(newConfig.value(Long.class)));

        final long interval = MevocoGlobalConfig.HYPERVISOR_VERSION_ALARM_INTERVAL.value(Long.class);
        installClusterHypervisorPeriodicChecker(interval);
    }

    private synchronized void installClusterHypervisorPeriodicChecker(final long intervalInSeconds) {
        if (clusterHypervisorPeriodicTask != null) {
            clusterHypervisorPeriodicTask.cancel(false);
        }

        logger.debug(String.format("Set interval for check cluster hypervisor to %d seconds", intervalInSeconds));
        clusterHypervisorPeriodicTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return intervalInSeconds;
            }

            @Override
            public String getName() {
                return "check-cluster-hypervisor-version";
            }

            @Override
            public void run() {
                try {
                    checkHostHypervisorMismatch();
                } catch (Exception e) {
                    logger.warn("failed to check cluster hypervisor version", e);
                }
            }
        });
    }

    /**
     * Check host hypervisor version.
     * If version mismatch, fire a canonical event.
     */
    public void checkHostHypervisorMismatch() {
        final List<String> clusterUuidList = SQL.New("select distinct host.clusterUuid from HostVO host").list();
        logger.debug(String.format("Check host hypervisor mismatch for %d clusters", clusterUuidList.size()));

        for (String clusterUuid : clusterUuidList) {
            final List<VirtualizerInfoInventory> invalidHosts =
                    VirtualizerVersionChecker.findHypervisorMismatchHostsInCluster(clusterUuid);
            if (invalidHosts.isEmpty()) {
                return;
            }

            ClusterHostHypervisorMismatchData data = new ClusterHostHypervisorMismatchData();
            data.setClusterUuid(clusterUuid);
            data.setHypervisorType(KVMConstant.VIRTUALIZER_QEMU_KVM);
            data.setHostCount(invalidHosts.size());
            evf.fire(ClusterHostHypervisorMismatchData.PATH, data);
        }
    }

    private void handle(final APISyncVmClockMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final SyncVmClockMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final SetVmQgaSyncClockTaskMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(final APIGetVmEmulatorPinningMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APISetVmEmulatorPinningMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APIGetVmNumaMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }


    private void handle(final APISetVmNumaMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(APIGetVmvNUMATopologyMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(ResizeVolumeMsg msg) {
        passToVolume(msg, msg.getVolumeUuid());
    }

    private void handle(ChangeVmPasswordMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(APIDeleteVmUserDefinedXmlMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APISetVmUserDefinedXmlMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APIGetVmXmlMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APIDeleteVmUserDefinedXmlHookScriptMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APISetVmUserDefinedXmlHookScriptMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APIGetVmXmlHookScriptMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APISetVmSecurityLevelMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(APISetVmCleanTrafficMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(APIChangeVmImageMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(APIGetImageCandidatesForVmToChangeMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void passToVmInstance(Message msg, String uuid) {
        VmInstanceVO vo = dbf.findByUuid(uuid, VmInstanceVO.class);
        if (vo == null) {
            String err = String.format("Cannot find VmInstance[uuid:%s], it may have been deleted", uuid);
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        VmInstance vm = factory.getVmInstance(vo);
        vm.handleMessage(msg);
    }

    private void passToVolume(Message msg, String uuid) {
        VolumeVO vo = dbf.findByUuid(uuid, VolumeVO.class);
        if (vo == null) {
            String err = String.format("Cannot find Volume[uuid:%s], it may have been deleted", uuid);
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        VolumeBase volume = volumeFactory.makeVolumeBase(vo);
        volume.handleMessage(msg);
    }

    private void passToImage(Message msg, String uuid) {
        ImageVO vo = dbf.findByUuid(uuid, ImageVO.class);
        if (vo == null) {
            String err = String.format("Cannot find Image[uuid:%s], it may have been deleted", uuid);
            bus.replyErrorByMessageType(msg, err);
            return;
        }
        Image image = imageFactory.getImage(vo);
        image.handleMessage(msg);
    }

    private void handle(final APICreateVolumesSnapshotMsg msg) {
        passToVolume(msg, msg.getVolumeUuids().get(0));
    }

    private void doAddHost(String managementIp, String clusterUuid,
                           APICreateMiniClusterMsg msg,
                           ConcurrentLinkedQueue<HostInventory> q,
                           ErrorCodeList errList,
                           WhileCompletion comp) {
        AddKVMHostMsg amsg = new AddKVMHostMsg();
        amsg.setName(String.format("%s/%s", msg.getName(), managementIp));
        amsg.setClusterUuid(clusterUuid);
        amsg.setUsername(msg.getUsername());
        amsg.setPassword(msg.getPassword());
        amsg.setSshPort(msg.getSshPort());
        amsg.setManagementIp(managementIp);
        amsg.setAccountUuid(msg.getSession().getAccountUuid());
        amsg.setUserTags(msg.getUserTags());
        bus.makeLocalServiceId(amsg, HostConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AddHostReply r = reply.castReply();
                    q.add(r.getInventory());
                    comp.done();
                    return;
                }

                errList.getCauses().add(reply.getError());
                comp.allDone();
            }
        });
    }

    private void handle(final APICreateMiniClusterMsg msg) {
        final APICreateMiniClusterEvent evt = new APICreateMiniClusterEvent(msg.getId());
        final Set<String> ips = new HashSet<>(msg.getHostManagementIps());

        if (ips.size() < 2) {
            evt.setError(argerr("unexpected host management IPs: [%s]", String.join(",", ips)));
            bus.publish(evt);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("create-mini-cluster-" + msg.getName());

        chain.then(new ShareFlow() {
            final ConcurrentLinkedQueue<HostInventory> queue = new ConcurrentLinkedQueue<> ();
            ClusterInventory inventory;

            @Override
            public void setup() {
                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateClusterMsg cmsg = new CreateClusterMsg();
                        cmsg.setClusterName(msg.getName());
                        cmsg.setDescription(msg.getDescription());
                        cmsg.setHypervisorType(msg.getHypervisorType());
                        cmsg.setType(ClusterConstant.ZSTACK_CLUSTER_TYPE);
                        cmsg.setResourceUuid(msg.getResourceUuid());
                        cmsg.setZoneUuid(msg.getZoneUuid());
                        bus.makeLocalServiceId(cmsg, ClusterConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateClusterReply r = reply.castReply();
                                    inventory = r.getInventory();

                                    tagMgr.createTagsFromAPICreateMessage(msg, inventory.getUuid(), ClusterVO.class.getSimpleName());

                                    SystemTagCreator creator = MiniClusterSystemTags.HOST_NUMBERS.newSystemTagCreator(inventory.getUuid());
                                    creator.setTagByTokens(Collections.singletonMap(MiniClusterSystemTags.HOST_NUMBERS_TOKEN, Integer.valueOf(ips.size()).toString()));
                                    creator.inherent = false;
                                    creator.recreate = false;
                                    creator.create();

                                    trigger.next();
                                    return;
                                }

                                trigger.fail(reply.getError());
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (inventory == null) {
                            trigger.rollback();
                            return;
                        }

                        ClusterDeletionMsg dmsg = new ClusterDeletionMsg();
                        dmsg.setClusterUuid(inventory.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, ClusterConstant.SERVICE_ID, inventory.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                dbf.removeByPrimaryKey(dmsg.getClusterUuid(), ClusterVO.class);
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(ips).each((ip, comp) -> doAddHost(ip, inventory.getUuid(), msg, queue, errList, comp))
                                .run(new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        if (errList.getCauses().isEmpty()) {
                                            trigger.next();
                                        } else {
                                            trigger.fail(errList.getCauses().get(0));
                                        }
                                    }
                                });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        new While<>(queue).each((q, comp) -> {
                            HostDeletionMsg dmsg = new HostDeletionMsg();
                            dmsg.setHostUuid(q.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, q.getUuid());
                            bus.send(dmsg, new CloudBusCallBack(comp) {
                                @Override
                                public void run(MessageReply reply) {
                                    comp.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                dbf.removeByPrimaryKeys(queue.stream().map(HostInventory::getUuid).collect(Collectors.toList()),
                                        HostVO.class);
                                trigger.rollback();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        for (MiniClusterExtensionPoint ext : pluginRgty.getExtensionList(MiniClusterExtensionPoint.class)) {
                            ext.afterCreateMiniCluster(inventory);
                        }

                        evt.setInventory(inventory);
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIGetCandidateMiniHostsMsg msg) {
        APIGetCandidateMiniHostsReply reply = new APIGetCandidateMiniHostsReply();

        if (msg.isLocal() && msg.isConfigure()) {
            reply.setError(argerr("can not set local and configure at same time"));
            bus.reply(msg, reply);
            return;
        }

        List<String> validSn = new ArrayList<>();
        if (msg.isLocal() || msg.isConfigure()) {
            Map<String, Map> r = restf.syncJsonGet(
                    BootstrapConstants.BOOTSTRAP_LOCAL_INTERFACE + BootstrapConstants.BOOTSTRAP_CONFIGURE_HOST_PATH, null, null, Map.class);
            for (Map<String, String> s : r.values()) {
                validSn.add(s.get("sn"));
            }
        }
        Map<String, Map> result = restf.syncJsonGet(
                BootstrapConstants.BOOTSTRAP_LOCAL_INTERFACE + BootstrapConstants.BOOTSTRAP_GET_ALL_HOST_PATH, null, null, Map.class);
        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).listValues();
        List<String> existsSerialNumbers = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            String sn = SYSTEM_SERIAL_NUMBER.getTokenByResourceUuid(hostUuid, HostVO.class, SYSTEM_SERIAL_NUMBER_TOKEN);
            if (sn != null) {
                existsSerialNumbers.add(sn);
            }
        }

        if (msg.isConfigure()) {
            validSn.addAll(existsSerialNumbers);
        }

        Map<String, MiniCandidateHostStruct> finalResult = new HashMap<>();
        for (Map.Entry<String, Map> s : result.entrySet()) {
            if (msg.isConfigure() && validSn.contains(s.getValue().get("sn"))) {
                finalResult.put(s.getKey(), MiniCandidateHostStruct.valueOf(s.getValue()));
            } else if (!existsSerialNumbers.contains(s.getValue().get("sn")) && (validSn.isEmpty() || validSn.contains(s.getValue().get("sn")))) {
                finalResult.put(s.getKey(), MiniCandidateHostStruct.valueOf(s.getValue()));
            }
        }
        reply.setHosts(new ArrayList<>(finalResult.values()));
        bus.reply(msg, reply);
    }

    private void handle(final APIBootstrapMiniHostMsg msg) {
        APIBootstrapMiniHostEvent event = new APIBootstrapMiniHostEvent(msg.getId());

        Map<String, Map> allHosts = restf.syncJsonGet(
                BootstrapConstants.BOOTSTRAP_LOCAL_INTERFACE + BootstrapConstants.BOOTSTRAP_GET_ALL_HOST_PATH, null, null, Map.class);

        MiniCandidateHostStruct target = new MiniCandidateHostStruct();
        if (msg.getLocal().getSn().toLowerCase().endsWith("a")) {
            target.setSn(msg.getLocal().getSn());
        } else if (msg.getPeer().getSn().toLowerCase().endsWith("a")) {
            target.setSn(msg.getPeer().getSn());
        } else {
            throw new ApiMessageInterceptionException(argerr("can not find node A config info"));
        }

        for (Map.Entry<String, Map> s : allHosts.entrySet()) {
            if (target.getSn().equals(s.getValue().get("sn"))) {
                target = MiniCandidateHostStruct.valueOf(s.getValue());
            }
        }

        if (target.getIpv6Address() == null || target.getIpv6Address().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("can not find node A address info from bootstrap agent"));
        }

        MiniBootstrapStruct s = new MiniBootstrapStruct();
        s.setLocal(msg.getLocal());
        s.setPeer(msg.getPeer());
        s.setRole("compute");
        s.setJobUuid(Platform.getUuid());

        ShellResult ret = ShellUtils.runAndReturn(String.format(
                "curl -d '%s' %s%%%s:7274%s", JSONObjectUtil.toJsonString(s), target.ipv6Address, target.ipv6Interface, BootstrapConstants.BOOTSTRAP_CONFIGURE_HOST_PATH));
        String runLog = String.format("curl bootstrap agent finished, return code: %s, stdout: %s, stderr: %s", ret.getRetCode(), ret.getStdout(), ret.getStderr());
        logger.debug(runLog);
        MiniBootstrapResult r = null;

        if (ret.getRetCode() == 0) {
            r = JSONObjectUtil.toObject(ret.getStdout(), MiniBootstrapResult.class);
        }
        if (ret.getRetCode() != 0 && ret.getRetCode() != 52 || (r != null && !r.isSuccess())) {
            event.setSuccess(false);
            event.setStage(r == null ? null : r.getStage());
            event.setError(argerr(r == null ? String.format("curl failed, %s", runLog) : r.getDetails()));
            bus.publish(event);
            return;
        }

        r = null;
        for (int i=1; i < 90; i++) {
            allHosts = restf.syncJsonGet(
                    BootstrapConstants.BOOTSTRAP_LOCAL_INTERFACE + BootstrapConstants.BOOTSTRAP_GET_ALL_HOST_PATH, null, null, Map.class);

            for (Map.Entry<String, Map> e : allHosts.entrySet()) {
                if (target.getSn().equals(e.getValue().get("sn"))) {
                    target = MiniCandidateHostStruct.valueOf(e.getValue());
                }
            }
            try {
                ret = ShellUtils.runAndReturn(String.format(
                        "curl %s%%%s:7274%s/%s", target.ipv6Address, target.ipv6Interface, BootstrapConstants.BOOTSTRAP_JOB_PATH, s.getJobUuid()));
            } catch (Exception e) {
                ret = new ShellResult();
                ret.setRetCode(-1);
                ret.setStderr(e.getMessage());
            }
            logger.debug(String.format("curl bootstrap agent finished, return code: %s, stdout: %s, stderr: %s", ret.getRetCode(), ret.getStdout(), ret.getStderr()));

            if (ret.getRetCode() != 0 || ret.getStdout().isEmpty()) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn("sleep got exception: %s", e.getCause());
                    Thread.currentThread().interrupt();
                }
            } else {
                r = JSONObjectUtil.toObject(ret.getStdout(), MiniBootstrapResult.class);
                logger.debug(String.format("bootstrap return: %s", r));
                break;
            }
        }

        if (r == null) {
            event.setSuccess(false);
            event.setError(operr("can not get bootstrap job %s result after 900s", s.getJobUuid()));
            bus.publish(event);
            return;
        }

        event.setSuccess(r.isSuccess());
        if (!r.isSuccess()) {
            event.setStage(r.getStage());
            event.setError(operr("curl bootstrap agent finished, return code: %s, stdout: %s, stderr: %s", ret.getRetCode(), ret.getStdout(), ret.getStderr()));
        }
        bus.publish(event);
    }

    private void handle(final APIGetImageQgaMsg msg) {
        passToImage(msg, msg.getUuid());
    }

    private void handle(final APISetImageQgaMsg msg) {
        passToImage(msg, msg.getUuid());
    }

    private void handle(final APISetImageSecurityLevelMsg msg) {
        passToImage(msg, msg.getUuid());
    }

    private void handle(final APIGetVmQgaMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }


    private void handle(final APISetVmQgaMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APIGetVmUsbRedirectMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }


    private void handle(final APISetVmUsbRedirectMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }


    private void handle(final APIUpdateVmNicMacMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(final APISetVmConsoleModeMsg msg) {
        passToVmInstance(msg, msg.getVmInstanceUuid());
    }

    private void handle(final APIGetVmMonitorNumberMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APIGetVmInstanceFirstBootDeviceMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APISetVmMonitorNumberMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APIGetVmRDPMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }


    private void handle(final APISetVmRDPMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void handle(final APIDeleteNicQosMsg msg) {
        String vmUuid = dbf.findByUuid(msg.getUuid(), VmNicVO.class).getVmInstanceUuid();
        passToVmInstance(msg, vmUuid);
    }

    private void handle(final APISetNicQosMsg msg) {
        String vmUuid = dbf.findByUuid(msg.getUuid(), VmNicVO.class).getVmInstanceUuid();
        passToVmInstance(msg, vmUuid);
    }

    private void handle(final APIDeleteVolumeQosMsg msg) {
        passToVolume(msg, msg.getUuid());
    }

    private void handle(final APIResizeRootVolumeMsg msg) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .find();
        PremiumVmInstanceFactory f = premiumVmInstanceFactories.get(vm.getType());
        if (f != null) {
            VmInstance vmInstance = f.getVmInstance(vm);
            vmInstance.handleMessage(msg);
        } else {
            passToVolume(msg, msg.getUuid());
        }
    }

    private void handle(final APIResizeDataVolumeMsg msg) {
        passToVolume(msg, msg.getUuid());
    }

    private void handle(final APISetVolumeQosMsg msg) {
        passToVolume(msg, msg.getUuid());
    }

    private void handle(final APIValidateVolumeSnapshotChainMsg msg) {
        passToVolume(msg, msg.getUuid());
    }

    private void handle(final APIGetNicQosMsg msg) {
        String vmUuid = dbf.findByUuid(msg.getUuid(), VmNicVO.class).getVmInstanceUuid();
        passToVmInstance(msg, vmUuid);
    }

    private void handle(final APIGetVolumeQosMsg msg) {
        passToVolume(msg, msg.getUuid());
    }

    private void handle(final APIChangeVmPasswordMsg msg) {
        passToVmInstance(msg, msg.getUuid());
    }

    private void cloneResourceConfig(GlobalConfig config, String srcUuid, String destUuid) {
        if (rcf.getResourceConfigValue(config, srcUuid, String.class).equals(config.defaultValue(String.class))) {
            return;
        }

        ResourceConfig resourceConfig = rcf.getResourceConfig(config.getIdentity());
        resourceConfig.updateValue(destUuid, rcf.getResourceConfigValue(config, srcUuid, String.class));
    }

    private void handle(final APICloneVmInstanceMsg msg) {
        VmInstanceVO vivo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vivo == null) {
            ErrorCode ec = err(
                    SysErrors.RESOURCE_NOT_FOUND,
                    "VM instance[uuid: %s] not found", msg.getVmInstanceUuid()
            );

            APICloneVmInstanceEvent evt = new APICloneVmInstanceEvent(msg.getId());
            evt.setError(ec);
            bus.publish(evt);
            return;
        }

        doCloneVmInstance(new CloneVmInstanceMessageBuilder(msg).buildCloneVmInstanceMsg(), new ReturnValueCompletion<CloneVmInstanceResults>(msg) {
            @Override
            public void success(CloneVmInstanceResults results) {
                APICloneVmInstanceEvent evt = new APICloneVmInstanceEvent(msg.getId());
                evt.setResult(results);
                bus.publish(evt);
                logger.info(String.format("cloned %d VMs from VM [uuid:%s]", results.getNumberOfClonedVm(), msg.getVmInstanceUuid()));
            }

            @Override
            public void fail(ErrorCode errCode) {
                APICloneVmInstanceEvent evt = new APICloneVmInstanceEvent(msg.getId());
                evt.setError(errCode);
                bus.publish(evt);
                logger.warn(String.format("failed to clone vm [uuid:%s], because %s", msg.getVmInstanceUuid(), errCode));
            }
        });
    }

    private void doCloneVmInstance(CloneVmInstanceMsg msg, ReturnValueCompletion<CloneVmInstanceResults> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("clone-vm-instance-%s", msg.getVmInstanceUuid()));
        chain.then(new ShareFlow() {
            final CloneVmInstanceResults results = new CloneVmInstanceResults();
            List<ImageInventory> tempImages;
            VmCreationStrategy strategy = VmCreationStrategy.valueOf(msg.getStrategy());

            final boolean templateIsTemporary = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("do-clone-vm-instance-for-vm-%s", msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CloneVmInstanceReply creply = reply.castReply();
                                results.setInventories(creply.getResults().getInventories());
                                results.setNumberOfClonedVm(creply.getResults().getNumberOfClonedVm());
                                tempImages = creply.getTempImages();
                                if (templateIsTemporary) {
                                    // TODO, remove this hack
                                    VolumeSnapshotReferenceUtils.cleanTemporaryImageReference(tempImages);
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                if (strategy == VmCreationStrategy.CreateStopped) {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("stop-paused-vm-%s", msg.getVmInstanceUuid());

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ErrorCodeList errorCodes = new ErrorCodeList();
                            new While<>(results.getInventoriesWithoutError()).all((vm, compl) -> {
                                if (vm.getInventory().getState().equals(VmInstanceState.Stopped.toString())) {
                                    compl.done();
                                    return;
                                }

                                String vmUuid = vm.getInventory().getUuid();
                                StopVmInstanceMsg stopMsg = new StopVmInstanceMsg();
                                stopMsg.setVmInstanceUuid(vmUuid);
                                stopMsg.setGcOnFailure(true);
                                stopMsg.setType(StopVmType.cold.toString());
                                stopMsg.setStopHA(true);
                                bus.makeTargetServiceIdByResourceUuid(stopMsg, VmInstanceConstant.SERVICE_ID, vm.getInventory().getUuid());
                                bus.send(stopMsg, new CloudBusCallBack(compl) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            vm.getInventory().setState(VmInstanceState.Stopped.toString());
                                        } else {
                                            errorCodes.getCauses().add(reply.getError());
                                        }
                                        compl.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (!errorCodes.getCauses().isEmpty()) {
                                        trigger.fail(errorCodes.getCauses().get(0));
                                        return;
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                boolean deleteTemplate = MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class);
                if ((deleteTemplate || templateIsTemporary) && strategy != VmCreationStrategy.JustCreate) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-temp-images";

                        @Override
                        public boolean skip(Map data) {
                            return CollectionUtils.isEmpty(tempImages);
                        }

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            new While<>(tempImages).all((imageInv, compl) -> {
                                ImageDeletionMsg msg = new ImageDeletionMsg();
                                msg.setImageUuid(imageInv.getUuid());
                                msg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                                msg.setForceDelete(true);
                                bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imageInv.getUuid());
                                bus.send(msg, new CloudBusCallBack(compl) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        compl.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat()) && msg.hasSystemTag(VolumeSystemTags.FLATTEN.getTagFormat())) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "flatten-cloned-vm";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            new While<>(results.getInventoriesWithoutError()).step((cloneInv, compl) -> {
                                FlattenVmInstanceMsg fmsg = new FlattenVmInstanceMsg();
                                fmsg.setUuid(cloneInv.getInventory().getUuid());
                                fmsg.setFull(true);
                                bus.makeTargetServiceIdByResourceUuid(fmsg, VmInstanceConstant.SERVICE_ID, fmsg.getUuid());
                                bus.send(fmsg, new CloudBusCallBack(compl) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            cloneInv.setError(reply.getError());
                                        }
                                        compl.done();
                                    }
                                });
                            }, 10).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        completion.success(results);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APICreateTemplatedVmInstanceFromVmInstanceMsg msg) {
        APICreateTemplatedVmInstanceFromVmInstanceEvent event = new APICreateTemplatedVmInstanceFromVmInstanceEvent(msg.getId());

        final CloneVmInstanceMsg cloneMsg = new CloneVmInstanceMessageBuilder(msg).buildCloneVmInstanceMsg();
        // create templeted VM always retain TPM states (if it has)
        cloneMsg.setResetTpm(false);

        doCloneVmInstance(cloneMsg, new ReturnValueCompletion<CloneVmInstanceResults>(msg) {
            @Override
            public void success(CloneVmInstanceResults results) {
                CloneVmInstanceInventory inv = results.getInventories().get(0);
                if (inv.getError() != null) {
                    event.setError(inv.getError());
                    bus.publish(event);
                    return;
                }
                TemplatedVmInstanceVO vo = new TemplatedVmInstanceVO();
                vo.setUuid(inv.getInventory().getUuid());
                TemplatedVmInstanceInventory.valueOf(vo);
                dbf.persist(vo);

                SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, inv.getInventory().getUuid())
                        .set(VmInstanceVO_.name, msg.getName()).update();

                event.setTemplatedVmInstanceInventory(TemplatedVmInstanceInventory.valueOf(vo));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errCode) {
                event.setError(errCode);
                bus.publish(event);
            }
        });
    }

    static class CreateVmInstanceFromTemplatedVmInstanceChunk {
        final CreateVmInstanceFromTemplatedVmInstanceResults results = new CreateVmInstanceFromTemplatedVmInstanceResults();
        TemplatedVmInstanceCacheVO templatedCache;
        List<DiskAO> newVolumeDiskAOs;
        APICreateVmInstanceFromTemplatedVmInstanceMsg msg;
    }

    private void handle(final APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        APICreateVmInstanceFromTemplatedVmInstanceEvent evt = new APICreateVmInstanceFromTemplatedVmInstanceEvent(msg.getId());
        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("create-vm-from-templated-vm-%s", msg.getTemplatedVmInstanceUuid()));

        CreateVmInstanceFromTemplatedVmInstanceChunk chunk = new CreateVmInstanceFromTemplatedVmInstanceChunk();
        chunk.msg = msg;

        flowChain.then(new NoRollbackFlow() {
            String __name__ = String.format("create-templated-vm-cache-for-templated-vm-%s", msg.getTemplatedVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                thdf.chainSubmit(new ChainTask(trigger) {
                    @Override
                    public String getName() {
                        return String.format("create-cache-for-templated-vm-%s", msg.getTemplatedVmInstanceUuid());
                    }

                    @Override
                    public String getSyncSignature() {
                        return getName();
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        createCacheForTemplatedVm(chunk, new Completion(trigger, chain) {
                            @Override
                            public void success() {
                                chain.next();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                chain.next();
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
            }
        });
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "create-vm-from-templated-cache";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CloneVmInstanceMessageBuilder cloneVmInstanceMessageBuilder = new CloneVmInstanceMessageBuilder(msg);
                chunk.newVolumeDiskAOs = cloneVmInstanceMessageBuilder.getNewVolumeDiskAOs();
                CloneVmInstanceMsg clmsg = cloneVmInstanceMessageBuilder.buildCloneVmInstanceMsg();
                doCloneVmInstance(clmsg,
                        new ReturnValueCompletion<CloneVmInstanceResults>(trigger) {
                            @Override
                            public void success(CloneVmInstanceResults res) {
                                List<TemplatedVmInstanceRefVO> refs = new ArrayList<>();
                                for (CloneVmInstanceInventory inv : res.getInventoriesWithoutError()) {
                                    TemplatedVmInstanceRefVO ref = new TemplatedVmInstanceRefVO();
                                    ref.setTemplatedVmInstanceUuid(msg.getTemplatedVmInstanceUuid());
                                    ref.setVmInstanceUuid(inv.getInventory().getUuid());
                                    dbf.persistAndRefresh(ref);
                                    refs.add(ref);
                                }
                                chunk.results.setInventories(res.getInventories());
                                chunk.results.setNumberOfClonedVm(res.getNumberOfClonedVm());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errCode) {
                                trigger.fail(errCode);
                            }
                        });
            }
        });
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "add-volume-to-vm";

            @Override
            public boolean skip(Map data) {
                return CollectionUtils.isEmpty(chunk.newVolumeDiskAOs);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(chunk.results.getInventoriesWithoutError()).each((vmInv, whileCompletion) -> {
                    FlowChain flowChain = FlowChainBuilder.newShareFlowChain();
                    flowChain.setName(String.format("add-volume-to-vm-%s", vmInv.getInventory().getUuid()));
                    flowChain.getData().put(VmInstanceInventory.class.getSimpleName(), vmInv.getInventory());
                    flowChain.then(new ShareFlow() {
                        @Override
                        public void setup() {
                            chunk.newVolumeDiskAOs.forEach(diskAO -> flow(new VmInstantiateOtherDiskFlow(diskAO)));
                            done(new FlowDoneHandler(whileCompletion) {
                                @Override
                                public void handle(Map data) {
                                    whileCompletion.done();
                                }
                            });
                            error(new FlowErrorHandler(whileCompletion) {
                                @Override
                                public void handle(ErrorCode errCode, Map data) {
                                    whileCompletion.done();
                                }
                            });
                        }
                    }).start();
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        });
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "start-vm";

            @Override
            public boolean skip(Map data) {
                return !Objects.equals(msg.getStrategy(), VmCreationStrategy.InstantStart.toString());
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(chunk.results.getInventoriesWithoutError()).all((inv, compl) -> {
                    StartVmInstanceMsg smsg = new StartVmInstanceMsg();
                    smsg.setVmInstanceUuid(inv.getInventory().getUuid());
                    smsg.setHostUuid(msg.getHostUuid());
                    bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                    bus.send(smsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                inv.getInventory().setState(VmInstanceState.Running.toString());
                            } else {
                                inv.setError(reply.getError());
                            }
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        });
        flowChain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setResult(chunk.results);
                bus.publish(evt);
            }
        });
        flowChain.error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        });
        flowChain.start();
    }

    private void createCacheForTemplatedVm(CreateVmInstanceFromTemplatedVmInstanceChunk chunk, Completion completion) {
        chunk.templatedCache = Q.New(TemplatedVmInstanceCacheVO.class)
                .eq(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, chunk.msg.getTemplatedVmInstanceUuid()).find();
        if (chunk.templatedCache != null) {
            List<VolumeSnapshotGroupVO> groups = Q.New(VolumeSnapshotGroupVO.class)
                    .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, chunk.templatedCache.getCacheVmInstanceUuid()).list();
            chunk.msg.setTemplatedVmInstanceCache(chunk.templatedCache);
            chunk.msg.setTemplatedCacheVolumeSnapshotGroup(groups.isEmpty() ? null : VolumeSnapshotGroupInventory.valueOf(groups.get(0)));
            completion.success();
            return;
        }

        CloneVmInstanceMsg clmsg = new CloneVmInstanceMessageBuilder(chunk.msg).buildCloneVmInstanceMsg();

        chunk.templatedCache = new TemplatedVmInstanceCacheVO();
        chunk.templatedCache.setTemplatedVmInstanceUuid(chunk.msg.getTemplatedVmInstanceUuid());
        chunk.templatedCache.setCacheVmInstanceUuid(clmsg.getResourceUuidByName().get(clmsg.getNames().get(0)));
        dbf.persist(chunk.templatedCache);
        chunk.msg.setTemplatedVmInstanceCache(chunk.templatedCache);

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("create-cache-for-templated-vm-%s", chunk.msg.getTemplatedVmInstanceUuid()));
        flowChain.then(new Flow() {
            boolean isTemplateCacheCreated = false;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                doCloneVmInstance(clmsg, new ReturnValueCompletion<CloneVmInstanceResults>(completion) {
                    @Override
                    public void success(CloneVmInstanceResults results) {
                        CloneVmInstanceInventory inv = results.getInventories().get(0);
                        if (inv.getError() != null) {
                            trigger.fail(operr("failed to create cache for templated vmInstance %s, " +
                                    "because %s", chunk.msg.getTemplatedVmInstanceUuid(), inv.getError().getDetails()));
                            return;
                        }
                        isTemplateCacheCreated = true;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errCode) {
                        trigger.fail(errCode);
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (isTemplateCacheCreated) {
                    trigger.rollback();
                    return;
                }
                String cacheUuid = chunk.templatedCache.getCacheVmInstanceUuid();

                DestroyVmInstanceMsg dmsg = new DestroyVmInstanceMsg();
                dmsg.setVmInstanceUuid(cacheUuid);
                dmsg.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct);
                bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, cacheUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete templated vmInstance cache[uuid: %s]", cacheUuid));
                        }
                        logger.info(String.format("success to delete templated vmInstance cache[uuid: %s]", cacheUuid));

                        SQL.New(TemplatedVmInstanceCacheVO.class)
                                .eq(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, chunk.templatedCache.getTemplatedVmInstanceUuid())
                                .eq(TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid, chunk.templatedCache.getCacheVmInstanceUuid())
                                .delete();
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("create-snapshot-groups-for-templated-vm-cache-%s", chunk.templatedCache.getCacheVmInstanceUuid());

            @Override
            public void run(FlowTrigger innerTrigger, Map data) {
                VmInstanceVO cacheVm = dbf.findByUuid(chunk.templatedCache.getCacheVmInstanceUuid(), VmInstanceVO.class);
                CreateVolumeSnapshotGroupMsg cmsg = new CreateVolumeSnapshotGroupMsg();
                cmsg.setRootVolumeUuid(cacheVm.getRootVolumeUuid());
                cmsg.setVmInstance(VmInstanceInventory.valueOf(cacheVm));
                cmsg.setConsistentType(ConsistentType.None);
                cmsg.setName("snapshot-groups-for-templated-vm-cache");
                cmsg.setDescription(String.format("bulk snapshot for templated vm cache[%s]", cacheVm.getUuid()));
                cmsg.setSession(chunk.msg.getSession());

                bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, cmsg.getVolumeUuid());
                bus.send(cmsg, new CloudBusCallBack(innerTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            innerTrigger.fail(reply.getError());
                            return;
                        }

                        CreateVolumeSnapshotGroupReply cr = reply.castReply();
                        VolumeSnapshotGroupInventory group = cr.getInventory();
                        for (VolumeSnapshotGroupRefInventory ref : group.getVolumeSnapshotRefs()) {
                            tagMgr.createNonInherentSystemTag(ref.getVolumeSnapshotUuid(),
                                    VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                                    VolumeSnapshotVO.class.getSimpleName());
                        }
                        chunk.msg.setTemplatedCacheVolumeSnapshotGroup(group);

                        CloneVmCanonicalEvents.VmInnerSnapshotCreated createdEvent = new CloneVmCanonicalEvents.VmInnerSnapshotCreated();
                        createdEvent.primaryStorageUuid = cacheVm.getRootVolume().getPrimaryStorageUuid();
                        createdEvent.vmInstanceUuid = cacheVm.getUuid();
                        createdEvent.snapshotInventoryList = group.getVolumeSnapshotRefs();
                        createdEvent.fire();

                        innerTrigger.next();
                    }
                });
            }
        });

        flowChain.done(new FlowDoneHandler(completion) {
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

    private void handle(StorageMigrationMessage msg) {
        storageMigrationService.handleMessage(msg);
    }

    private void handle(APIUpdateFactoryModeStateMsg msg) {
        APIUpdateFactoryModeStateEvent event = new APIUpdateFactoryModeStateEvent(msg.getId());
        List<ManagementNodeVO> managementNodeVOS = Q.New(ManagementNodeVO.class).list();

        ErrorCodeList errorOfNodeA = new ErrorCodeList();
        new While<>(managementNodeVOS).all((managementNodeVO, whileCompletion) -> {
            if (!managementNodeVO.getState().equals(ManagementNodeState.RUNNING)) {
                whileCompletion.addError(operr("management node status is not %s", ManagementNodeState.RUNNING));
                whileCompletion.done();
                return;
            }

            UpdateFactoryModeStateMsg umsg = new UpdateFactoryModeStateMsg();
            umsg.setFactoryModeState(msg.getFactoryModeState());
            bus.makeServiceIdByManagementNodeId(umsg, MevocoConstants.SERVICE_ID, managementNodeVO.getUuid());
            bus.send(umsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                        UpdateFactoryModeStateReply r = reply.castReply();
                        if (r.isNodeA()) {
                            errorOfNodeA.getCauses().add(reply.getError());
                        }
                    }
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(event) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() == managementNodeVOS.size()) {
                    if (errorOfNodeA.getCauses().isEmpty()) {
                        event.setError(operr("all management node update factory mode failed, details: %s", errorCodeList.getCauses().get(0)));
                    } else {
                        event.setError(operr("node A update factory mode failed, details: %s", errorOfNodeA.getCauses().get(0)));
                    }
                }
                bus.publish(event);
            }
        });
    }

    private void handle(UpdateFactoryModeStateMsg msg) {
        UpdateFactoryModeStateReply reply = new UpdateFactoryModeStateReply();

        if (msg.getFactoryModeState().equals(false)) {
            new Bash() {
                @Override
                protected void scripts() {
                    sudoRun("sudo ip a del 100.66.66.66/24 dev br_bond0; " +
                            "sudo ip a del 100.66.66.66/24 dev br_eno1; " +
                            "sudo ip a del 100.66.66.66/24 dev bond0; " +
                            "sudo ip a del 100.66.66.66/24 dev eno1; " +
                            "sudo ip a del 100.66.66.67/24 dev br_bond0; " +
                            "sudo ip a del 100.66.66.67/24 dev br_eno1; " +
                            "sudo ip a del 100.66.66.67/24 dev bond0; " +
                            "sudo ip a del 100.66.66.67/24 dev eno1; " +
                            "sudo systemctl disable zstack-mini-factory-mode.service; " +
                            "sudo systemctl daemon-reload");
                }
            }.execute();
            bus.reply(msg, reply);
            return;
        }

        String cmd = "sudo arping -b 100.66.66.66 -I br_bond0 -c1 -w1 -f || sudo arping -b 100.66.66.66 -I br_eno1 -c1 -w1 -f || sudo arping -b 100.66.66.66 -I bond0 -c1 -w1 -f || sudo arping -b 100.66.66.66 -I eno1 -c1 -w1 -f";
        ShellResult r = ShellUtils.runAndReturn(cmd, false);
        if (r.getRetCode() == 0) {
            reply.setError(operr("some node on factory mode exists, detail of arping: %s", r.getStdout()));
            bus.reply(msg, reply);
            return;
        }

        cmd = "set -x; " +
                        "isMini=`sudo dmidecode -s system-serial-number | rev | head -c1`;" +
                        "if [ $isMini == 'A' ]; then" +
                        "  sudo ip link set br_bond0 up || true; sudo ip link set br_eno1 up || true; sudo ip link set bond0 up || true; sudo ip link set eno1 up || true;" +
                        "  hasIp=`sudo ip r | grep 100.66.66.66 || true`;" +
                        "  if [ x\"$hasIp\" == \"x\" ]; then" +
                        "    sudo ip a add 100.66.66.66/24 dev br_bond0 || sudo ip a add 100.66.66.66/24 dev br_eno1 || sudo ip a add 100.66.66.66/24 dev bond0 || sudo ip a add 100.66.66.66/24 dev eno1;" +
                        "  fi;" +
                        "  sudo arping -U -c 3 -I `ip -o r get 100.66.66.254 | awk '{print $3}'` 100.66.66.66 || true;" +
                        "else exit 127;" +
                        "fi";
        r = ShellUtils.runAndReturn(cmd, false);
        if (r.getRetCode() == 127) {
            reply.setError(operr("this node is not node A"));
            logger.debug(String.format("returnCode: %s, stdout: %s, stderr: %s", r.getRetCode(), r.getStdout(), r.getStderr()));
        } else if (r.getRetCode() != 0) {
            reply.setError(operr("set address on node A failed"));
            reply.setNodeA(true);
            logger.debug(String.format("returnCode: %s, stdout: %s, stderr: %s", r.getRetCode(), r.getStdout(), r.getStderr()));
        }

        if (reply.isSuccess()) {
            final String script = "#!/bin/sh\nip a add 100.66.66.66/24 dev br_bond0 || ip a add 100.66.66.66/24 dev br_eno1 || ip a add 100.66.66.66/24 dev bond0 || ip a add 100.66.66.66/24 dev eno1";
            final String scriptPath = "/var/lib/zstack/zstack-mini-factory-mode.sh";
            final String servicePath = "/usr/lib/systemd/system/zstack-mini-factory-mode.service";
            final String tmpServicePath = "/tmp/zstack-mini-factory-mode.service";
            final String service = String.format("[Unit]\n" +
                    "Description=zstack-mini-factory-mode\n" +
                    "Documentation=https://zstack.io\n" +
                    "After=libvirtd.service\n" +
                    "\n" +
                    "[Service]\n" +
                    "Type=oneshot\n" +
                    "ExecStart=\"%s\"\n" +
                    "\n" +
                    "[Install]\n" +
                    "WantedBy=multi-user.target", scriptPath);

            new Bash() {
                @Override
                protected void scripts() {
                    runFormat("echo '%s' > %s", script, scriptPath);
                    runFormat("echo '%s' > %s; sudo cp %s %s", service, tmpServicePath, tmpServicePath, servicePath);
                    sudoRunFormat("chmod +x %s", scriptPath);
                    sudoRunFormat("systemctl enable zstack-mini-factory-mode; sudo systemctl daemon-reload");
                }
            }.execute();

            try (FileOutputStream s = new FileOutputStream(servicePath)) {
                s.getFD().sync();
            } catch (IOException e) {
                logger.warn(String.format("persist %s failed: %s", servicePath, e.getMessage()));
            }
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetFactoryModeStateMsg msg) {
        APIGetFactoryModeStateReply reply = new APIGetFactoryModeStateReply();
        reply.setFactoryModeState(false);
        List<ManagementNodeVO> managementNodeVOS = Q.New(ManagementNodeVO.class).list();

        new While<>(managementNodeVOS).all((managementNodeVO, whileCompletion) -> {
            if (!managementNodeVO.getState().equals(ManagementNodeState.RUNNING)) {
                whileCompletion.addError(operr("management node status is not %s", ManagementNodeState.RUNNING));
                whileCompletion.done();
                return;
            }

            GetFactoryModeStateMsg umsg = new GetFactoryModeStateMsg();
            bus.makeServiceIdByManagementNodeId(umsg, MevocoConstants.SERVICE_ID, managementNodeVO.getUuid());
            bus.send(umsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                    }
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() != managementNodeVOS.size()) {
                    reply.setFactoryModeState(true);
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetFactoryModeStateMsg msg) {
        GetFactoryModeStateReply reply = new GetFactoryModeStateReply();
        try {
            new Bash() {
                @Override
                protected void scripts() {
                    setE();
                    sudoRun("ip r | grep -w 100.66.66.66");
                }
            }.execute();
        } catch (RuntimeException e) {
            reply.setFactoryModeState(false);
            reply.setSuccess(false);
            reply.setError(operr(e.getMessage()));
        }

        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(MevocoConstants.SERVICE_ID);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        // intercept all APIs
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateInstanceOfferingMsg) {
            validate((APICreateInstanceOfferingMsg) msg);
        } else if (msg instanceof APIDeleteMessage) {
            fillResourceNameInDeleteMessages((APIDeleteMessage) msg);
        }
        return msg;
    }

    private void validate(APICreateInstanceOfferingMsg msg) {
        if (msg.getSystemTags() == null) {
            return;
        }

        for (String tag : msg.getSystemTags()) {
            String bandwidth = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByTag(tag, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
            if (bandwidth != null) {
                try {
                    long band = Long.parseLong(bandwidth);
                    if (band > MevocoConstants.MAX_NIC_BANDWIDTH) {
                        throw new ApiMessageInterceptionException(operr("networkInboundBandwidth execeds the max value 32G bps"));
                    }
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(operr("networkInboundBandwidth format error %s", bandwidth));
                }
            }

            bandwidth = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByTag(tag, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
            if (bandwidth != null) {
                try {
                    long band = Long.parseLong(bandwidth);
                    if (band > MevocoConstants.MAX_NIC_BANDWIDTH) {
                        throw new ApiMessageInterceptionException(operr("networkOutboundBandwidth execeds the max value 32G bps"));
                    }
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(operr("networkOutboundBandwidth format error %s", bandwidth));
                }
            }
        }
    }

    private void fillResourceNameInDeleteMessages(APIDeleteMessage msg) {
        if (msg.getDeletedResourceUuidList() == null) {
            return;
        }

        final Set<String> uuidSet = new HashSet<>(msg.getDeletedResourceUuidList());
        uuidSet.remove(null);
        if (CollectionUtils.isEmpty(uuidSet)) {
            return;
        }

        final List<Tuple> tuples = Q.New(ResourceVO.class)
                .in(ResourceVO_.uuid, uuidSet)
                .notNull(ResourceVO_.resourceName)
                .select(ResourceVO_.uuid, ResourceVO_.resourceName)
                .listTuple();
        msg.setResourceNameMap(CollectionUtils.toMap(tuples, t -> t.get(0, String.class), t -> t.get(1, String.class)));
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeToSharebleIfNeed(inventory, to);
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, Map data) {
        setVolumeNativeAio(volume.getUuid(), cmd.getAddons());
        setVolumeQos(host, volume.getUuid(), cmd.getAddons());
        checkShareableDiskAttachable(vm, volume);
        cmd.setVolume(convertVolumeToSharebleIfNeed(volume, cmd.getVolume()));
        setPhysicalBlockSize(cmd.getVolume());
    }

    private void setVolumeNativeAio(String volumeUuid, Map<String, Object> addons) {
        Map<String, Boolean> aioMap;
        Object obj = addons.get(MevocoConstants.NATIVE_AIO);
        if(obj != null){
            aioMap = (Map<String, Boolean>) obj;
        }else {
            aioMap = new HashMap<>();
        }

        Boolean volumeAioConfig = rcf.getResourceConfigValue(MevocoGlobalConfig.AIO_NATIVE, volumeUuid, Boolean.class);
        aioMap.put(volumeUuid, volumeAioConfig);
        
        addons.put(MevocoConstants.NATIVE_AIO, aioMap);
    }

    private void checkShareableDiskAttachable(VmInstanceInventory vm, VolumeInventory volume) {
        boolean isExists = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vm.getUuid())
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, volume.getUuid())
                .count() > 0;
        if (isExists) {
            throw new OperationFailureException(operr(
                    "Shareable Volume[uuid:%s] has already been attached to VM[uuid:%s]",
                    volume.getUuid(), vm.getUuid()));
        }
    }


    private VolumeTO convertVolumeToSharebleIfNeed(VolumeInventory volumeInventory, VolumeTO to) {
        boolean isShareable;
        boolean isVirtioSCSI;

        isShareable = volumeInventory.isShareable();
        isVirtioSCSI = KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volumeInventory.getUuid());

        if (!isShareable) {
            return to;
        }
        if (!isVirtioSCSI) {
            throw new ApiMessageInterceptionException(operr("shareable disk only support virtio-scsi type for now"));
        }

        to.setShareable(true);
        return to;
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, ErrorCode err, Map data) {

    }

    @Override
    public void preChangeInstanceOffering(VmInstanceInventory vm, InstanceOfferingInventory offering) {

    }

    @Override
    public void beforeChangeInstanceOffering(VmInstanceInventory vm, InstanceOfferingInventory offering) {

    }

    public void copyInstanceOfferingQos(String instanceOfferingUuid, String volumeUuid) {
        if (instanceOfferingUuid == null || volumeUuid == null) {
            return;
        }

        VolumeQosHelper.copyQosFromOffering(instanceOfferingUuid, InstanceOfferingVO.class, volumeUuid);
        VolumeQosHelper.copyIopsQosFromOffering(instanceOfferingUuid, InstanceOfferingVO.class, volumeUuid);
    }

    @Override
    public void afterChangeInstanceOffering(VmInstanceInventory vm, InstanceOfferingInventory offering) {
        if (MevocoSystemTags._VOLUME_TOTAL_IOPS.hasTag(offering.getUuid())) {
            MevocoSystemTags._VOLUME_TOTAL_IOPS.copy(offering.getUuid(), InstanceOfferingVO.class, vm.getUuid(), VmInstanceVO.class);
        } else {
            MevocoSystemTags._VOLUME_TOTAL_IOPS.delete(vm.getUuid(), VmInstanceVO.class);
        }

        copyInstanceOfferingQos(offering.getUuid(), vm.getRootVolumeUuid());

        if (MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.hasTag(offering.getUuid())) {
            MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.copy(offering.getUuid(), InstanceOfferingVO.class, vm.getUuid(), VmInstanceVO.class);
        } else {
            MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.delete(vm.getUuid(), VmInstanceVO.class);
        }

        if (MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.hasTag(offering.getUuid())) {
            MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.copy(offering.getUuid(), InstanceOfferingVO.class, vm.getUuid(), VmInstanceVO.class);
        } else {
            MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.delete(vm.getUuid(), VmInstanceVO.class);
        }
    }

    @Override
    public void preUpdateNic(KVMHostInventory host, UpdateNicCmd cmd) {
        for (NicTO nic : cmd.getNics()) {
            setNicQos(host, nic.getUuid(), cmd);
            setNicCleanTraffic(nic, cmd.getVmInstanceUuid());
        }
    }

    private void setNicCleanTraffic(NicTO nic, String vmInstanceUuid) {
        String tagValue = VmSystemTags.CLEAN_TRAFFIC.getTokenByResourceUuid(vmInstanceUuid, VmSystemTags.CLEAN_TRAFFIC_TOKEN);
        if (tagValue != null) {
            nic.setCleanTraffic(Boolean.parseBoolean(tagValue));
        } else {
            nic.setCleanTraffic(VmGlobalConfig.VM_CLEAN_TRAFFIC.value(Boolean.class));
        }
    }

    @Override
    public void preAttachNicExtensionPoint(KVMHostInventory host, AttachNicCommand cmd) {
        String vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, cmd.getVmUuid()).select(VmInstanceVO_.type).findValue();
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(vmType);
        VmNicQosStruct struct = backend.getNicQos(cmd.getVmUuid(), cmd.getNic().getUuid());
        Long nobw = struct.outboundBandwidth;
        Long nibw = struct.inboundBandwidth;
        if (nobw != -1L || nibw != -1L) {
            exceptionIfQosNotSupportedOnKvm(host.getUuid());

            NicQos qos = new NicQos();
            qos.outboundBandwidth = nobw;
            qos.inboundBandwidth = nibw;
            qos.uuid = cmd.getNic().getUuid();
            Map<String, NicQos> nicQos = new HashMap<>();

            nicQos.put(qos.uuid, qos);
            cmd.getAddons().put(MevocoConstants.KVM_NIC_QOS, nicQos);
        }
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {

    }

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void afterInstantiateVolumeForNewCreatedVm(VmInstanceInventory vm, VolumeInventory volume) {
        updateShareableVolume(vm, volume);
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        updateShareableVolume(vm, volume);
    }

    private void updateShareableVolume(VmInstanceInventory vm, VolumeInventory volume) {
        if (volume.isShareable()) {
            ShareableVolumeVmInstanceRefVO vo = new ShareableVolumeVmInstanceRefVO();
            vo.setUuid(Platform.getUuid());
            vo.setVmInstanceUuid(vm.getUuid());
            vo.setVolumeUuid(volume.getUuid());
            vo.setDeviceId(new NextVolumeDeviceIdGetter().getNextVolumeDeviceId(vm.getUuid()));
            dbf.persist(vo);
        }
    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {

    }

    @VolumeSnapshotGroupOperationValidator.VolumeSnapshotGroupCreationValidatorMethod
    public static void validate(String vmUuid) {
        List<String> sharedVolUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vmUuid)
                .select(ShareableVolumeVmInstanceRefVO_.volumeUuid)
                .listValues();
        if (!sharedVolUuids.isEmpty()) {
            throw new OperationFailureException(operr("shareable volume(s)[uuid: %s] attached," +
                    " not support to group snapshot.", sharedVolUuids));
        }
    }

    @Override
    public void beforeGetNextVolumeDeviceId(String vmUuid, List<Integer> devIds) {
        SimpleQuery<ShareableVolumeVmInstanceRefVO> q2 = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
        q2.select(ShareableVolumeVmInstanceRefVO_.deviceId);
        q2.add(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, Op.EQ, vmUuid);
        List<Integer> tmpList = q2.listValue();
        if (tmpList != null && !tmpList.isEmpty()) {
            devIds.addAll(tmpList);
        }
    }

    @Override
    public void afterCreateVolume(VolumeVO vo) {
        String volumeQosStr;
        if (vo.getType() == VolumeType.Root) {
            VmInstanceVO vm = dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class);
            if (vm == null || vm.getInstanceOfferingUuid() == null) {
                return;
            }
            VolumeQosHelper.copyQosFromOffering(vm.getInstanceOfferingUuid(), InstanceOfferingVO.class, vo.getUuid());
            VolumeQosHelper.copyIopsQosFromOffering(vm.getInstanceOfferingUuid(), InstanceOfferingVO.class,
                    vo.getUuid());
            volumeQosStr = VolumeQosHelper.getVolumeQosStr(vm.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        } else {
            if (vo.getDiskOfferingUuid() == null) {
                return;
            }
            VolumeQosHelper.copyQosFromOffering(vo.getDiskOfferingUuid(), DiskOfferingVO.class, vo.getUuid());
            VolumeQosHelper.copyIopsQosFromOffering(vo.getDiskOfferingUuid(), DiskOfferingVO.class,
                    vo.getUuid());
            volumeQosStr = VolumeQosHelper.getVolumeQosStr(vo.getDiskOfferingUuid(), DiskOfferingVO.class);
        }

        if (!volumeQosStr.isEmpty()) {
            vo.setVolumeQos(volumeQosStr);
            dbf.updateAndRefresh(vo);
        }
    }

    @Override
    public void preCreateVolume(VolumeCreateMessage msg) {
    }

    @Override
    public void beforeCreateVolume(VolumeInventory volume) {
    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (volume.isShareable()) {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            q.add(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, Op.EQ, vm.getUuid());
            q.add(ShareableVolumeVmInstanceRefVO_.volumeUuid, Op.EQ, volume.getUuid());
            List<ShareableVolumeVmInstanceRefVO> list = q.list();
            dbf.removeCollection(list, ShareableVolumeVmInstanceRefVO.class);
        }
        completion.success();
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {

    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        return Collections.singletonList(GetVmConsoleAddressFromHostReply.class);
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        if (replyOrEvent instanceof GetVmConsoleAddressFromHostReply) {
            marshal((GetVmConsoleAddressFromHostReply) replyOrEvent,
                    (GetVmConsoleAddressFromHostMsg) msg);
        }
    }

    private String getDisplayNetworkIp(final String hostUuid, final String clusterUuid) {
        if (clusterUuid == null) {
            logger.warn(String.format("Host [uuid: %s] has no cluster", hostUuid));
            return null;
        }

        final String displayNetworkCidr = MevocoSystemTags.CLUSTER_DISPLAY_NETWORK.getTokenByResourceUuid(
                clusterUuid,
                MevocoSystemTags.CLUSTER_DISPLAY_NETWORK_TOKEN
        );

        if (displayNetworkCidr == null) {
            return null;
        }

        final String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(
                hostUuid,
                HostSystemTags.EXTRA_IPS_TOKEN
        );

        if (extraIps == null) {
            return null;
        }

        final String[] ips = extraIps.split(",");
        for (String ip: ips) {
            if (NetworkUtils.isIpv4InCidr(ip, displayNetworkCidr)) {
                return ip;
            }
        }

        logger.warn(String.format("Cluster [uuid: %s] configured display network %s, but host [uuid: %s] has no matching IP",
                        clusterUuid, displayNetworkCidr, hostUuid));
        return null;
    }

    private void marshal(GetVmConsoleAddressFromHostReply reply, GetVmConsoleAddressFromHostMsg msg) {
        if (!reply.isSuccess()) {
            return;
        }

        final String clusterUuid = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .select(HostVO_.clusterUuid)
                .findValue();

        final String ip = getDisplayNetworkIp(msg.getHostUuid(), clusterUuid);
        if (ip != null) {
            reply.setHostIp(ip);
        }
    }

    public LinkedHashMap kvmBeforeAsyncJsonPostExtensionPoint(String path, LinkedHashMap commandMap, Map header) {
        StringBuilder options = new StringBuilder(String.format(" -o cluster_size=%s ",
                MevocoGlobalConfig.QCOW2_CLUSTER_SIZE.value()));

        if (commandMap.containsKey(MevocoConstants.PRIMARYSTORAGE_UUID)) {
            String primaryStorageUuid = (String) commandMap.get(MevocoConstants.PRIMARYSTORAGE_UUID);
            if (StringUtils.isNotEmpty(primaryStorageUuid)) {
                String preallocation = getPreallocationCommand(primaryStorageUuid);
                if (StringUtils.isNotEmpty(preallocation)) {
                    options.append(String.format(" -o preallocation=%s ", preallocation));
                }

            }
        }

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(MevocoConstants.QCOW2_OPTIONS, options);
        return linkedHashMap;
    }

    private String getPreallocationCommand(String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            return null;
        }

        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(uuid, PrimaryStorageVO.class);
        if (primaryStorageVO == null) {
            return null;
        }

        PrimaryStorageType type = PrimaryStorageType.valueOf(primaryStorageVO.getType());
        String preallocation = getPreallocationFactory(type).getPreallocation(uuid);

        if (StringUtils.isNotEmpty(preallocation) && !preallocation.equals("none")) {
            return preallocation;
        }
        return null;
    }

    public PreallocationFactory getPreallocationFactory(PrimaryStorageType type) {
        PreallocationFactory factory = preallocationFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No PreallocationFactory for type: %s found", type));
        }
        return factory;
    }

    @Override
    public SshFileMd5Checker getSshFileMd5Checker(KVMHostVO kvmHostVO) {
        SshFileMd5Checker spiceChecker = new SshFileMd5Checker();
        spiceChecker.setUsername(kvmHostVO.getUsername());
        spiceChecker.setPassword(kvmHostVO.getPassword());
        spiceChecker.setSshPort(kvmHostVO.getPort());
        spiceChecker.setTargetIp(kvmHostVO.getManagementIp());
        if (MevocoGlobalConfig.ENABLE_SPICE_CHANNEL_SUPPORT_TLS.value(Integer.class) > 0) {
            spiceChecker.addSrcDestPair((PathUtil.join(PathUtil.getZStackHomeFolder(), "ansible", "files", "kvm") + "/spice-certs/ca-cert.pem"),
                    MevocoGlobalProperty.REGISTRY_CERTS);
        } else {
            spiceChecker.addSrcDestPair((PathUtil.join(PathUtil.getZStackHomeFolder(), "ansible", "files", "kvm") + "/qemu.conf"),
                    MevocoGlobalProperty.QEMU_CONF);
        }
        return spiceChecker;
    }

    @Override
    public List<String> getVmUuidFromShareableVolumeByPrimaryStorage(PrimaryStorageDetachStruct s) {
        String sql = "select distinct vm.uuid" +
                " from VmInstanceVO vm, ShareableVolumeVmInstanceRefVO svv, VolumeVO vol" +
                " where vm.type = :vmType" +
                " and vm.state in (:vmStates)" +
                " and vm.clusterUuid = :clusterUuid" +
                " and vm.uuid = svv.vmInstanceUuid" +
                " and vol.primaryStorageUuid = :psUuid" +
                " and vol.uuid = svv.volumeUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
        q.setParameter("vmStates", Arrays.asList(
                VmInstanceState.Unknown,
                VmInstanceState.Running,
                VmInstanceState.Pausing,
                VmInstanceState.Paused));
        q.setParameter("clusterUuid", s.getClusterUuid());
        q.setParameter("psUuid", s.getPrimaryStorageUuid());
        return q.getResultList();
    }

    @Override
    public void afterForceLogoutSession(IdentityCanonicalEvents.SessionForceLogoutData data) {
        SNSPublishMsg msg = new SNSPublishMsg();
        bus.makeLocalServiceId(msg, SNSConstants.SERVICE_ID);
        msg.setTopicUuid(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID);
        msg.setMessage(Collections.singletonMap(SNSSystemHttpEndpointFactory.type.toString(), JSONObjectUtil.toJsonString(data)));
        msg.setMetadata(Collections.singletonMap(SNSSystemHttpEndpointFactory.type.toString(), Collections.emptyMap()));
        bus.send(msg);
    }

    @Override
    public void saveQcow2VolumeProvisioningStrategy(VolumeInventory volume, boolean hasBackingFile) {
        if (VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(volume.getUuid())) {
            return;
        }

        String volumeProvisioningStrategy = hasBackingFile ? VolumeProvisioningStrategy.ThinProvisioning.toString() : getPrimaryStorageProvisioningStrategy(volume.getPrimaryStorageUuid());
        SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volume.getUuid());
        tagCreator.setTagByTokens(
                map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, volumeProvisioningStrategy))
        );
        tagCreator.inherent = false;
        tagCreator.create();
    }

    public String getPrimaryStorageProvisioningStrategy(String primaryStorageUuid) {
        PrimaryStorageType type = PrimaryStorageType.valueOf(dbf.findByUuid(primaryStorageUuid, PrimaryStorageVO.class).getType());
        return getPreallocationFactory(type).getProvisioningStrategy(primaryStorageUuid);
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICloneVmInstanceMsg.class)
                .addCounterQuota(ImageQuotaConstant.IMAGE_NUM)
                .addMessageRequiredQuotaHandler(ImageQuotaConstant.IMAGE_SIZE,(msg) -> new VmQuotaUtil()
                        .getVmInstanceRootVolumeSize(msg.getVmInstanceUuid()))
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_NUM, (msg) -> {
                    if (msg.getStrategy().equals(VmCreationStrategy.InstantStart.toString())
                            || msg.getStrategy().equals(VmCreationStrategy.CreatedPaused.toString())) {
                        return (long) msg.getNames().size();
                    }

                    return 0L;
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_CPU_NUM, (msg) -> {
                    if (msg.getStrategy().equals(VmCreationStrategy.InstantStart.toString())
                            || msg.getStrategy().equals(VmCreationStrategy.CreatedPaused.toString())) {
                        Integer cpuNum = Q.New(VmInstanceVO.class)
                                .select(VmInstanceVO_.cpuNum)
                                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                                .findValue();
                        return (long) cpuNum * msg.getNames().size();
                    }

                    return 0L;
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE, (msg) -> {
                    if (msg.getStrategy().equals(VmCreationStrategy.InstantStart.toString())
                            || msg.getStrategy().equals(VmCreationStrategy.CreatedPaused.toString())) {
                        Long memorySize = Q.New(VmInstanceVO.class)
                                .select(VmInstanceVO_.memorySize)
                                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                                .findValue();
                        return memorySize * msg.getNames().size();
                    }

                    return 0L;
                }).addMessageRequiredQuotaHandler(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM, (msg) -> {
                    if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                            .eq(VmInstanceVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).isExists()) {
                        return 0L;
                    }

                    long requiredSnapshotNum;
                    if (msg.getFull()) {
                        requiredSnapshotNum = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid()).count();
                        if (requiredSnapshotNum == 0) {
                            throw new CloudRuntimeException(String.format("No Volume of vm[uuid:%s] can be found.",
                                    msg.getVmInstanceUuid()));
                        }
                    } else {
                        requiredSnapshotNum = 1;
                    }

                    return requiredSnapshotNum;
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VOLUME_SIZE, (msg) -> {
                    if (msg.getStrategy().equals(VmCreationStrategy.JustCreate.toString())) {
                        return null;
                    }

                    VolumeVO vvo = Q.New(VolumeVO.class)
                            .eq(VolumeVO_.type, VolumeType.Root)
                            .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                            .find();
                    if (vvo == null) {
                        throw new CloudRuntimeException(String.format("Root Volume of vm[uuid:%s] not found.",
                                msg.getVmInstanceUuid()));
                    }

                    return msg.getNames().size() * vvo.getSize();
                }));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeVmImageMsg.class)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VOLUME_SIZE, (msg) -> Q.New(ImageVO.class)
                        .select(ImageVO_.size)
                        .eq(ImageVO_.uuid, msg.getImageUuid())
                        .findValue()));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIResizeDataVolumeMsg.class)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VOLUME_SIZE, (msg) -> {
                    VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
                    long originVolumeSize = vo.getSize();
                    if (!(msg.getSize() > originVolumeSize)) {
                        return 0L;
                    }

                    return msg.getSize() - originVolumeSize;
                }));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIResizeRootVolumeMsg.class)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VOLUME_SIZE, (msg) -> {
                    VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
                    long originVolumeSize = vo.getSize();
                    if (!(msg.getSize() > originVolumeSize)) {
                        return 0L;
                    }

                    return msg.getSize() - originVolumeSize;
                }));

        return list(quota);
    }

    @Override
    public List<String> appendExtraPackages(HostInventory host) {
        return null;
    }

    @Override
    public void modifyDeploymentArguments(HostInventory host, KVMHostDeployArguments args) {
        for (QemuConfigItemOperator qemuConfigItem : qemuConfigItems) {
            String status = qemuConfigItem.getStatus(host);
            boolean isEnabled = qemuConfigItem.isEnabled();
            if (status != null && status.equals(String.valueOf(isEnabled))) {
                qemuConfigItem.applyConfig(args);
                continue;
            }
            qemuConfigItem.applyConfig(args);
            args.setForceRun(true);
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new Flow() {
            String __name__ = "record-host-configuration-state";

            Map<QemuConfigItemOperator, Boolean> updatedStatusMap = new HashMap<>();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (QemuConfigItemOperator qemuConfigItem : qemuConfigItems) {
                    String status = qemuConfigItem.getStatus(context.getInventory());
                    boolean isEnabled = qemuConfigItem.isEnabled();
                    if (status == null || Boolean.parseBoolean(status) != isEnabled) {
                        qemuConfigItem.createOrUpdateTag(context.getInventory().getUuid());
                        updatedStatusMap.put(qemuConfigItem, true);
                    }
                }

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                for (Map.Entry<QemuConfigItemOperator, Boolean> entry : updatedStatusMap.entrySet()) {
                    if (entry.getValue()) {
                        entry.getKey().rollbackTag(context.getInventory().getUuid());
                    }
                }
                trigger.rollback();
            }
        };
    }
}
