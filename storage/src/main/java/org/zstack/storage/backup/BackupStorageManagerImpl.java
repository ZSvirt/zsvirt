package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

public class BackupStorageManagerImpl extends AbstractService implements BackupStorageManager,
        ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BackupStorageManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private TagManager tagMgr;

    private Map<String, BackupStorageFactory> backupStorageFactories = Collections.synchronizedMap(new HashMap<String, BackupStorageFactory>());
    private static final Set<Class> allowedMessageAfterDeletion = new HashSet<Class>();
    private Map<String, BackupStorageAllocatorStrategyFactory> allocatorStrategyFactories = new HashMap<String, BackupStorageAllocatorStrategyFactory>();

    static {
        allowedMessageAfterDeletion.add(BackupStorageDeletionMsg.class);
    }

    private void handleApiMessage(APIMessage msg) {
        try {
            if (msg instanceof APIAddBackupStorageMsg) {
                handle((APIAddBackupStorageMsg) msg);
            } else if (msg instanceof APIGetBackupStorageCapacityMsg) {
                handle((APIGetBackupStorageCapacityMsg) msg);
            } else if (msg instanceof APIGetBackupStorageTypesMsg) {
                handle((APIGetBackupStorageTypesMsg) msg);
            } else if (msg instanceof BackupStorageMessage) {
                passThrough((BackupStorageMessage) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handle(final APIGetBackupStorageCapacityMsg msg) {
        APIGetBackupStorageCapacityReply reply = new APIGetBackupStorageCapacityReply();

        Tuple ret = new Callable<Tuple>() {
            @Override
            @Transactional(readOnly = true)
            public Tuple call() {
                if (msg.getBackupStorageUuids() != null && !msg.getBackupStorageUuids().isEmpty()) {
                    String sql = "select sum(bs.totalCapacity), sum(bs.availableCapacity) from BackupStorageVO bs where bs.uuid in (:bsUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("bsUuids", msg.getBackupStorageUuids());
                    return q.getSingleResult();
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select sum(bs.totalCapacity), sum(bs.availableCapacity) from BackupStorageVO bs, BackupStorageZoneRefVO ref where ref.backupStorageUuid = bs.uuid and ref.zoneUuid in (:zoneUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    return q.getSingleResult();
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        Long total = ret.get(0, Long.class);
        Long avail = ret.get(1, Long.class);
        reply.setTotalCapacity(total == null ? 0 : total);
        reply.setAvailableCapacity(avail == null ? 0 : avail);
        bus.reply(msg, reply);
    }

    private void handle(APIGetBackupStorageTypesMsg msg) {
        List<String> types = new ArrayList<String>();
        APIGetBackupStorageTypesReply reply = new APIGetBackupStorageTypesReply();
        types.addAll(BackupStorageType.getAllTypeNames());
        reply.setBackupStorageTypes(types);
        bus.reply(msg, reply);
    }

    private void passThrough(BackupStorageMessage pmsg) {
        BackupStorageVO vo = dbf.findByUuid(pmsg.getBackupStorageUuid(), BackupStorageVO.class);
        if (vo == null && allowedMessageAfterDeletion.contains(pmsg.getClass())) {
            BackupStorageEO eo = dbf.findByUuid(pmsg.getBackupStorageUuid(), BackupStorageEO.class);
            vo = ObjectUtils.newAndCopy(eo, BackupStorageVO.class);
        }

        Message msg = (Message) pmsg;
        if (vo == null) {
            String err = String.format("Cannot find backup storage[uuid:%s], it may have been deleted", pmsg.getBackupStorageUuid());
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        BackupStorageFactory factory = getBackupStorageFactory(BackupStorageType.valueOf(vo.getType()));
        BackupStorageVO finalVo = vo;
        BackupStorage ss = New(()-> factory.getBackupStorage(finalVo));
        ss.handleMessage(msg);
    }

    private void handle(final APIAddBackupStorageMsg msg) {
        BackupStorageType type = BackupStorageType.valueOf(msg.getType());
        final BackupStorageFactory factory = getBackupStorageFactory(type);

        BackupStorageVO vo = new BackupStorageVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }

        vo.setUrl(msg.getUrl());
        vo.setType(type.toString());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(BackupStorageState.Enabled);
        vo.setStatus(BackupStorageStatus.Connecting);

        final BackupStorageInventory inv = factory.createBackupStorage(vo, msg);
        AddBackupStorageStruct addBackupStoragestruct = new AddBackupStorageStruct();
        if(msg.isImportImages()) {
            addBackupStoragestruct.setImportImages(true);
        }
        addBackupStoragestruct.setBackupStorageInventory(inv);
        addBackupStoragestruct.setType(vo.getType());
        addBackupStoragestruct.setTag( msg.getSystemTags());

        final APIAddBackupStorageEvent evt = new APIAddBackupStorageEvent(msg.getId());
        ConnectBackupStorageMsg cmsg = new ConnectBackupStorageMsg();
        cmsg.setNewAdd(true);
        cmsg.setBackupStorageUuid(inv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, vo.getUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(factory.reload(inv.getUuid()));
                    bus.publish(evt);
                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddBackupStorageExtensionPoint.class), new ForEachFunction<AddBackupStorageExtensionPoint>() {
                        @Override
                        public void run(AddBackupStorageExtensionPoint ext) {
                            ext.afterAddBackupStorage(addBackupStoragestruct);
                        }
                    });
                    tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), BackupStorageVO.class.getSimpleName());

                } else {
                    dbf.removeByPrimaryKey(inv.getUuid(), BackupStorageVO.class);
                    evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, reply.getError().getDetails()).withCause(reply.getError()));
                    bus.publish(evt);
                }
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
        if (msg instanceof BackupStorageMessage) {
            passThrough((BackupStorageMessage) msg);
        } else if (msg instanceof AllocateBackupStorageMsg) {
            handle((AllocateBackupStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional
    private boolean reserve(String bsUuid, long size) {
        BackupStorageVO vo = dbf.getEntityManager().find(BackupStorageVO.class, bsUuid, LockModeType.PESSIMISTIC_WRITE);
        if (vo == null) {
            logger.warn(String.format("reservation failure, cannot find backup storage[uuid:%s]", bsUuid));
            return false;
        }

        if (vo.getAvailableCapacity() < size) {
            logger.warn(String.format("reservation failure, cannot reserve capacity[%s bytes] on backup storage[uuid:%s]", size, bsUuid));
            return false;
        }

        long avail = vo.getAvailableCapacity() - size;
        vo.setAvailableCapacity(avail);
        dbf.getEntityManager().merge(vo);
        logger.debug(String.format("reserve %s bytes on backup storage[uuid:%s, total capacity:%s, available capacity:%s]",
                size, bsUuid, vo.getTotalCapacity(), avail));
        return true;
    }

    private void handle(AllocateBackupStorageMsg msg) {
        String allocatorStrategy = msg.getAllocatorStrategy() == null ? BackupStorageConstant.DEFAULT_ALLOCATOR_STRATEGY : msg.getAllocatorStrategy();
        BackupStorageAllocatorStrategyFactory factory = getAllocatorFactory(allocatorStrategy);
        BackupStorageAllocatorStrategy strategy = factory.getAllocatorStrategy();

        BackupStorageAllocationSpec spec = new BackupStorageAllocationSpec();
        spec.setAllocationMessage(msg);
        spec.setRequiredBackupStorageUuid(msg.getBackupStorageUuid());
        spec.setSize(msg.getSize());
        spec.setRequiredZoneUuid(msg.getRequiredZoneUuid());
        spec.setRequiredPrimaryStorageUuid(msg.getRequiredPrimaryStorageUuid());
        AllocateBackupStorageReply reply = new AllocateBackupStorageReply();
        try {
            List<BackupStorageInventory> invs = strategy.allocateAllCandidates(spec);
            Iterator<BackupStorageInventory> it = invs.iterator();
            BackupStorageInventory target = null;
            while (it.hasNext()) {
                BackupStorageInventory inv = it.next();
                if (reserve(inv.getUuid(), msg.getSize())) {
                    target = inv;
                    break;
                }

                logger.debug(String.format("concurrent reservation on backup storage[uuid:%s], try next", inv.getUuid()));
            }

            if (target == null) {
                reply.setError(operr("capacity reservation on all backup storage failed"));
            } else {
                reply.setInventory(target);
            }
        } catch (BackupStorageException e) {
            reply.setError(e.getError());
        }

        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BackupStorageConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateBackupStorageFactory();
        installValidatorToGlobalConfig();
        installValidatorToSystemTag();
        return true;
    }

    private void installValidatorToSystemTag() {
        class BackupStorageDataNetworkValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg instanceof APIAddBackupStorageMsg) {
                    check((APIAddBackupStorageMsg) msg);
                }
            }

            private void check(APIAddBackupStorageMsg msg) {
                List<String> backupStorageDataNetworkTags = msg.getSystemTags()
                        .stream()
                        .filter(sysTag -> BackupStorageSystemTags.BACKUP_STORAGE_DATA_NETWORK.isMatch(sysTag))
                        .collect(Collectors.toList());

                if (backupStorageDataNetworkTags.isEmpty()) {
                    return;
                }

                if (backupStorageDataNetworkTags.size() > 1) {
                    throw new ApiMessageInterceptionException(argerr("only one backup storage data network system tag is allowed, but %s got", backupStorageDataNetworkTags.size()));
                }

                validateDataNetworkSystemTag(backupStorageDataNetworkTags.get(0));
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                validateDataNetworkSystemTag(systemTag);
            }

            private void validateDataNetworkSystemTag(String systemTag) {
                String cidr = BackupStorageSystemTags.BACKUP_STORAGE_DATA_NETWORK.getTokenByTag(systemTag,
                        BackupStorageSystemTags.BACKUP_STORAGE_DATA_NETWORK_TOKEN);
                String fmtCidr = NetworkUtils.fmtCidr(cidr);
                if (!fmtCidr.equals(cidr)) {
                    throw new ApiMessageInterceptionException(argerr("[%s] is not a standard cidr, do you mean [%s]?", cidr, fmtCidr));
                }
            }
        }

        BackupStorageDataNetworkValidator backupStorageDataNetworkValidator = new BackupStorageDataNetworkValidator();
        tagMgr.installCreateMessageValidator(BackupStorageVO.class.getSimpleName(), backupStorageDataNetworkValidator);
        BackupStorageSystemTags.BACKUP_STORAGE_DATA_NETWORK.installValidator(backupStorageDataNetworkValidator);
    }

    private void installValidatorToGlobalConfig() {
        BackupStorageGlobalConfig.RESERVED_CAPACITY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(newValue)) {
                    throw new GlobalConfigException(String.format("%s is not a size string; a size string consists of a number ending with suffix B/K/M/G/T or without suffix; for example, 512M, 1G", newValue));
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateBackupStorageFactory() {
        for (BackupStorageFactory factory : pluginRgty.getExtensionList(BackupStorageFactory.class)) {
            BackupStorageFactory old = backupStorageFactories.get(factory.getBackupStorageType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BackupStorageFactory[%s, %s] for type[%s]",
                        factory.getClass().getName(), old.getClass().getName(), old.getBackupStorageType()));
            }
            backupStorageFactories.put(factory.getBackupStorageType().toString(), factory);
        }

        for (BackupStorageAllocatorStrategyFactory factory : pluginRgty.getExtensionList(BackupStorageAllocatorStrategyFactory.class)) {
            BackupStorageAllocatorStrategyFactory old = allocatorStrategyFactories.get(factory.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BackupStorageAllocatorStrategyFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), factory.getClass().getName(), factory.getType()));
            }
            allocatorStrategyFactories.put(factory.getType().toString(), factory);
        }
    }

    @Override
    public BackupStorageFactory getBackupStorageFactory(BackupStorageType type) {
        BackupStorageFactory factory = backupStorageFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No BackupStorageFactory for type: %s found", type));
        }
        return factory;
    }

    @Override
    public BackupStorageAllocatorStrategyFactory getAllocatorFactory(String type) {
        BackupStorageAllocatorStrategyFactory factory = allocatorStrategyFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("no BackupStorageAllocatorStrategyFactory[%s] found", type));
        }

        return factory;
    }

    private List<String> getBackupStorageManagedByUs(boolean skipConnected) {
        List<String> ret = new ArrayList<>();

        final Q q = Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.uuid);

        if (skipConnected) {
            // treat connecting as disconnected
            q.notEq(BackupStorageVO_.status, BackupStorageStatus.Connected);
        }

        List<String> uuids = q.listValues();
        for (String uuid : uuids) {
            if (destMaker.isManagedByUs(uuid)) {
                ret.add(uuid);
            }
        }

        return ret;
    }

    private void loadBackupStorage(boolean skipConnected) {
        List<String> uuids = getBackupStorageManagedByUs(skipConnected);
        if (uuids.isEmpty()) {
            return;
        }

        List<ConnectBackupStorageMsg> msgs = new ArrayList<ConnectBackupStorageMsg>();
        for (String uuid : uuids) {
            ConnectBackupStorageMsg msg = new ConnectBackupStorageMsg();
            msg.setBackupStorageUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, uuid);
            msgs.add(msg);
        }

        bus.send(msgs);
    }


    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        logger.debug(String.format("management node[uuid:%s] left, node[uuid:%s] starts taking over backup storage...", inv.getUuid(), Platform.getManagementServerId()));
        loadBackupStorage(true);
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        logger.debug(String.format("management node[uuid:%s] joins, starts load backup storage...", Platform.getManagementServerId()));
        loadBackupStorage(false);
    }
}
