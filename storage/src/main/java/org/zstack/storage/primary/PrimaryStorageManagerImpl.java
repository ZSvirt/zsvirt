package org.zstack.storage.primary;

import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.InstanceOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.*;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfigValidator;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfig;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfigValidator;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.trash.InstallPathRecycleVO;
import org.zstack.header.core.trash.InstallPathRecycleVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.extension.VmInstancePreStartExtensionPoint;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.header.vm.*;
import org.zstack.resourceconfig.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class PrimaryStorageManagerImpl extends AbstractService implements PrimaryStorageManager,
        ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint, VmInstanceStartExtensionPoint,
        VmInstancePreStartExtensionPoint,
        VmInstanceCreateExtensionPoint, InstanceOfferingUserConfigValidator, DiskOfferingUserConfigValidator,
        PrimaryStorageSortExtensionPoint, PrimaryStorageFeatureAllocatorExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    private PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    protected PrimaryStorageUsageReport primaryStorageUsageForecaster;

    private final Map<String, PrimaryStorageFactory> primaryStorageFactories = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, PrimaryStorageAllocatorStrategyFactory> allocatorFactories = Collections.synchronizedMap(new HashMap<>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<>();
    private final Map<String, AutoDeleteTrashTask> autoDeleteTrashTask = new HashMap<>();
    private static final Map<String, List<PrimaryStorageExtensionFactory>> extensionFactories = Maps.newConcurrentMap();
    private AutoDeleteTrashTask globalTrashTask;
    private Future refreshPrimaryStorageHostStatusTask;

    static {
        allowedMessageAfterSoftDeletion.add(PrimaryStorageDeletionMsg.class);
    }

    void init() {
        PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(
                        systemTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG_TOKEN);
                if (!StringDSL.isZStackUuid(uuid)) {
                    throw new ApiMessageInterceptionException(argerr("%s is invalid. %s is not a valid zstack uuid", systemTag, uuid));
                }

                if (!dbf.isExist(uuid, PrimaryStorageVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("no primary storage[uuid:%s] found", resourceUuid));
                }
            }
        });
    }

    public PrimaryStorageFactory getPrimaryStorageFactory(String type) {
        return primaryStorageFactories.get(type);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddPrimaryStorageMsg) {
            handle((APIAddPrimaryStorageMsg) msg);
        } else if (msg instanceof PrimaryStorageMessage) {
            passThrough((PrimaryStorageMessage) msg);
        } else if (msg instanceof APIGetPrimaryStorageTypesMsg) {
            handle((APIGetPrimaryStorageTypesMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageAllocatorStrategiesMsg) {
            handle((APIGetPrimaryStorageAllocatorStrategiesMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageCapacityMsg) {
            handle((APIGetPrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageLicenseInfoMsg) {
            handle((APIGetPrimaryStorageLicenseInfoMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageUsageReportMsg) {
            handle((APIGetPrimaryStorageUsageReportMsg) msg);
        } else if (msg instanceof APIDiscoverStrangePrimaryStorageMsg) {
            handle((APIDiscoverStrangePrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetPrimaryStorageLicenseInfoMsg msg) {
        APIGetPrimaryStorageLicenseInfoReply sreply = new APIGetPrimaryStorageLicenseInfoReply();
        PrimaryStorageVO vo = dbf.findByUuid(msg.getUuid(), PrimaryStorageVO.class);
        if (vo == null) {
            sreply.setError(operr("primaryStorage[uuid=%s] does not exist", msg.getUuid()));
            bus.reply(msg, sreply);
            return;
        }
        if (!PrimaryStorageSystemTags.PRIMARY_STORAGE_VENDOR.hasTag(msg.getUuid())) {
            bus.reply(msg, sreply);
            return;
        }

        GetPrimaryStorageLicenseInfoMsg gmsg = new GetPrimaryStorageLicenseInfoMsg();
        gmsg.setPrimaryStorageUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, msg.getUuid());
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    GetPrimaryStorageLicenseInfoReply r = reply.castReply();
                    PrimaryStorageLicenseInfo info = r.getPrimaryStorageLicenseInfo();
                    if (info != null) {
                        sreply.setUuid(msg.getUuid());
                        sreply.setExpireTime(info.getExpireTime().toString());
                        sreply.setName(vo.getName());
                    }
                } else {
                    sreply.setError(reply.getError());
                }
                bus.reply(msg, sreply);
            }
        });
    }

    private void handle(final APIGetPrimaryStorageCapacityMsg msg) {
        APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();

        Tuple ret = new Callable<Tuple>() {
            @Override
            @Transactional(readOnly = true)
            public Tuple call() {
                if (msg.getPrimaryStorageUuids() != null && !msg.getPrimaryStorageUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity)," +
                            " sum(psc.availableCapacity)," +
                            " sum(psc.totalPhysicalCapacity)," +
                            " sum(psc.availablePhysicalCapacity)" +
                            " from PrimaryStorageCapacityVO psc" +
                            " where psc.uuid in (:psUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", msg.getPrimaryStorageUuids());
                    return q.getSingleResult();
                } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity)," +
                            " sum(psc.availableCapacity)," +
                            " sum(psc.totalPhysicalCapacity)," +
                            " sum(psc.availablePhysicalCapacity)" +
                            " from PrimaryStorageCapacityVO psc" +
                            " where psc.uuid in" +
                            " (" +
                            " select distinct ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref" +
                            " where ref.clusterUuid in (:clusterUuids)" +
                            " )";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("clusterUuids", msg.getClusterUuids());
                    return q.getSingleResult();
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity)," +
                            " sum(psc.availableCapacity)," +
                            " sum(psc.totalPhysicalCapacity)," +
                            " sum(psc.availablePhysicalCapacity)" +
                            " from PrimaryStorageCapacityVO psc, PrimaryStorageVO ps" +
                            " where ps.uuid = psc.uuid" +
                            " and ps.zoneUuid in (:zoneUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    return q.getSingleResult();
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        Long total = ret.get(0, Long.class);
        Long avail = ret.get(1, Long.class);
        Long ptotal = ret.get(2, Long.class);
        Long pavail = ret.get(3, Long.class);
        reply.setTotalCapacity(total == null ? 0 : total);
        reply.setAvailableCapacity(avail == null ? 0 : avail);
        reply.setTotalPhysicalCapacity(ptotal == null ? 0 : ptotal);
        reply.setAvailablePhysicalCapacity(pavail == null ? 0 : pavail);
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageAllocatorStrategiesMsg msg) {
        APIGetPrimaryStorageAllocatorStrategiesReply reply = new APIGetPrimaryStorageAllocatorStrategiesReply();
        reply.setPrimaryStorageAllocatorStrategies(PrimaryStorageAllocatorStrategyType.getAllExposedTypeNames());
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageTypesMsg msg) {
        APIGetPrimaryStorageTypesReply reply = new APIGetPrimaryStorageTypesReply();
        List<String> ret = new ArrayList<>(PrimaryStorageType.getAllTypeNames());
        reply.setPrimaryStorageTypes(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageUsageReportMsg msg) {
        APIGetPrimaryStorageUsageReportReply reply = new APIGetPrimaryStorageUsageReportReply();

        if (CollectionUtils.isEmpty(msg.getUris())) {
            reply.setUsageReport(primaryStorageUsageForecaster.getUsageReportByResourceUuids(
                    Collections.singletonList(msg.getPrimaryStorageUuid())).get(msg.getPrimaryStorageUuid()));
            bus.reply(msg, reply);
            return;
        }

        GetPrimaryStorageUsageReportMsg gmsg = new GetPrimaryStorageUsageReportMsg();
        gmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        gmsg.setUris(msg.getUris());

        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, gmsg.getPrimaryStorageUuid());
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                } else {
                    GetPrimaryStorageUsedPhysicalCapacityForecastReply re = r.castReply();
                    reply.setUriUsageForecast(re.getUsageReportMap());
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIDiscoverStrangePrimaryStorageMsg msg) {
        APIDiscoverStrangePrimaryStorageReply reply = new APIDiscoverStrangePrimaryStorageReply();

        List<PrimaryStorageDiscoverExtensionPoint> exts = pluginRgty.getExtensionList(PrimaryStorageDiscoverExtensionPoint.class);
        if (exts.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        new While<>(exts).all((ext, whileCompletion) -> {
            ext.discoverStrangePrimaryStorage(msg.getClusterUuid(), new ReturnValueCompletion<PrimaryStorageDiscoveryResult>(whileCompletion) {
                @Override
                public void success(PrimaryStorageDiscoveryResult result) {
                    if (result != null) {
                        synchronized (reply) {
                            if (result.getInventories() != null) {
                                reply.getInventories().addAll(result.getInventories());
                            }
                            if (result.getResetVgUuidRequiredUuids() != null) {
                                reply.getResetVgUuidRequiredUuids().addAll(result.getResetVgUuidRequiredUuids());
                            }
                        }
                    }
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to discover strange primary storage from extension[%s]: %s",
                            ext.getClass().getSimpleName(), errorCode));
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void passThrough(PrimaryStorageMessage pmsg) {
        PrimaryStorageVO vo = dbf.findByUuid(pmsg.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(pmsg.getClass())) {
            PrimaryStorageEO eo = dbf.findByUuid(pmsg.getPrimaryStorageUuid(), PrimaryStorageEO.class);
            vo = ObjectUtils.newAndCopy(eo, PrimaryStorageVO.class);
        }

        Message msg = (Message) pmsg;
        if (vo == null) {
            bus.replyErrorByMessageType(msg, err(SysErrors.RESOURCE_NOT_FOUND, "Cannot find primary storage[uuid:%s], it may have been deleted", pmsg.getPrimaryStorageUuid()));
            return;
        }

        PrimaryStorageVO finalVo = vo;

        PrimaryStorageFactory factory = getPrimaryStorageFactory(PrimaryStorageType.valueOf(vo.getType()));
        if (extensionFactories.containsKey(factory.getPrimaryStorageType().toString())) {
            PrimaryStorageExtensionFactory extFactory = extensionFactories.get(factory.getPrimaryStorageType().toString()).stream().filter(it -> it.getMessageClasses()
                    .stream().anyMatch(clz -> clz.isAssignableFrom(msg.getClass()))).findFirst().orElse(null);
            if (extFactory != null) {
                PrimaryStorage ps = Platform.New(()-> extFactory.getPrimaryStorage(finalVo));
                ps.handleMessage(msg);
                return;
            }
        }
        PrimaryStorage ps = Platform.New(()-> factory.getPrimaryStorage(finalVo));
        ps.handleMessage(msg);
    }

    @Deferred
    private void handle(final APIAddPrimaryStorageMsg msg) {
        PrimaryStorageType type = PrimaryStorageType.valueOf(msg.getType());
        final PrimaryStorageFactory factory = getPrimaryStorageFactory(type);

        PrimaryStorageVO primaryStorageVO = new PrimaryStorageVO();
        if (msg.getResourceUuid() != null) {
            primaryStorageVO.setUuid(msg.getResourceUuid());
        } else {
            primaryStorageVO.setUuid(Platform.getUuid());
        }
        primaryStorageVO.setUrl(msg.getUrl());
        primaryStorageVO.setType(type.toString());
        primaryStorageVO.setName(msg.getName());
        primaryStorageVO.setDescription(msg.getDescription());
        primaryStorageVO.setState(PrimaryStorageState.Enabled);
        primaryStorageVO.setStatus(PrimaryStorageStatus.Connecting);
        primaryStorageVO.setZoneUuid(msg.getZoneUuid());

        final APIAddPrimaryStorageEvent evt = new APIAddPrimaryStorageEvent(msg.getId());
        final PrimaryStorageInventory inv = factory.createPrimaryStorage(primaryStorageVO, msg);
        primaryStorageVO = dbf.findByUuid(primaryStorageVO.getUuid(), PrimaryStorageVO.class);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), PrimaryStorageVO.class.getSimpleName());

        PrimaryStorageCapacityVO primaryStorageCapacityVO = dbf.findByUuid(primaryStorageVO.getUuid(), PrimaryStorageCapacityVO.class);
        if (primaryStorageCapacityVO == null) {
            primaryStorageCapacityVO = new PrimaryStorageCapacityVO();
            primaryStorageCapacityVO.setUuid(primaryStorageVO.getUuid());
            dbf.persist(primaryStorageCapacityVO);
        }

        final ConnectPrimaryStorageMsg cmsg = new ConnectPrimaryStorageMsg();
        cmsg.setPrimaryStorageUuid(inv.getUuid());
        cmsg.setNewAdded(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, inv.getUuid());
        final PrimaryStorageVO finalVo = primaryStorageVO;
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess() && !reply.getError().isError(PrimaryStorageErrors.DISCONNECTED)) {
                    evt.setError(reply.getError());
                    logger.warn(String.format("failed to connect primary storage[uuid:%s, name:%s, url:%s]",
                            finalVo.getUuid(), finalVo.getName(), finalVo.getUrl()));
                    dbf.remove(finalVo);
                } else {
                    PrimaryStorageInventory pinv = factory.getInventory(finalVo.getUuid());
                    logger.debug(String.format("successfully add primary storage[uuid:%s, name:%s, url: %s]",
                            finalVo.getUuid(), finalVo.getName(), finalVo.getUrl()));
                    evt.setInventory(pinv);
                }

                bus.publish(evt);
            }
        });

    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AllocatePrimaryStorageSpaceMsg) {
            handle((AllocatePrimaryStorageSpaceMsg) msg);
        } else if (msg instanceof AllocatePrimaryStorageMsg) {
            handle((AllocatePrimaryStorageMsg) msg);
        } else if (msg instanceof ReleasePrimaryStorageSpaceMsg) {
            handle((ReleasePrimaryStorageSpaceMsg) msg);
        } else if (msg instanceof IncreasePrimaryStorageCapacityMsg) {
            handle((IncreasePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof DecreasePrimaryStorageCapacityMsg) {
            handle((DecreasePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof RecalculatePrimaryStorageCapacityMsg) {
            handle((RecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof PrimaryStorageMessage) {
            passThrough((PrimaryStorageMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final RecalculatePrimaryStorageCapacityMsg msg) {
        RecalculatePrimaryStorageCapacityReply reply = new RecalculatePrimaryStorageCapacityReply();

        final List<String> psUuids = new ArrayList<>();

        if (msg.getPrimaryStorageUuid() != null) {
            passThrough(msg);
        } else if (msg.getZoneUuid() != null) {
            List<String> uuids = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.uuid)
                    .eq(PrimaryStorageVO_.zoneUuid, msg.getZoneUuid())
                    .listValues();
            psUuids.addAll(uuids);

            PrimaryStorageCapacityRecalculator psRecal = new PrimaryStorageCapacityRecalculator();
            psRecal.psUuids = psUuids;
            psRecal.recalculate();
            bus.reply(msg, reply);
        }
    }

    private void handle(IncreasePrimaryStorageCapacityMsg msg) {
        long diskSize = msg.isNoOverProvisioning() ? msg.getDiskSize() : ratioMgr.calculateByRatio(msg.getPrimaryStorageUuid(), msg.getDiskSize());
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(msg.getPrimaryStorageUuid());
        if (updater.increaseAvailableCapacity(diskSize)) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Successfully return %s bytes to primary storage[uuid:%s]",
                        diskSize, msg.getPrimaryStorageUuid()));
            }
        }
    }

    private void handle(DecreasePrimaryStorageCapacityMsg msg) {
        long diskSize = msg.isNoOverProvisioning() ? msg.getDiskSize() : ratioMgr.calculateByRatio(msg.getPrimaryStorageUuid(), msg.getDiskSize());
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(msg.getPrimaryStorageUuid());
        if (updater.decreaseAvailableCapacity(diskSize)) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Successfully return %s bytes to primary storage[uuid:%s]",
                        diskSize, msg.getPrimaryStorageUuid()));
            }
        }
    }

    /**
     * Supported allocation strategy：
     * DefaultPrimaryStorageAllocationStrategy (only work for non-local primary storage)
     * LocalPrimaryStorageStrategy (only work for local primary storage)
     *
     * Note：
     * If the allocation strategy is not specified
     * If the cluster is mounted with local storage, the default is LocalPrimaryStorageStrategy。
     * Otherwise, it is DefaultPrimaryStorageAllocationStrategy
     */
    private void handle(AllocatePrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                allocatePrimaryStore(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "allocate-primary-store";
            }

            @Override
            protected int getSyncLevel() {
                return PrimaryStorageGlobalConfig.ALLOCATE_PRIMARYSTORAGE_CONCURRENCY.value(Integer.class);
            }
        });
    }

    private void handle(AllocatePrimaryStorageSpaceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                allocatePrimaryStoreSpace(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "allocate-primary-store-space";
            }

            @Override
            protected int getSyncLevel() {
                return PrimaryStorageGlobalConfig.ALLOCATE_PRIMARYSTORAGE_CONCURRENCY.value(Integer.class);
            }
        });
    }

    private void handle(ReleasePrimaryStorageSpaceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                releasePrimaryStorageSpaceMsg(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "release-primary-store-space";
            }

            @Override
            protected int getSyncLevel() {
                return PrimaryStorageGlobalConfig.ALLOCATE_PRIMARYSTORAGE_CONCURRENCY.value(Integer.class);
            }
        });
    }

    private void allocatePrimaryStoreSpace(AllocatePrimaryStorageSpaceMsg msg, NoErrorCompletion completion) {
        AllocatePrimaryStorageSpaceReply reply = new AllocatePrimaryStorageSpaceReply(null);
        if (msg.getRequiredInstallUri() == null && msg.getPurpose() == null) {
            throw new OperationFailureException(
                    argerr("please specify the purpose before allocating space"));
        }

        if (msg.getRequiredInstallUri() != null && msg.isForce()) {
            PrimaryStorageVO psVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getRequiredPrimaryStorageUuid()).find();
            PrimaryStorageInventory psInv = PrimaryStorageInventory.valueOf(psVO);
            PSCapacityExtensionPoint PSCapacityExt = pluginRgty.getExtensionFromMap(psInv.getType(), PSCapacityExtensionPoint.class);
            if (PSCapacityExt != null) {
                long requiredSize = msg.getSize();
                if (!msg.isNoOverProvisioning()) {
                    requiredSize = ratioMgr.calculateByRatio(psInv.getUuid(), requiredSize);
                }
                String allocatedInstallUrl = forceReserveSpace(psInv, requiredSize, msg);
                reply.setAllocatedInstallUrl(allocatedInstallUrl);
                reply.setPrimaryStorageInventory(psInv);
                reply.setSize(msg.getSize());
                bus.reply(msg, reply);
                completion.done();
                return;
            }
        }
        String allocatorStrategyType = getAllocateStrategyFromMsg(msg);
        PrimaryStorageAllocatorStrategyFactory factory = getPrimaryStorageAllocatorStrategyFactory(
                PrimaryStorageAllocatorStrategyType.valueOf(allocatorStrategyType));
        PrimaryStorageAllocatorStrategy strategy = factory.getPrimaryStorageAllocatorStrategy();
        PrimaryStorageAllocationSpec spec = buildAllocateSpecFromMsg(msg);
        List<PrimaryStorageInventory> ret = strategy.allocateAllCandidates(spec);

        if (msg.isDryRun()) {
            // check capacity has been done before
            AllocatePrimaryStorageDryRunReply r = new AllocatePrimaryStorageDryRunReply();
            r.setPrimaryStorageInventories(ret);
            bus.reply(msg, r);
            completion.done();
            return;
        }
        Iterator<PrimaryStorageInventory> it = ret.iterator();
        List<String> errs = new ArrayList<>();
        PrimaryStorageInventory target = null;
        while (it.hasNext()) {
            PrimaryStorageInventory psInv = it.next();

            if (!physicalCapacityMgr.checkCapacityByRatio(psInv.getUuid(), psInv.getTotalPhysicalCapacity(), psInv.getAvailablePhysicalCapacity())) {
                errs.add(String.format("primary storage[uuid:%s]'s physical capacity usage has exceeded the threshold[%s]",
                        psInv.getUuid(), physicalCapacityMgr.getRatio(psInv.getUuid())));
                continue;
            }

            long requiredSize = spec.getSize();
            if (!msg.isNoOverProvisioning()) {
                requiredSize = ratioMgr.calculateByRatio(psInv.getUuid(), requiredSize);
            }
            String allocatedInstallUrl;
            allocatedInstallUrl = reserveSpace(psInv, requiredSize, msg);
            if (allocatedInstallUrl != null) {
                target = psInv;
                reply.setAllocatedInstallUrl(allocatedInstallUrl);
                break;
            } else {
                errs.add(String.format("unable to reserve capacity on the primary storage[uuid:%s], it has no space", psInv.getUuid()));
                logger.debug(String.format("concurrent reservation on the primary storage[uuid:%s], try next one", psInv.getUuid()));
            }
        }

        if (target == null) {
            throw new OperationFailureException(operr("cannot find any qualified primary storage, errors are %s", errs));
        }

        reply.setPrimaryStorageInventory(target);
        reply.setSize(msg.getSize());
        bus.reply(msg, reply);
        completion.done();
    }

    private void allocatePrimaryStore(AllocatePrimaryStorageMsg msg, NoErrorCompletion completion) {
        AllocatePrimaryStorageReply reply = new AllocatePrimaryStorageReply(null);

        String allocatorStrategyType = getAllocateStrategyFromMsg(msg);

        PrimaryStorageAllocatorStrategyFactory factory = getPrimaryStorageAllocatorStrategyFactory(
                PrimaryStorageAllocatorStrategyType.valueOf(allocatorStrategyType));
        PrimaryStorageAllocatorStrategy strategy = factory.getPrimaryStorageAllocatorStrategy();
        PrimaryStorageAllocationSpec spec = buildAllocateSpecFromMsg(msg);
        List<PrimaryStorageInventory> ret = strategy.allocateAllCandidates(spec);

        if (msg.isDryRun()) {
            // check capacity has been done before
            AllocatePrimaryStorageDryRunReply r = new AllocatePrimaryStorageDryRunReply();
            r.setPrimaryStorageInventories(ret);
            bus.reply(msg, r);
            completion.done();
            return;
        }
        Iterator<PrimaryStorageInventory> it = ret.iterator();
        List<String> errs = new ArrayList<>();
        PrimaryStorageInventory target = null;
        while (it.hasNext()) {
            PrimaryStorageInventory psInv = it.next();

            if (!physicalCapacityMgr.checkCapacityByRatio(psInv.getUuid(), psInv.getTotalPhysicalCapacity(), psInv.getAvailablePhysicalCapacity())) {
                errs.add(String.format("primary storage[uuid:%s]'s physical capacity usage has exceeded the threshold[%s]",
                        psInv.getUuid(), physicalCapacityMgr.getRatio(psInv.getUuid())));
                continue;
            }

            long requiredSize = spec.getSize();
            if (!msg.isNoOverProvisioning()) {
                requiredSize = ratioMgr.calculateByRatio(psInv.getUuid(), requiredSize);
            }

            if (reserve(psInv, requiredSize)) {
                target = psInv;
                break;
            } else {
                errs.add(String.format("unable to reserve capacity on the primary storage[uuid:%s], it has no space", psInv.getUuid()));
                logger.debug(String.format("concurrent reservation on the primary storage[uuid:%s], try next one", psInv.getUuid()));
            }
        }

        if (target == null) {
            throw new OperationFailureException(operr("cannot find any qualified primary storage, errors are %s", errs));
        }

        reply.setPrimaryStorageInventory(target);
        reply.setSize(msg.getSize());
        bus.reply(msg, reply);

        completion.done();
    }

    private boolean reserve(final PrimaryStorageInventory inv, final long size) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(inv.getUuid());
        return updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                long avail = cap.getAvailableCapacity() - size;
                if (avail < 0) {
                    logger.warn(String.format("[Primary Storage Allocation] reserved capacity on primary storage[uuid:%s] failed," +
                            " no available capacity on it", inv.getUuid()));
                    return null;
                }

                long origin = cap.getAvailableCapacity();
                cap.setAvailableCapacity(avail);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[Primary Storage Allocation] reserved %s bytes on primary storage[uuid:%s," +
                            " available before:%s, available now:%s]", size, inv.getUuid(), origin, avail));
                }

                return cap;
            }
        });
    }

    private String reserveSpace(final PrimaryStorageInventory inv, final long size, AllocatePrimaryStorageSpaceMsg msg) {
        final String[] allocatedInstallUrl = new String[1];
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(inv.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                long avail = cap.getAvailableCapacity() - size;
                if (avail < 0) {
                    logger.warn(String.format("[Primary Storage Allocation] reserved capacity on primary storage[uuid:%s] failed," +
                            " no available capacity on it", inv.getUuid()));
                    return null;
                }

                long origin = cap.getAvailableCapacity();
                cap.setAvailableCapacity(avail);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[Primary Storage Allocation] reserved %s bytes on primary storage[uuid:%s," +
                            " available before:%s, available now:%s]", size, inv.getUuid(), origin, avail));
                }

                PSCapacityExtensionPoint PSCapacityExt = pluginRgty.getExtensionFromMap(inv.getType(), PSCapacityExtensionPoint.class);
                allocatedInstallUrl[0] = inv.getUrl();
                if (PSCapacityExt != null) {
                    allocatedInstallUrl[0] = PSCapacityExt.buildAllocatedInstallUrl(msg, inv);
                    if (allocatedInstallUrl[0] == null) {
                        return null;
                    }
                    PSCapacityExt.reserveCapacity(msg, allocatedInstallUrl[0], size, inv.getUuid());
                }
                return cap;
            }
        });
        return allocatedInstallUrl[0];
    }

    private String forceReserveSpace(final PrimaryStorageInventory inv, final long size, AllocatePrimaryStorageSpaceMsg msg) {
        final String[] allocatedInstallUrl = new String[1];
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(inv.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                long origin = cap.getAvailableCapacity();
                long capacityBeforeAllocate = 0;
                long avail = 0;

                PSCapacityExtensionPoint PSCapacityExt = pluginRgty.getExtensionFromMap(inv.getType(), PSCapacityExtensionPoint.class);
                allocatedInstallUrl[0] = PSCapacityExt.buildAllocatedInstallUrl(msg, inv);
                capacityBeforeAllocate = PSCapacityExt.reserveCapacity(msg, allocatedInstallUrl[0], size, inv.getUuid());

                long diff = capacityBeforeAllocate - size;
                avail = cap.getAvailableCapacity() - size;

                if (diff < 0) {
                    avail = cap.getAvailableCapacity() - capacityBeforeAllocate;
                }

                cap.setAvailableCapacity(avail);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[Primary Storage Allocation] reserved %s bytes on primary storage[uuid:%s," +
                            " available before:%s, available now:%s]", size, inv.getUuid(), origin, avail));
                }
                return cap;
            }
        });
        return allocatedInstallUrl[0];
    }

    @Transactional
    private void releasePrimaryStorageSpaceMsg(ReleasePrimaryStorageSpaceMsg msg, NoErrorCompletion completion) {
        long diskSize = msg.isNoOverProvisioning() ? msg.getDiskSize() : ratioMgr.calculateByRatio(msg.getPrimaryStorageUuid(), msg.getDiskSize());
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(msg.getPrimaryStorageUuid());
        if (updater.increaseAvailableCapacity(diskSize)) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Successfully return %s bytes to primary storage[uuid:%s]",
                        diskSize, msg.getPrimaryStorageUuid()));
            }
        }

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid()).findValue();
        PSCapacityExtensionPoint PSReserveCapacityExt = pluginRgty.getExtensionFromMap(psType, PSCapacityExtensionPoint.class);
        if (PSReserveCapacityExt != null) {
            PSReserveCapacityExt.releaseCapacity(msg.getAllocatedInstallUrl(), diskSize, msg.getPrimaryStorageUuid());
        }
        completion.done();
    }

    private String getAllocateStrategyFromMsg(AllocatePrimaryStorageMsg msg) {
        String allocatorStrategyType = null;

        for (PrimaryStorageAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyExtensionPoint.class)) {
            allocatorStrategyType = ext.getPrimaryStorageAllocatorStrategyName(msg);
            if (allocatorStrategyType != null) {
                break;
            }
        }

        if (allocatorStrategyType == null && msg.getDiskOfferingUuid() != null) {
            allocatorStrategyType = Q.New(DiskOfferingVO.class)
                    .eq(DiskOfferingVO_.uuid, msg.getDiskOfferingUuid())
                    .select(DiskOfferingVO_.allocatorStrategy)
                    .findValue();
        }

        if (allocatorStrategyType == null) {
            allocatorStrategyType = msg.getAllocationStrategy() == null ?
                    PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE
                    : msg.getAllocationStrategy();
        }

        if (msg.getExcludeAllocatorStrategies() != null && msg.getExcludeAllocatorStrategies().contains(allocatorStrategyType)) {
            throw new CloudRuntimeException(
                    String.format("%s is set as excluded, there is no available primary storage allocator strategy",
                            allocatorStrategyType));
        }
        return allocatorStrategyType;
    }

    private PrimaryStorageAllocationSpec buildAllocateSpecFromMsg(AllocatePrimaryStorageMsg msg) {
        PrimaryStorageAllocationSpec spec = new PrimaryStorageAllocationSpec();
        spec.setPossiblePrimaryStorageTypes(msg.getPossiblePrimaryStorageTypes());
        spec.setRequiredFeatures(msg.getRequiredFeatures());
        spec.setExcludePrimaryStorageTypes(msg.getExcludePrimaryStorageTypes());
        spec.setImageUuid(msg.getImageUuid());
        spec.setDiskOfferingUuid(msg.getDiskOfferingUuid());
        spec.setVmInstanceUuid(msg.getVmInstanceUuid());
        spec.setPurpose(msg.getPurpose());
        spec.setSize(msg.getSize());
        spec.setTotalSize(msg.getTotalSize());
        spec.setNoOverProvisioning(msg.isNoOverProvisioning());
        spec.setRequiredClusterUuids(msg.getRequiredClusterUuids());
        spec.setRequiredHostUuid(msg.getRequiredHostUuid());
        spec.setRequiredZoneUuid(msg.getRequiredZoneUuid());
        spec.setBackupStorageUuid(msg.getBackupStorageUuid());
        spec.setTags(msg.getTags());
        spec.setAllocationMessage(msg);
        spec.setAvoidPrimaryStorageUuids(msg.getExcludePrimaryStorageUuids());
        spec.setCandidatePrimaryStorageUuids(msg.getCandidatePrimaryStorageUuids());
        if (CollectionUtils.isEmpty(msg.getCandidatePrimaryStorageUuids()) && msg.getDiskOfferingUuid() != null
                && DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(msg.getDiskOfferingUuid())) {
            DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(msg.getDiskOfferingUuid(), DiskOfferingUserConfig.class);
            if (config.getAllocate() != null && config.getAllocate().getAllPrimaryStorages() != null) {
                List<String> requiredPrimaryStorageUuidsFromDiskOffering = config.getAllocate().getAllPrimaryStorages().stream()
                        .map(PrimaryStorageAllocateConfig::getUuid).collect(Collectors.toList());
                spec.setCandidatePrimaryStorageUuids(requiredPrimaryStorageUuidsFromDiskOffering);
            }
        }
        return spec;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PrimaryStorageConstant.SERVICE_ID);
    }


    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(PSCapacityExtensionPoint.class, new Function<Object, PSCapacityExtensionPoint>() {
            @Override
            public Object call(PSCapacityExtensionPoint arg) {
                return arg.getPrimaryStorageType().toString();
            }
        });
        populateExtensions();
        installResourceConfigValidator();
        installGlobalConfigValidator();
        installPrimaryStorageCidrValidator();
        return true;
    }

    private void installPrimaryStorageCidrValidator() {
        class PrimaryStorageCidrValidator implements SystemTagCreateMessageValidator, SystemTagValidator {

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.isMatch(systemTag)) {
                    String cidr = PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByTag(systemTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN);
                    if (!NetworkUtils.isCidr(cidr)) {
                        throw new ApiMessageInterceptionException(argerr(
                                "cidr[%s] Input Format Error", cidr));
                    }
                }
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
                    return;
                }

                int cidrCount = 0;
                for (String systemTag : msg.getSystemTags()) {
                    if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.isMatch(systemTag)) {
                        if (++cidrCount > 1) {
                            throw new ApiMessageInterceptionException(argerr("only one primaryStorage cidr system tag is allowed, but %d got", cidrCount));
                        }
                        String cidr = PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByTag(systemTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN);
                        if (!NetworkUtils.isCidr(cidr)) {
                            throw new ApiMessageInterceptionException(argerr(
                                    "cidr[%s] Input Format Error", cidr));
                        }
                    }
                }
            }
        }

        PrimaryStorageCidrValidator primaryStorageCidrValidator = new PrimaryStorageCidrValidator();
        tagMgr.installCreateMessageValidator(PrimaryStorageVO.class.getSimpleName(), primaryStorageCidrValidator);
        PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.installValidator(primaryStorageCidrValidator);
    }

    private void installResourceConfigValidator(){
        ResourceConfig resourceConfig = rcf.getResourceConfig(PrimaryStorageGlobalConfig.PRIMARY_STORAGE_AUTO_DELETE_TRASH.getIdentity());
        resourceConfig.installUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue){
                startPrimaryStorageAutoDeleteTrashTask(resourceUuid, newValue);
            }
        });
    }

    private void installGlobalConfigValidator() {
        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(newValue)) {
                    throw new GlobalConfigException(String.format("%s is not a size string;" +
                            " a size string consists of a number ending with suffix B/K/M/G/T or without suffix;" +
                            " for example, 512M, 1G", newValue));
                }
            }
        });

        PrimaryStorageGlobalConfig.PRIMARY_STORAGE_AUTO_DELETE_TRASH.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startPrimaryStorageAutoDeleteTrashTask(newConfig.value());
            }
        });

        PrimaryStorageGlobalConfig.TRASH_EXPIRATION_TIME.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                List<String> supportTypes = PrimaryStorageType.getSupportFeaturesTypes(PrimaryStorageType::isSupportStorageTrash);
                if (supportTypes.isEmpty()) {
                    return;
                }

                List<String> psUuids = Q.New(PrimaryStorageVO.class)
                        .select(PrimaryStorageVO_.uuid)
                        .in(PrimaryStorageVO_.type, supportTypes)
                        .listValues();

                if (psUuids.isEmpty()) {
                    return;
                }

                List<SetTrashExpirationTimeMsg> msgs = psUuids.stream().map(uuid -> {
                            SetTrashExpirationTimeMsg msg = new SetTrashExpirationTimeMsg();
                            msg.setUuid(uuid);
                            msg.setExpirationTime(newConfig.value(Integer.class));
                            bus.makeLocalServiceId(msg, PrimaryStorageConstant.SERVICE_ID);
                            return msg;
                        }).collect(Collectors.toList());

                bus.send(msgs);
            }
        });
    }

    private void initResourcePrimaryStorageAutoDeleteTrash(){
        List<Tuple> tuples = Q.New(ResourceConfigVO.class)
                .select(ResourceConfigVO_.resourceUuid, ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.name, PrimaryStorageGlobalConfig.PRIMARY_STORAGE_AUTO_DELETE_TRASH.getName())
                .listTuple();
        if( !tuples.isEmpty()){
            for(Tuple tuple : tuples){
                String resourceUuid = tuple.get(0, String.class);
                String value = tuple.get(1, String.class);
                startPrimaryStorageAutoDeleteTrashTask(resourceUuid, value);
            }
        }

        String globalValue = String.valueOf( PrimaryStorageGlobalConfig.PRIMARY_STORAGE_AUTO_DELETE_TRASH.value(Long.class));
        startPrimaryStorageAutoDeleteTrashTask(globalValue);
    }

    private void startPrimaryStorageAutoDeleteTrashTask(String newValue){
        startPrimaryStorageAutoDeleteTrashTask(null, newValue);
    }

    private void startPrimaryStorageAutoDeleteTrashTask(String resourceUuid, String newValue){
        primaryStorageAutoDeleteTrashTask(resourceUuid, newValue);
    }

    @Override
    public void sort(List<PrimaryStorageVO> primaryStorageVOS, VmInstanceSpec.ImageSpec imageSpec, String allocateStrategy) {
        if (primaryStorageVOS.size() < 2) {
            return;
        }

        if (allocateStrategy == null) {
            allocateStrategy = PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE;
        }
        PrimaryStorageAllocatorStrategyFactory factory = getPrimaryStorageAllocatorStrategyFactory(
                PrimaryStorageAllocatorStrategyType.valueOf(allocateStrategy));
        PrimaryStorageAllocatorStrategy strategy = factory.getPrimaryStorageAllocatorStrategy();
        PrimaryStorageAllocationSpec allocationSpec = new PrimaryStorageAllocationSpec();
        if (imageSpec != null && imageSpec.getInventory() != null) {
            allocationSpec.setImageUuid(imageSpec.getInventory().getUuid());
            Optional.ofNullable(imageSpec.getSelectedBackupStorage())
                    .ifPresent(it -> allocationSpec.setBackupStorageUuid(it.getBackupStorageUuid()));
        }

        strategy.sort(allocationSpec, primaryStorageVOS);
    }

    @Override
    public List<PrimaryStorageVO> allocatePrimaryStorage(Set<PrimaryStorageFeature> requiredFeatures, List<PrimaryStorageVO> candidates) {
        if (requiredFeatures.contains(PrimaryStorageFeature.SHARED_VOLUME)) {
            candidates = candidates.stream()
                    .filter(v -> PrimaryStorageType.getSupportFeaturesTypes(PrimaryStorageType::isSupportSharedVolume)
                            .contains(v.getType()))
                    .collect(Collectors.toList());
        }

        return candidates;
    }

    class AutoDeleteTrashTask {
        Future<Void> runnable;
        PeriodicTask task;

        public AutoDeleteTrashTask(PeriodicTask task) {
            this.runnable = thdf.submitPeriodicTask(task);
            this.task = task;
        }

        public long getPeriod() {
            return task.getInterval();
        }

        public void cancel() {
            runnable.cancel(true);
        }
    }

    private List<InstallPathRecycleVO> findRecycle(String psUuid) {
        if (psUuid != null) {
            return Q.New(InstallPathRecycleVO.class)
                    .eq(InstallPathRecycleVO_.storageUuid, psUuid).list();
        } else {
            List<String> resourceConfigPSUuids = Q.New(ResourceConfigVO.class)
                    .select(ResourceConfigVO_.resourceUuid)
                    .eq(ResourceConfigVO_.name, PrimaryStorageGlobalConfig.PRIMARY_STORAGE_AUTO_DELETE_TRASH.getName())
                    .listValues();
            return Q.New(InstallPathRecycleVO.class).notIn(InstallPathRecycleVO_.storageUuid, resourceConfigPSUuids).list();
        }
    }

    private PeriodicTask getTrashPeriodicTask(String psUuid, long period) {
        DebugUtils.Assert(period > 0, String.format("cannot submit a trash task which period[%d] <= 0", period));
        return new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return period;
            }

            @Override
            public String getName() {
                return "auto clean primary storage trash";
            }

            @Override
            public void run() {
                List<InstallPathRecycleVO> vos = findRecycle(psUuid);

                if(vos.isEmpty()){
                    return;
                }
                for (InstallPathRecycleVO vo: vos) {
                    CleanUpTrashOnPrimaryStroageMsg pmsg = new CleanUpTrashOnPrimaryStroageMsg();
                    pmsg.setPrimaryStorageUuid(vo.getStorageUuid());
                    pmsg.setTrashId(vo.getTrashId());
                    bus.makeTargetServiceIdByResourceUuid(pmsg, PrimaryStorageConstant.SERVICE_ID, vo.getStorageUuid());
                    bus.send(pmsg, new CloudBusCallBack(pmsg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("Delete trash [%s] on primary storage [%s] successfully in periodic task",
                                        vo.getTrashId(), vo.getStorageUuid()));
                            }else{
                                logger.warn(String.format("Delete trash [%s] on primary storage [%s] failed in periodic task, because: %s",
                                        vo.getTrashId(), vo.getStorageUuid(), reply.getError().getDetails()));
                            }
                        }
                    });
                }
            }
        };
    }

    private void primaryStorageAutoDeleteTrashTask(String primaryStorageUuid, String newValue){
        logger.debug(String.format("start submit auto delete trash task for primary storage[%s]", primaryStorageUuid != null ? primaryStorageUuid : "globalTrashTask"));

        long period;
        try {
            period = Long.parseLong(newValue);
        } catch (NumberFormatException e) {
            logger.error(e.getLocalizedMessage());
            return;
        }

        if (period <= 0){
            return;
        }

        if (primaryStorageUuid == null) {
            if (globalTrashTask != null) {
                globalTrashTask.cancel();
            }
            globalTrashTask = new AutoDeleteTrashTask(getTrashPeriodicTask(null, period));
            logger.debug(String.format("submit new globalTrashTask, period: %d", period));
        } else {
            synchronized (autoDeleteTrashTask) {
                if(autoDeleteTrashTask.containsKey(primaryStorageUuid)) {
                    if (autoDeleteTrashTask.get(primaryStorageUuid).getPeriod() != period) {
                        autoDeleteTrashTask.get(primaryStorageUuid).cancel();
                        logger.debug(String.format("cancel trash task for %s, period: %d", primaryStorageUuid, autoDeleteTrashTask.get(primaryStorageUuid).getPeriod()));
                    } else {
                        return;
                    }
                }
                autoDeleteTrashTask.put(primaryStorageUuid, new AutoDeleteTrashTask(getTrashPeriodicTask(primaryStorageUuid, period)));
                logger.debug(String.format("submit clean trash task for %s, period: %d", primaryStorageUuid, period));
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void startPeriodTasks() {
        PrimaryStorageGlobalConfig.PRIMARY_STORAGE_HOST_STATUS_REFRESH_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startRefreshPrimaryStorageHostStatusTask());
        startRefreshPrimaryStorageHostStatusTask();
    }

    private synchronized void startRefreshPrimaryStorageHostStatusTask() {
        if (refreshPrimaryStorageHostStatusTask != null) {
            refreshPrimaryStorageHostStatusTask.cancel(true);
        }

        refreshPrimaryStorageHostStatusTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return PrimaryStorageGlobalConfig.PRIMARY_STORAGE_HOST_STATUS_REFRESH_INTERVAL.value(Integer.class).longValue();
            }

            @Override
            public String getName() {
                return "update-host-storage-connection-status";
            }

            @Override
            public void run() {
                Map<String, List<String>> disconnectedHostsByPsUuid = new HashMap<>();

                List<PrimaryStorageHostRefVO> refs = Q.New(PrimaryStorageHostRefVO.class)
                        .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                        .list();

                refs.forEach(ref -> {
                    disconnectedHostsByPsUuid.computeIfAbsent(ref.getPrimaryStorageUuid(), key -> new ArrayList<>()).add(ref.getHostUuid());
                });

                List<CheckHostStorageConnectionMsg> msgs = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : disconnectedHostsByPsUuid.entrySet()) {
                    PrimaryStorageVO storageVO = dbf.findByUuid(entry.getKey(), PrimaryStorageVO.class);
                    if (!PrimaryStorageType.valueOf(storageVO.getType()).isSupportCheckHostStatus()){
                        continue;
                    }
                    CheckHostStorageConnectionMsg msg = new CheckHostStorageConnectionMsg();
                    msg.setPrimaryStorageUuid(entry.getKey());
                    msg.setHostUuids(entry.getValue());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, entry.getKey());
                    msgs.add(msg);
                }

                new While<>(msgs).step((msg, comp) -> {
                    bus.send(msg, new CloudBusCallBack(comp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.error(String.format("Failed to check host storage connection for primary storage %s and host %s due to: %s",
                                        msg.getPrimaryStorageUuid(), msg.getHostUuids().get(0), reply.getError()));
                            }
                            comp.done();
                        }
                    });
                }, 10).run(new NopeWhileDoneCompletion());
            }
        });
    }

    private void populateExtensions() {
        for (PrimaryStorageAllocatorStrategyFactory f : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyFactory.class)) {
            PrimaryStorageAllocatorStrategyFactory old = allocatorFactories.get(f.getPrimaryStorageAllocatorStrategyType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PrimaryStorageAllocatorStrategyFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getPrimaryStorageAllocatorStrategyType()));
            }
            allocatorFactories.put(f.getPrimaryStorageAllocatorStrategyType().toString(), f);
        }

        for (PrimaryStorageFactory f : pluginRgty.getExtensionList(PrimaryStorageFactory.class)) {
            PrimaryStorageFactory old = primaryStorageFactories.get(f.getPrimaryStorageType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PrimaryStorageFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getPrimaryStorageType()));
            }
            primaryStorageFactories.put(f.getPrimaryStorageType().toString(), f);
        }

        for (PrimaryStorageExtensionFactory f : pluginRgty.getExtensionList(PrimaryStorageExtensionFactory.class)) {
            List<PrimaryStorageExtensionFactory> factories = extensionFactories.computeIfAbsent(f.getPrimaryStorageType(), k->new ArrayList<>());
            factories.add(f);
        }
    }


    private PrimaryStorageAllocatorStrategyFactory getPrimaryStorageAllocatorStrategyFactory(PrimaryStorageAllocatorStrategyType type) {
        PrimaryStorageAllocatorStrategyFactory factory = allocatorFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No PrimaryStorageAllocatorStrategyFactory for type: %s found", type));
        }
        return factory;
    }

    @Override
    public PrimaryStorageFactory getPrimaryStorageFactory(PrimaryStorageType type) {
        PrimaryStorageFactory factory = primaryStorageFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No PrimaryStorageFactory for type: %s found", type));
        }
        return factory;
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        logger.debug(String.format("management node[uuid:%s] left, node[uuid:%s] starts taking over primary storage...",
                inv.getUuid(), Platform.getManagementServerId()));
        loadPrimaryStorage(true);
    }

    private List<String> getPrimaryStorageManagedByUs(boolean skipConnected) {
        List<String> ret = new ArrayList<>();
        final Q q = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid);

        if (skipConnected) {
            // treat connecting as disconnected
            q.notEq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected);
        }

        List<String> uuids = q.listValues();
        for (String uuid : uuids) {
            if (destMaker.isManagedByUs(uuid)) {
                ret.add(uuid);
            }
        }

        return ret;
    }

    private void loadPrimaryStorage(boolean skipConnected) {
        List<String> uuids = getPrimaryStorageManagedByUs(skipConnected);
        if (uuids.isEmpty()) {
            return;
        }

        List<ConnectPrimaryStorageMsg> msgs = transformAndRemoveNull(uuids, arg -> {
            ConnectPrimaryStorageMsg msg = new ConnectPrimaryStorageMsg();
            msg.setPrimaryStorageUuid(arg);
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg);
            return msg;
        });

        bus.send(msgs);
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("management node[uuid:%s] joins, starts load primary storage ...",
                Platform.getManagementServerId()));
        loadPrimaryStorage(false);
        initResourcePrimaryStorageAutoDeleteTrash();
        startPeriodTasks();
    }

    private ErrorCode checkVmAllVolumePrimaryStorageState(String vmUuid) {
        String sql = "select uuid from PrimaryStorageVO where uuid in (" +
                " select distinct(primaryStorageUuid) from VolumeVO" +
                " where vmInstanceUuid = :vmUuid and primaryStorageUuid is not null)" +
                " and state = :psState";
        List<String> result = SQL.New(sql, String.class)
                .param("vmUuid", vmUuid)
                .param("psState", PrimaryStorageState.Maintenance)
                .list();
        if (result != null && !result.isEmpty()) {
            return operr("the VM[uuid:%s] volume stored location primary storage is in a state of maintenance", vmUuid);
        }
        return null;
    }

    @Override
    public ErrorCode preStartVm(VmInstanceInventory inv) {
        return checkVmAllVolumePrimaryStorageState(inv.getUuid());
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {
        // do nothing
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        // do nothing
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {
        // do nothing
    }

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {
        settingRootVolume(msg);
        settingDataVolume(msg);

        String instanceOffering = msg.getInstanceOfferingUuid();
        if (InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.hasTag(instanceOffering)) {
            InstanceOfferingUserConfig config = OfferingUserConfigUtils.getInstanceOfferingConfig(instanceOffering, InstanceOfferingUserConfig.class);
            if (config.getAllocate() == null || config.getAllocate().getClusterUuid() == null) {
                return;
            }

            String clusterUuid = config.getAllocate().getClusterUuid();
            if (clusterUuid != null) {
                if (!dbf.isExist(clusterUuid, ClusterVO.class)) {
                    throw new IllegalArgumentException(String.format("the cluster[uuid=%s] does not exist", clusterUuid));
                }
            }

            if (msg.getClusterUuid() != null && !msg.getClusterUuid().equals(clusterUuid)) {
                throw new OperationFailureException(operr("clusterUuid conflict, the cluster specified by the instance offering is %s, and the cluster specified in the creation parameter is %s"
                        , clusterUuid, msg.getClusterUuid()));
            }

            msg.setClusterUuid(clusterUuid);
        }
    }
    private void settingRootVolume(CreateVmInstanceMsg msg) {
        String instanceOffering = msg.getInstanceOfferingUuid();

        if (InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.hasTag(instanceOffering)) {
            InstanceOfferingUserConfig config = OfferingUserConfigUtils.getInstanceOfferingConfig(instanceOffering, InstanceOfferingUserConfig.class);
            if (config.getAllocate() != null && config.getAllocate().getPrimaryStorage() != null) {
                String psUuid = config.getAllocate().getPrimaryStorage().getUuid();
                if (!msg.getCandidatePrimaryStorageUuidsForRootVolume().isEmpty() && !msg.getCandidatePrimaryStorageUuidsForRootVolume().contains(psUuid)) {
                    throw new OperationFailureException(operr("primaryStorageUuid conflict, the primary storage specified by the instance offering is %s, and the primary storage specified in the creation parameter is %s"
                            , psUuid, msg.getCandidatePrimaryStorageUuidsForRootVolume()));
                }
                msg.setPrimaryStorageUuidForRootVolume(psUuid);
            }
        }

        String rootDiskOffering = msg.getRootDiskOfferingUuid();
        if (rootDiskOffering == null || !DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(rootDiskOffering)) {
            return;
        }

        DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(rootDiskOffering, DiskOfferingUserConfig.class);
        if (config.getAllocate() == null) {
            return;
        }

        if (!config.getAllocate().getAllPrimaryStorages().isEmpty()) {
            List<String> requiredPrimaryStorageUuids = config.getAllocate().getAllPrimaryStorages().stream()
                    .map(PrimaryStorageAllocateConfig::getUuid).collect(Collectors.toList());
            if (!msg.getCandidatePrimaryStorageUuidsForRootVolume().isEmpty() && Collections.disjoint(requiredPrimaryStorageUuids, msg.getCandidatePrimaryStorageUuidsForRootVolume())) {
                throw new OperationFailureException(operr("primaryStorageUuid conflict, the primary storage specified by the disk offering are %s, and the primary storage specified in the creation parameter is %s",
                        requiredPrimaryStorageUuids, msg.getCandidatePrimaryStorageUuidsForRootVolume()));
            } else if (msg.getCandidatePrimaryStorageUuidsForRootVolume().isEmpty()) {
                msg.setCandidatePrimaryStorageUuidsForRootVolume(requiredPrimaryStorageUuids);
            }
        }
    }

    private void settingDataVolume(CreateVmInstanceMsg msg) {
        if (isEmpty(msg.getDeprecatedDataVolumeSpecs())) {
            return;
        }

        String diskOffering = msg.getDeprecatedDataVolumeSpecs().get(0).getDiskOfferingUuid();
        if (diskOffering == null || !DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            return;
        }

        DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);
        if (config.getAllocate() == null) {
            return;
        }

        if (!config.getAllocate().getAllPrimaryStorages().isEmpty()) {
            List<String> requiredPrimaryStorageUuids = config.getAllocate().getAllPrimaryStorages().stream()
                    .map(PrimaryStorageAllocateConfig::getUuid).collect(Collectors.toList());
            if (!msg.getCandidatePrimaryStorageUuidsForDataVolume().isEmpty() && Collections.disjoint(requiredPrimaryStorageUuids, msg.getCandidatePrimaryStorageUuidsForDataVolume())) {
                throw new OperationFailureException(operr("primaryStorageUuid conflict, the primary storage specified by the disk offering are %s, and the primary storage specified in the creation parameter is %s",
                        requiredPrimaryStorageUuids, msg.getCandidatePrimaryStorageUuidsForDataVolume()));
             } else if (msg.getCandidatePrimaryStorageUuidsForDataVolume().isEmpty()) {
                msg.setCandidatePrimaryStorageUuidsForDataVolume(requiredPrimaryStorageUuids);
            }
        }

    }

    @Override
    public void validateInstanceOfferingUserConfig(String userConfig, String instanceOfferingUuid) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }

        InstanceOfferingUserConfig config;

        try {
            config = OfferingUserConfigUtils.toObject(userConfig, InstanceOfferingUserConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Syntax error(s) in instance offering user configuration, user configuration should write in json format.", e);
        }

        if (config.getAllocate() == null) {
            return;
        }

        String clusterUuid = config.getAllocate().getClusterUuid();
        if (clusterUuid != null) {
            if (!dbf.isExist(clusterUuid, ClusterVO.class)) {
                throw new IllegalArgumentException(String.format("the cluster[uuid=%s] does not exist", clusterUuid));
            }
        }

        PrimaryStorageAllocateConfig primaryStorageAllocateConfig = config.getAllocate().getPrimaryStorage();
        if (primaryStorageAllocateConfig == null) {
            return;
        }

        if (primaryStorageAllocateConfig.getType() == null) {
            throw new IllegalArgumentException("primaryStorage type cannot be empty");
        }

        if (primaryStorageAllocateConfig.getUuid() == null) {
            throw new IllegalArgumentException("primaryStorage uuid cannot be empty");
        }

        String psUuid = primaryStorageAllocateConfig.getUuid();
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        if (primaryStorageVO == null) {
            throw new IllegalArgumentException(String.format("primaryStorage[uuid=%s] does not exist", psUuid));
        }

        if (!primaryStorageVO.getType().equalsIgnoreCase(primaryStorageAllocateConfig.getType())) {
            throw new IllegalArgumentException(String.format("primaryStorage[uuid=%s] type is %s", psUuid, primaryStorageVO.getType()));
        }
    }

    @Override
    public void validateDiskOfferingUserConfig(String userConfig, String diskOfferingUuid) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }

        DiskOfferingUserConfig config;

        try {
            config = OfferingUserConfigUtils.toObject(userConfig, DiskOfferingUserConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Syntax error(s) in disk offering user configuration, user configuration should write in json format.", e);
        }

        if (config.getAllocate() == null) {
            return;
        }

        PrimaryStorageAllocateConfig primaryStorageAllocateConfig = config.getAllocate().getPrimaryStorage();
        if (primaryStorageAllocateConfig == null) {
            return;
        }

        if (primaryStorageAllocateConfig.getType() == null) {
            throw new IllegalArgumentException("primaryStorage type cannot be empty");
        }

        if (primaryStorageAllocateConfig.getUuid() == null) {
            throw new IllegalArgumentException("primaryStorage uuid cannot be empty");
        }

        String psUuid = primaryStorageAllocateConfig.getUuid();
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        if (primaryStorageVO == null) {
            throw new IllegalArgumentException(String.format("primaryStorage[uuid=%s] does not exist", psUuid));
        }

        if (!primaryStorageVO.getType().equalsIgnoreCase(primaryStorageAllocateConfig.getType())) {
            throw new IllegalArgumentException(String.format("primaryStorage[uuid=%s] type is %s", psUuid, primaryStorageVO.getType()));
        }
    }
}
