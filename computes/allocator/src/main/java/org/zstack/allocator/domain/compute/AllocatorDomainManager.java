package org.zstack.allocator.domain.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.api.APICreateAllocatorDomainEvent;
import org.zstack.allocator.domain.api.APICreateAllocatorDomainMsg;
import org.zstack.allocator.domain.api.APIDeleteAllocatorDomainEvent;
import org.zstack.allocator.domain.api.APIDeleteAllocatorDomainMsg;
import org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesMsg;
import org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply;
import org.zstack.allocator.domain.api.APIGetAllocatorDomainVmsMsg;
import org.zstack.allocator.domain.api.APIGetAllocatorDomainVmsReply;
import org.zstack.allocator.domain.api.APIUpdateAllocatorDomainEvent;
import org.zstack.allocator.domain.api.APIUpdateAllocatorDomainMsg;
import org.zstack.allocator.domain.entity.AllocatorDomainInventory;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleParam;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO_;
import org.zstack.allocator.domain.entity.AllocatorDomainSceneVO;
import org.zstack.allocator.domain.entity.AllocatorDomainSceneVO_;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorDomainVO_;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig;
import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags;
import org.zstack.allocator.domain.message.AllocatorDomainDeletionMsg;
import org.zstack.allocator.domain.message.AllocatorDomainDeletionReply;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.SkipResetGlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.inerr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class AllocatorDomainManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(AllocatorDomainManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigs;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            if (msg instanceof APICreateAllocatorDomainMsg) {
                handle((APICreateAllocatorDomainMsg) msg);
            } else if (msg instanceof APIUpdateAllocatorDomainMsg) {
                handle((APIUpdateAllocatorDomainMsg) msg);
            } else if (msg instanceof APIDeleteAllocatorDomainMsg) {
                handle((APIDeleteAllocatorDomainMsg) msg);
            } else if (msg instanceof APIGetAllocatorDomainVmsMsg) {
                handle((APIGetAllocatorDomainVmsMsg) msg);
            } else if (msg instanceof APIGetAllocatorDomainCandidatesMsg) {
                handle((APIGetAllocatorDomainCandidatesMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } else if (msg instanceof AllocatorDomainDeletionMsg) {
            handle((AllocatorDomainDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AllocatorDomainConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        AllocatorDomainGlobalConfig.SCHEDULING_DOMAIN_UUID.installLocalBeforeUpdateExtension((oldConfig, newValue) -> {
            throw err(AllocatorDomainErrors.GLOBAL_CONFIG_UPDATE_NOT_ALLOWED,
                    "global config[%s] can only be changed as a resource config",
                    AllocatorDomainGlobalConfig.DOMAIN_UUID_IDENTITY)
                    .toException();
        });
        AllocatorDomainGlobalConfig.SCHEDULING_DOMAIN_UUID.installBeforeResetExtension(session -> {
            throw new SkipResetGlobalConfigException();
        });

        ResourceConfig rc = resourceConfigs.getResourceConfig(AllocatorDomainGlobalConfig.DOMAIN_UUID_IDENTITY);
        rc.installValidatorExtension((resourceUuid, oldValue, newValue) -> {
            if (StringUtils.isEmpty(newValue) || AllocatorDomainConstant.UNBOUND_DOMAIN_UUID.equals(newValue)) {
                return;
            }
            VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, resourceUuid).find();
            if (vm != null && !VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
                throw err(AllocatorDomainErrors.DOMAIN_RELATED_ERROR,
                        "allocator domain can only be bound to UserVm, resource[uuid:%s, type:%s]",
                        resourceUuid, vm.getType())
                        .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_UUID, resourceUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_TYPE, vm.getType())
                        .toException();
            }
            boolean exists = Q.New(AllocatorDomainVO.class)
                    .eq(AllocatorDomainVO_.uuid, newValue)
                    .isExists();
            if (!exists) {
                throw err(AllocatorDomainErrors.DOMAIN_NOT_FOUND,
                        "allocator domain[uuid:%s] not found", newValue)
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, newValue)
                        .toException();
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    // ------------------------------------- Message Handers ------------------------------------

    private String syncSignature(String domainUuid) {
        return "allocator-domain-" + domainUuid;
    }

    private void handle(APICreateAllocatorDomainMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature(msg.getResourceUuid());
            }

            @Override
            public String getName() {
                return "create-allocator-domain-" + msg.getResourceUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                createAllocatorDomain(msg, chain);
            }
        });
    }

    private void createAllocatorDomain(APICreateAllocatorDomainMsg msg, SyncTaskChain chain) {
        APICreateAllocatorDomainEvent event = new APICreateAllocatorDomainEvent(msg.getId());
        String uuid = msg.getResourceUuid();
        Timestamp now = timeHelper.getCurrentTimestamp();

        SimpleFlowChain.of("create-allocator-domain-" + uuid)
        .then(Flow.of("persist-allocator-domain")
            .handle(trigger -> {
                AllocatorDomainVO vo = new AllocatorDomainVO();
                vo.setUuid(uuid);
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setZoneUuid(msg.getZoneUuid());
                vo.setState(AllocatorDomainState.Enabled);
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                vo.setCreateDate(now);
                vo.setLastOpDate(now);
                dbf.persistAndRefresh(vo);
                persistRules(uuid, msg.getRules(), now);
                persistScenes(uuid, msg.getScenes(), now);
                trigger.next();
            })
            .rollback(trigger -> {
                AllocatorDomainVO vo = dbf.findByUuid(uuid, AllocatorDomainVO.class);
                if (vo != null) {
                    dbf.remove(vo);
                }
                trigger.rollback();
            })
            .build())
        .then(Flow.of("create-allocator-domain-tags")
            .handle(trigger -> {
                try {
                    tagMgr.createTagsFromAPICreateMessage(msg, uuid, AllocatorDomainVO.class.getSimpleName());
                    trigger.next();
                } catch (ApiMessageInterceptionException e) {
                    trigger.fail(e.getError());
                } catch (OperationFailureException e) {
                    trigger.fail(e.getErrorCode());
                }
            })
            .build())
        .propagateExceptionTo(msg)
        .done(() -> {
            chain.next();
            event.setInventory(toInventory(uuid));
            bus.publish(event);
        })
        .error(errorCode -> {
            chain.next();
            event.setError(errorCode);
            bus.publish(event);
        })
        .start();
    }

    private void handle(APIUpdateAllocatorDomainMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature(msg.getUuid());
            }

            @Override
            public String getName() {
                return "update-allocator-domain-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateAllocatorDomainEvent event = new APIUpdateAllocatorDomainEvent(msg.getId());
                try {
                    updateAllocatorDomain(msg);
                    event.setInventory(toInventory(msg.getUuid()));
                    bus.publish(event);
                } catch (OperationFailureException e) {
                    event.setError(e.getErrorCode());
                    bus.publish(event);
                } catch (Throwable t) {
                    logger.warn(String.format("failed to update allocator domain[uuid:%s]", msg.getUuid()), t);
                    event.setError(inerr(t.getMessage()));
                    bus.publish(event);
                } finally {
                    chain.next();
                }
            }
        });
    }

    private void updateAllocatorDomain(APIUpdateAllocatorDomainMsg msg) {
        boolean rewriteRules = msg.getRules() != null;
        boolean rewriteScenes = msg.getScenes() != null;
        boolean changed = rewriteRules || rewriteScenes
                || msg.getName() != null || msg.getDescription() != null || msg.getState() != null;

        if (!changed) {
            return;
        }

        Timestamp now = timeHelper.getCurrentTimestamp();
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (rewriteRules) {
                    sql(AllocatorDomainRuleVO.class).eq(AllocatorDomainRuleVO_.domainUuid, msg.getUuid()).delete();
                    flush();
                }
                if (rewriteScenes) {
                    sql(AllocatorDomainSceneVO.class).eq(AllocatorDomainSceneVO_.domainUuid, msg.getUuid()).delete();
                    flush();
                }

                AllocatorDomainVO vo = findByUuid(msg.getUuid(), AllocatorDomainVO.class);
                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }
                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }
                if (msg.getState() != null) {
                    vo.setState(AllocatorDomainState.valueOf(msg.getState()));
                }
                if (rewriteRules) {
                    for (AllocatorDomainRuleVO rvo : buildRuleVOs(vo.getUuid(), msg.getRules(), now)) {
                        persist(rvo);
                    }
                }
                if (rewriteScenes) {
                    for (AllocatorDomainSceneVO svo : buildSceneVOs(vo.getUuid(), msg.getScenes(), now)) {
                        persist(svo);
                    }
                }
                merge(vo);
            }
        }.execute();
    }

    private void handle(APIDeleteAllocatorDomainMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature(msg.getUuid());
            }

            @Override
            public String getName() {
                return "delete-allocator-domain-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteAllocatorDomainEvent event = new APIDeleteAllocatorDomainEvent(msg.getId());
                try {
                    deleteAllocatorDomain(msg.getUuid(),
                            msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Enforcing);
                    bus.publish(event);
                } catch (OperationFailureException e) {
                    event.setError(e.getErrorCode());
                    bus.publish(event);
                } catch (Throwable t) {
                    logger.warn(String.format("failed to delete allocator domain[uuid:%s]", msg.getUuid()), t);
                    event.setError(inerr(t.getMessage()));
                    bus.publish(event);
                } finally {
                    chain.next();
                }
            }
        });
    }

    private void handle(AllocatorDomainDeletionMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature(msg.getUuid());
            }

            @Override
            public String getName() {
                return "delete-allocator-domain-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                AllocatorDomainDeletionReply reply = new AllocatorDomainDeletionReply();
                try {
                    deleteAllocatorDomain(msg.getUuid(), msg.isForceDelete());
                } catch (OperationFailureException e) {
                    reply.setError(e.getErrorCode());
                } catch (Throwable t) {
                    logger.warn(String.format("failed to delete allocator domain[uuid:%s]", msg.getUuid()), t);
                    reply.setError(inerr(t.getMessage()));
                } finally {
                    bus.reply(msg, reply);
                    chain.next();
                }
            }
        });
    }

    private void deleteAllocatorDomain(String uuid, boolean forceDelete) {
        AllocatorDomainVO vo = dbf.findByUuid(uuid, AllocatorDomainVO.class);
        if (vo == null) {
            return;
        }

        List<String> referringVmUuids = listReferringVmUuids(uuid);
        if (!referringVmUuids.isEmpty()) {
            if (!forceDelete) {
                throw err(AllocatorDomainErrors.DOMAIN_IN_USE,
                        "allocator domain[uuid:%s] is still referenced by VMs %s and cannot be deleted",
                        uuid, referringVmUuids)
                        .toException();
            }
            cleanupVmReferences(uuid);
        }

        dbf.remove(vo);
    }

    private void handle(APIGetAllocatorDomainVmsMsg msg) {
        APIGetAllocatorDomainVmsReply reply = new APIGetAllocatorDomainVmsReply();
        List<String> currentVms = listCurrentVmUuids(msg.getUuid());
        Set<String> currentSet = new HashSet<>(currentVms);
        List<String> nextStartVms = listConfigVmUuids(msg.getUuid()).stream()
                .filter(uuid -> !currentSet.contains(uuid))
                .collect(Collectors.toList());
        reply.setCurrentVms(currentVms);
        reply.setNextStartVms(nextStartVms);
        bus.reply(msg, reply);
    }

    private void handle(APIGetAllocatorDomainCandidatesMsg msg) {
        String zoneUuid = msg.getZoneUuid();
        APIGetAllocatorDomainCandidatesReply reply = new APIGetAllocatorDomainCandidatesReply();
        reply.setHosts(HostInventory.valueOf(Q.New(HostVO.class)
                .eq(HostVO_.zoneUuid, zoneUuid)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.status, HostStatus.Connected)
                .list()));
        reply.setClusters(ClusterInventory.valueOf(Q.New(ClusterVO.class)
                .eq(ClusterVO_.zoneUuid, zoneUuid)
                .eq(ClusterVO_.state, ClusterState.Enabled)
                .list()));
        reply.setPrimaryStorages(PrimaryStorageInventory.valueOf(Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                .list()));
        reply.setL3Networks(L3NetworkInventory.valueOf(Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.zoneUuid, zoneUuid)
                .eq(L3NetworkVO_.state, L3NetworkState.Enabled)
                .list()));
        bus.reply(msg, reply);
    }

    // ------------------------------------- Utility Handers ------------------------------------

    private void persistRules(String domainUuid, List<AllocatorDomainRuleParam> rules, Timestamp now) {
        List<AllocatorDomainRuleVO> vos = buildRuleVOs(domainUuid, rules, now);
        if (!vos.isEmpty()) {
            dbf.persistCollection(vos);
        }
    }

    private void persistScenes(String domainUuid, List<String> scenes, Timestamp now) {
        List<AllocatorDomainSceneVO> vos = buildSceneVOs(domainUuid, scenes, now);
        if (!vos.isEmpty()) {
            dbf.persistCollection(vos);
        }
    }

    private List<AllocatorDomainRuleVO> buildRuleVOs(String domainUuid, List<AllocatorDomainRuleParam> rules, Timestamp now) {
        List<AllocatorDomainRuleVO> vos = new ArrayList<>();
        if (rules == null || rules.isEmpty()) {
            return vos;
        }
        int priority = 0;
        for (AllocatorDomainRuleParam param : rules) {
            AllocatorDomainRuleVO rvo = new AllocatorDomainRuleVO();
            rvo.setDomainUuid(domainUuid);
            rvo.setPriority(priority++);
            rvo.setResourceUuid(param.getResourceUuid());
            rvo.setResourceType(param.getResourceType());
            rvo.setResourceQualifier(param.getResourceQualifier());
            rvo.setEffect(AllocatorDomainRuleEffect.valueOf(param.getEffect()));
            rvo.setCreateDate(now);
            vos.add(rvo);
        }
        return vos;
    }

    private List<AllocatorDomainSceneVO> buildSceneVOs(String domainUuid, List<String> scenes, Timestamp now) {
        List<AllocatorDomainSceneVO> vos = new ArrayList<>();
        if (scenes == null || scenes.isEmpty()) {
            return vos;
        }
        for (String scene : scenes) {
            AllocatorDomainSceneVO svo = new AllocatorDomainSceneVO();
            svo.setDomainUuid(domainUuid);
            svo.setScene(AllocatorDomainScene.valueOf(scene));
            svo.setCreateDate(now);
            vos.add(svo);
        }
        return vos;
    }

    private AllocatorDomainInventory toInventory(String uuid) {
        return AllocatorDomainInventory.valueOf(dbf.reload(dbf.findByUuid(uuid, AllocatorDomainVO.class)));
    }

    private List<String> listReferringVmUuids(String domainUuid) {
        Set<String> uuids = new HashSet<>();
        uuids.addAll(listCurrentVmUuids(domainUuid));
        uuids.addAll(listConfigVmUuids(domainUuid));
        return new ArrayList<>(uuids);
    }

    @SuppressWarnings("unchecked")
    private List<String> listCurrentVmUuids(String domainUuid) {
        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                map(e(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN, domainUuid)));
        List<String> uuids = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.tag, tag)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .select(SystemTagVO_.resourceUuid)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    private List<String> listConfigVmUuids(String domainUuid) {
        List<String> uuids = Q.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.category, AllocatorDomainGlobalConfig.CATEGORY)
                .eq(ResourceConfigVO_.name, AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME)
                .eq(ResourceConfigVO_.value, domainUuid)
                .select(ResourceConfigVO_.resourceUuid)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    @SuppressWarnings("unchecked")
    private void cleanupVmReferences(String domainUuid) {
        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                map(e(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN, domainUuid)));
        SQL.New(SystemTagVO.class)
                .eq(SystemTagVO_.tag, tag)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .delete();
        SQL.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.category, AllocatorDomainGlobalConfig.CATEGORY)
                .eq(ResourceConfigVO_.name, AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME)
                .eq(ResourceConfigVO_.value, domainUuid)
                .delete();
        logger.debug(String.format("cleaned VM references of allocator domain[uuid:%s] on force delete", domainUuid));
    }
}
