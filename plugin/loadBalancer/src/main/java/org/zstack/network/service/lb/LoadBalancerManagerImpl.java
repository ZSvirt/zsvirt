package org.zstack.network.service.lb;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.acl.AccessControlListEntryVO;
import org.zstack.header.acl.AccessControlListEntryVO_;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.tag.*;
import org.zstack.header.vm.VmNicChangeNetworkExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.identity.Account;
import org.zstack.network.service.lb.quota.LoadBalanceListenerNumQuotaDefinition;
import org.zstack.network.service.lb.quota.LoadBalanceNumQuotaDefinition;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerManagerImpl extends AbstractService implements LoadBalancerManager,
        AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, VipGetUsedPortRangeExtensionPoint, VipGetServiceReferencePoint, VmNicChangeNetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LoadBalancerManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;

    private Map<String, LoadBalancerBackend> backends = new HashMap<String, LoadBalancerBackend>();
    private Map<String, LoadBalancerFactory> lbFactories = new HashMap<String, LoadBalancerFactory>();
    private Map<String, LoadBalancerFactory> lbFactoriesByApplianceVmType = new HashMap<String, LoadBalancerFactory>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof LoadBalancerMessage) {
            passThrough((LoadBalancerMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(LoadBalancerMessage msg) {
        LoadBalancerVO vo = dbf.findByUuid(msg.getLoadBalancerUuid(), LoadBalancerVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the load balancer[uuid:%s]", msg.getLoadBalancerUuid()));
        }

        LoadBalancerBase base = new LoadBalancerBase(vo);
        base.handleMessage((Message) msg);
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CertificateDeletionMsg) {
            handle((CertificateDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CertificateDeletionMsg msg) {
        CertificateDeletionReply reply = new CertificateDeletionReply();
        dbf.removeByPrimaryKey(msg.getUuid(), CertificateVO.class);
        bus.reply(msg, reply);
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateLoadBalancerMsg) {
            handle((APICreateLoadBalancerMsg) msg);
        } else if (msg instanceof APICreateCertificateMsg) {
            handle((APICreateCertificateMsg) msg);
        } else if (msg instanceof APIDeleteCertificateMsg) {
            handle((APIDeleteCertificateMsg) msg);
        } else if (msg instanceof APIUpdateCertificateMsg) {
            handle((APIUpdateCertificateMsg) msg);
        } else if (msg instanceof APIGetLoadBalancerListenerACLEntriesMsg) {
            handle((APIGetLoadBalancerListenerACLEntriesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetLoadBalancerListenerACLEntriesMsg msg) {
        APIGetLoadBalancerListenerACLEntriesReply reply = new APIGetLoadBalancerListenerACLEntriesReply();
        HashMap<String, List<APIGetLoadBalancerListenerACLEntriesReply.LoadBalancerACLEntry>> lbAclMap = new HashMap<>();
        if (msg.getListenerUuids().isEmpty()) {
            reply.setInventories(lbAclMap);
            bus.reply(msg, reply);
            return;
        }

        List<LoadBalancerListenerACLRefVO> loadBalancerListenerACLRefVOS = new ArrayList<>();
        loadBalancerListenerACLRefVOS = Q.New(LoadBalancerListenerACLRefVO.class).in(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuids()).list();
        if (loadBalancerListenerACLRefVOS.isEmpty()) {
            reply.setInventories(lbAclMap);
            bus.reply(msg, reply);
            return;
        }

        List<String> aclUuids = loadBalancerListenerACLRefVOS.stream().map(LoadBalancerListenerACLRefVO::getAclUuid).collect(Collectors.toList());
        List<AccessControlListEntryVO> aclEntries = Q.New(AccessControlListEntryVO.class).in(AccessControlListEntryVO_.aclUuid, aclUuids).list();

        for (String listenUuid : msg.getListenerUuids()) {
            List<String> aclOfListenerUuids = new ArrayList<>();
            if (StringUtils.isBlank(msg.getType())) {
                aclOfListenerUuids = loadBalancerListenerACLRefVOS.stream().filter(ref -> ref.getListenerUuid().equals(listenUuid))
                        .map(LoadBalancerListenerACLRefVO::getAclUuid).collect(Collectors.toList());
            } else {
                aclOfListenerUuids = loadBalancerListenerACLRefVOS.stream().filter(ref -> ref.getListenerUuid().equals(listenUuid) && msg.getType().equals(ref.getType().toString()))
                        .map(LoadBalancerListenerACLRefVO::getAclUuid).collect(Collectors.toList());
            }
            aclOfListenerUuids = aclOfListenerUuids.stream().distinct().collect(Collectors.toList());
            ArrayList<APIGetLoadBalancerListenerACLEntriesReply.LoadBalancerACLEntry> entries = new ArrayList<>();
            for (String aclUuid : aclOfListenerUuids) {
                List<AccessControlListEntryVO> aclEntriesOfAcl = aclEntries.stream().filter(aclEntry -> StringUtils.equals(aclEntry.getAclUuid(), aclUuid)).collect(Collectors.toList());
                for (AccessControlListEntryVO entryVO : aclEntriesOfAcl) {
                    APIGetLoadBalancerListenerACLEntriesReply.LoadBalancerACLEntry loadBalancerACLEntry = new APIGetLoadBalancerListenerACLEntriesReply.LoadBalancerACLEntry();
                    loadBalancerACLEntry.valueOf(entryVO);
                    List<String> serverGroupUuids = loadBalancerListenerACLRefVOS.stream().filter(ref -> ref.getAclUuid().equals(aclUuid) && ref.getListenerUuid().equals(listenUuid) && ref.getServerGroupUuid()!=null).map(LoadBalancerListenerACLRefVO::getServerGroupUuid).collect(Collectors.toList());
                    if (!serverGroupUuids.isEmpty()) {
                        List<LoadBalancerServerGroupVO> serverGroupVOS = Q.New(LoadBalancerServerGroupVO.class).in(LoadBalancerServerGroupVO_.uuid, serverGroupUuids).list();
                        loadBalancerACLEntry.setServerGroups(LoadBalancerServerGroupInventory.valueOf(serverGroupVOS));
                    }
                    entries.add(loadBalancerACLEntry);
                }
            }
            lbAclMap.put(listenUuid, entries);
        }
        reply.setInventories(lbAclMap);
        bus.reply(msg, reply);
    }

    private void handle(final APICreateLoadBalancerMsg msg) {
        final APICreateLoadBalancerEvent evt = new APICreateLoadBalancerEvent(msg.getId());

        String type = msg.getType() != null ? msg.getType() : LoadBalancerType.Shared.toString();
        LoadBalancerFactory f = getLoadBalancerFactory(type);
        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(msg.getVipUuid(), VipVO.class));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-lb-%s", msg.getName()));
        chain.then(new ShareFlow() {
            LoadBalancerVO vo;

            @Override
            public void setup() {

                flow(new Flow() {
                    String __name__ = "write-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vo = f.persistLoadBalancer(msg);
                        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), LoadBalancerVO.class.getSimpleName());

                        LoadBalancerServerGroupVO groupVO = new LoadBalancerServerGroupVO();
                        groupVO.setUuid(Platform.getUuid());
                        groupVO.setAccountUuid(msg.getSession().getAccountUuid());
                        groupVO.setDescription(String.format("default server group for load balancer %s", msg.getName()));
                        groupVO.setLoadBalancerUuid(vo.getUuid());
                        groupVO.setName(String.format("default-server-group-%s", msg.getName()));
                        dbf.persist(groupVO);

                        vo = dbf.reload(vo);
                        vo.setServerGroupUuid(groupVO.getUuid());
                        vo = dbf.updateAndRefresh(vo);

                        /* put vo to data for rollback */
                        data.put(LoadBalancerConstants.Param.LOAD_BALANCER_VO, vo);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        LoadBalancerVO vo = (LoadBalancerVO)data.get(LoadBalancerConstants.Param.LOAD_BALANCER_VO);
                        if (vo != null) {
                            dbf.removeByPrimaryKey(vo.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                            f.deleteLoadBalancer(vo);
                        }
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "acquire-vip";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(f.getNetworkServiceType());
                        struct.setServiceUuid(vo.getUuid());
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(struct);
                        v.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        LoadBalancerVO vo = (LoadBalancerVO)data.get(LoadBalancerConstants.Param.LOAD_BALANCER_VO);
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(f.getNetworkServiceType());
                        struct.setServiceUuid(vo.getUuid());
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(struct);
                        v.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO add GC
                                logger.warn(errorCode.getReadableDetails());
                                trigger.rollback();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(LoadBalancerInventory.valueOf(dbf.reload(vo)));
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

    private void handle(final APICreateCertificateMsg msg) {
        final APICreateCertificateEvent evt = new APICreateCertificateEvent(msg.getId());

        CertificateVO vo = new CertificateVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        vo.setCertificate(msg.getCertificate());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), CertificateVO.class.getSimpleName());

        evt.setInventory(CertificateInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(final APIDeleteCertificateMsg msg) {
        final APIDeleteCertificateEvent evt = new APIDeleteCertificateEvent(msg.getId());

        CertificateInventory inv = CertificateInventory.valueOf(dbf.findByUuid(msg.getUuid(), CertificateVO.class));
        SQL.New(CertificateVO.class).eq(CertificateVO_.uuid, msg.getUuid()).delete();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-certificate-%s", msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (isEmpty(inv.getListeners())) {
                            trigger.next();
                            return;
                        }

                        List<String> lbUuidList = Q.New(LoadBalancerListenerVO.class)
                                .in(LoadBalancerListenerVO_.uuid, transform(inv.getListeners(), LoadBalancerListenerCertificateRefInventory::getListenerUuid))
                                .select(LoadBalancerListenerVO_.loadBalancerUuid)
                                .listValues();

                        if (isEmpty(lbUuidList)) {
                            trigger.next();
                            return;
                        }

                        new While<>(new HashSet<>(lbUuidList)).each((lbUuid, wcompl) -> {
                            LoadBalancerChangeCertificateMsg cmsg = new LoadBalancerChangeCertificateMsg();
                            cmsg.setLoadBalancerUuid(lbUuid);
                            cmsg.setCertificateUuid(null);
                            bus.makeLocalServiceId(cmsg, LoadBalancerConstants.SERVICE_ID);
                            bus.send(cmsg, new CloudBusCallBack(wcompl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        wcompl.addError(reply.getError());
                                    }
                                    wcompl.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(multiErr(errorCodeList));
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
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

    private void handle(final APIUpdateCertificateMsg msg) {
        APIUpdateCertificateEvent evt = new APIUpdateCertificateEvent(msg.getId());

        CertificateVO vo = dbf.findByUuid(msg.getUuid(), CertificateVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        evt.setInventory(CertificateInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LoadBalancerConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        for (LoadBalancerBackend bkd : pluginRgty.getExtensionList(LoadBalancerBackend.class)) {
            LoadBalancerBackend old = backends.get(bkd.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate LoadBalancerBackend[%s, %s]", old.getClass(), bkd.getNetworkServiceProviderType()));
            }

            backends.put(bkd.getNetworkServiceProviderType(), bkd);
        }

        for (LoadBalancerFactory f : pluginRgty.getExtensionList(LoadBalancerFactory.class)) {
            LoadBalancerFactory old = lbFactories.get(f.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate LoadBalancerFactory[%s, %s]", old.getClass(), f.getType()));
            }

            lbFactories.put(f.getType(), f);
        }

        for (LoadBalancerFactory f : pluginRgty.getExtensionList(LoadBalancerFactory.class)) {
            LoadBalancerFactory old = lbFactoriesByApplianceVmType.get(f.getApplianceVmType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate LoadBalancerFactory[%s, %s]", old.getClass(), f.getType()));
            }

            lbFactoriesByApplianceVmType.put(f.getApplianceVmType(), f);
        }

        installConfigValidateExtension();
        prepareSystemTags();

        upgradeLoadBalancerServerGroup();
        return true;
    }


    private void installConfigValidateExtension(){
        LoadBalancerGlobalConfig.HTTP_MODE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                List<String> httpModes = new ArrayList<>(Arrays.asList("http-keep-alive",
                                                                        "http-server-close",
                                                                        "http-tunnel",
                                                                        "httpclose",
                                                                        "forceclose"));
                if (!httpModes.contains(value)) {
                    throw new GlobalConfigException(String.format("%s must be in %s",
                            LoadBalancerGlobalConfig.HTTP_MODE.getName(),String.join(", ",httpModes)));
                }
            }
        });

    }

    private void prepareSystemTags() {
        AbstractSystemTagOperationJudger judger = new AbstractSystemTagOperationJudger() {
            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                throw new OperationFailureException(operr("cannot delete the system tag[%s]. The load balancer plugin relies on it, you can only update it", tag.getTag()));
            }
        };
        LoadBalancerSystemTags.BALANCER_ALGORITHM.installJudger(judger);
        LoadBalancerSystemTags.HEALTHY_THRESHOLD.installJudger(judger);
        LoadBalancerSystemTags.MAX_CONNECTION.installJudger(judger);
        LoadBalancerSystemTags.NUMBER_OF_PROCESS.installJudger(judger);
        LoadBalancerSystemTags.HEALTH_INTERVAL.installJudger(judger);
        LoadBalancerSystemTags.HEALTH_TARGET.installJudger(judger);
        LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.installJudger(judger);
        LoadBalancerSystemTags.HEALTH_TIMEOUT.installJudger(judger);
        LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.installJudger(judger);

        LoadBalancerSystemTags.BALANCER_WEIGHT.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                Map<String, String> tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensByTag(systemTag);
                String nicUuid = tokens.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN);
                if (!dbf.isExist(nicUuid, VmNicVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("nic[uuid:%s] not found. Please correct your system tag[%s] of loadbalancer",
                            nicUuid, systemTag));
                }

                String s = tokens.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN);
                try {
                    long weight = Long.parseLong(s);
                    if (weight < LoadBalancerConstants.BALANCER_WEIGHT_MIN || weight > LoadBalancerConstants.BALANCER_WEIGHT_MAX) {
                        throw new OperationFailureException(argerr("invalid balancer weight[%s], %s is not in the range [%d, %d]",
                                systemTag, s, LoadBalancerConstants.BALANCER_WEIGHT_MIN, LoadBalancerConstants.BALANCER_WEIGHT_MAX));
                    }
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid balancer weight[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.BALANCER_ALGORITHM.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String algorithm = LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN);

                if (!LoadBalancerConstants.BALANCE_ALGORITHMS.contains(algorithm)) {
                    throw new OperationFailureException(argerr("invalid balance algorithm[%s], valid algorithms are %s", algorithm, LoadBalancerConstants.BALANCE_ALGORITHMS));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_PARAMETER.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                /*miaozhanyong to be done*/
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {

            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                if (!LoadBalancerSystemTags.HEALTH_PARAMETER.isMatch(newTag.getTag())) {
                    return;
                }
                /*miaozhanyong to be done*/
            }
        });

        LoadBalancerSystemTags.BALANCER_ALGORITHM.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {

            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {

            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                if (!LoadBalancerSystemTags.BALANCER_ALGORITHM.isMatch(newTag.getTag())) {
                    return;
                }

                String oldValue = LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByTag(old.getTag(), LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN);
                String newValue = LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByTag(newTag.getTag(), LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN);

                String defaultServerGroupUuid = Q.New(LoadBalancerListenerVO.class)
                        .select(LoadBalancerListenerVO_.serverGroupUuid)
                        .eq(LoadBalancerListenerVO_.uuid, newTag.getResourceUuid())
                        .findValue();
                if(defaultServerGroupUuid == null){
                    return ;
                }
                List<String> nicUuids = Q.New(LoadBalancerServerGroupVmNicRefVO.class).select(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid)
                        .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, defaultServerGroupUuid).listValues();
                if (nicUuids.isEmpty()) {
                    return;
                }

                if (!LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN.equals(oldValue) && LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN.equals(newValue)) {
                    nicUuids.forEach(nicUuid -> new LoadBalancerWeightOperator().setWeight(newTag.getResourceUuid(), nicUuid, LoadBalancerConstants.BALANCER_WEIGHT_default));
                }
                if (LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN.equals(oldValue) && !LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN.equals(newValue)) {
                    new LoadBalancerWeightOperator().deleteNicsWeight(nicUuids, newTag.getResourceUuid());
                }
            }
        });

        LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid unhealthy threshold[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTHY_THRESHOLD.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.HEALTHY_THRESHOLD.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid healthy threshold[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_TIMEOUT.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.HEALTH_TIMEOUT.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTH_TIMEOUT_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid healthy timeout[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid connection idle timeout[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_INTERVAL.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.HEALTH_INTERVAL.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid health check interval[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.MAX_CONNECTION.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.MAX_CONNECTION.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.MAX_CONNECTION_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid max connection[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.NUMBER_OF_PROCESS.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.NUMBER_OF_PROCESS.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.NUMBER_OF_PROCESS_TOKEN);

                try {
                    Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid process number[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_TARGET.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String target = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);

                String[] ts = target.split(":");
                if (ts.length != 2) {
                    throw new OperationFailureException(argerr("invalid health target[%s], the format is targetCheckProtocol:port, for example, tcp:default", systemTag));
                }

                String protocol = ts[0];
                if (!LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCOLS.contains(protocol)) {
                    throw new OperationFailureException(argerr("invalid health target[%s], the target checking protocol[%s] is invalid, valid protocols are %s",
                            systemTag, protocol, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCOLS));
                }

                String port = ts[1];
                if (!"default".equals(port)) {
                    try {
                        int p = Integer.parseInt(port);
                        if (p < 1 || p > 65535) {
                            throw new OperationFailureException(argerr("invalid invalid health target[%s], port[%s] is not in the range of [1, 65535]", systemTag, port));
                        }
                    } catch (NumberFormatException e) {
                        throw new OperationFailureException(argerr("invalid invalid health target[%s], port[%s] is not a number", systemTag, port));
                    }
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public LoadBalancerBackend getBackend(String providerType) {
        LoadBalancerBackend bkd = backends.get(providerType);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find LoadBalancerBackend[provider type:%s]", providerType));
        }
        return bkd;
    }

    @Override
    public LoadBalancerFactory getLoadBalancerFactory(String type) {
        LoadBalancerFactory f = lbFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find LoadBalancerFactory[type:%s]", type));
        }
        return f;
    }

    @Override
    public LoadBalancerFactory getLoadBalancerFactoryByApplianceVmType(String applianceVmType) {
        LoadBalancerFactory f = lbFactories.get(applianceVmType);
        return f;
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct s = new ExpandedQueryStruct();
        s.setExpandedField("loadBalancerListenerRef");
        s.setHidden(true);
        s.setForeignKey("uuid");
        s.setExpandedInventoryKey("vmNicUuid");
        s.setInventoryClassToExpand(VmNicInventory.class);
        s.setInventoryClass(LoadBalancerListenerVmNicRefInventory.class);
        structs.add(s);

        s = new ExpandedQueryStruct();
        s.setInventoryClassToExpand(VipInventory.class);
        s.setExpandedField("loadBalancer");
        s.setInventoryClass(LoadBalancerInventory.class);
        s.setForeignKey("uuid");
        s.setExpandedInventoryKey("vipUuid");
        structs.add(s);
        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        List<ExpandedQueryAliasStruct> structs = new ArrayList<ExpandedQueryAliasStruct>();

        ExpandedQueryAliasStruct s = new ExpandedQueryAliasStruct();
        s.setInventoryClass(VmNicInventory.class);
        s.setAlias("loadBalancerListener");
        s.setExpandedField("loadBalancerListenerRef.listener");
        structs.add(s);
        return structs;
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new LoadBalanceNumQuotaDefinition());
        quota.defineQuota(new LoadBalanceListenerNumQuotaDefinition());

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateLoadBalancerMsg.class)
                .addCounterQuota(LoadBalanceQuotaConstant.LOAD_BALANCER_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateLoadBalancerListenerMsg.class)
                .addCounterQuota(LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(LoadBalancerVO.class)
                        .eq(LoadBalancerVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(LoadBalanceQuotaConstant.LOAD_BALANCER_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(LoadBalancerListenerVO.class)
                        .eq(LoadBalancerListenerVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM));

        return list(quota);
    }

    @Override
    public RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList){

        RangeSet portRangeList = new RangeSet();
        List<RangeSet.Range> portRanges = new ArrayList<RangeSet.Range>();
        List<String> protocols = new ArrayList<>();
        if (LoadBalancerConstants.LB_PROTOCOL_UDP.equalsIgnoreCase(protocol.toLowerCase())) {
            protocols.add(LoadBalancerConstants.LB_PROTOCOL_UDP);
        } else {
            protocols.add(LoadBalancerConstants.LB_PROTOCOL_TCP);
            protocols.add(LoadBalancerConstants.LB_PROTOCOL_HTTP);
            protocols.add(LoadBalancerConstants.LB_PROTOCOL_HTTPS);
        }

        List<Tuple> lbPortList = SQL.New("select lbl.loadBalancerPort, lbl.loadBalancerPort from LoadBalancerListenerVO lbl, LoadBalancerVO lb "
                + "where lbl.protocol in (:protocols) and lbl.loadBalancerUuid=lb.uuid and lb.vipUuid = :vipUuid", Tuple.class).
                param("protocols", protocols).
                param("vipUuid", vipUuid).list();

        for (Tuple strRange : lbPortList) {
            int start = strRange.get(0, Integer.class);
            int end = strRange.get(1, Integer.class);

            RangeSet.Range range = new RangeSet.Range(start, end);
            portRanges.add(range);
        }
        portRangeList.setRanges(portRanges);

        return portRangeList;
    }

    @Override
    public ServiceReference getServiceReference(String vipUuid) {
        long count = 0;
        List<String> l3Uuids = SQL.New("select nic.l3NetworkUuid" +
                " from LoadBalancerVO lb, LoadBalancerServerGroupVO g, LoadBalancerListenerVO listener, VmNicVO nic, " +
                " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef" +
                " where lb.vipUuid = :vipUuid and lb.uuid = listener.loadBalancerUuid" +
                " and listener.uuid = lgref.listenerUuid and lgref.serverGroupUuid = g.uuid " +
                " and g.uuid = nicRef.serverGroupUuid and nicRef.vmNicUuid = nic.uuid" +
                " and nicRef.status != 'Pending'")
                        .param("vipUuid", vipUuid).list();
        if (l3Uuids != null && !l3Uuids.isEmpty()) {
            count = new HashSet<>(l3Uuids).size();
        }
        List<String> uuids = SQL.New("select distinct lb.uuid" +
                " from LoadBalancerVO lb, LoadBalancerServerGroupVO g, LoadBalancerListenerVO listener, VmNicVO nic, " +
                " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef" +
                " where lb.vipUuid = :vipUuid and lb.uuid = listener.loadBalancerUuid" +
                " and listener.uuid = lgref.listenerUuid and lgref.serverGroupUuid = g.uuid " +
                " and g.uuid = nicRef.serverGroupUuid and nicRef.vmNicUuid = nic.uuid" +
                " and nicRef.status != 'Pending'")
                .param("vipUuid", vipUuid).list();

        return new VipGetServiceReferencePoint.ServiceReference(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING, count, uuids == null?new ArrayList<>() : uuids);
    }

    @Override
    public ServiceReference getServicePeerL3Reference(String vipUuid, String peerL3Uuid) {
        long count = 0;
        List<String> l3Uuids = SQL.New("select nic.l3NetworkUuid" +
                " from LoadBalancerVO lb, LoadBalancerServerGroupVO g, LoadBalancerListenerVO listener, VmNicVO nic, " +
                " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef" +
                " where lb.vipUuid = :vipUuid and lb.uuid = listener.loadBalancerUuid" +
                " and listener.uuid = lgref.listenerUuid and lgref.serverGroupUuid = g.uuid " +
                " and g.uuid = nicRef.serverGroupUuid and nicRef.vmNicUuid = nic.uuid" +
                " and nicRef.status != 'Pending' and nic.l3NetworkUuid = :l3uuid")
                .param("vipUuid", vipUuid).param("l3uuid", peerL3Uuid).list();
        if (l3Uuids != null && !l3Uuids.isEmpty()) {
            count = new HashSet<>(l3Uuids).size();
        }
        List<String> uuids = SQL.New("select distinct lb.uuid" +
                " from LoadBalancerVO lb, LoadBalancerServerGroupVO g, LoadBalancerListenerVO listener, VmNicVO nic, " +
                " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef" +
                " where lb.vipUuid = :vipUuid and lb.uuid = listener.loadBalancerUuid" +
                " and listener.uuid = lgref.listenerUuid and lgref.serverGroupUuid = g.uuid " +
                " and g.uuid = nicRef.serverGroupUuid and nicRef.vmNicUuid = nic.uuid" +
                " and nicRef.status != 'Pending' and nic.l3NetworkUuid = :l3uuid")
                .param("vipUuid", vipUuid).param("l3uuid", peerL3Uuid).list();


        return new VipGetServiceReferencePoint.ServiceReference(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING, count, uuids == null?new ArrayList<>() : uuids);
    }

    @Override
    public LoadBalancerStruct makeStruct(LoadBalancerVO vo) {
        vo = dbf.reload(vo);
        LoadBalancerStruct struct = new LoadBalancerStruct();
        struct.setLb(LoadBalancerInventory.valueOf(vo));
        struct.setVip(VipInventory.valueOf(dbf.findByUuid(vo.getVipUuid(), VipVO.class)));

        List<String> activeNicUuids = new ArrayList<String>();
        for (LoadBalancerListenerVO l : vo.getListeners()) {
            List<LoadBalancerServerGroupInventory> groupInventories = new ArrayList<>();
            for (LoadBalancerListenerServerGroupRefVO groupRef : l.getServerGroupRefs()) {
                LoadBalancerServerGroupVO groupVO = dbf.findByUuid(groupRef.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                for (LoadBalancerServerGroupVmNicRefVO nicRef : groupVO.getLoadBalancerServerGroupVmNicRefs()) {
                    if (nicRef.getStatus() == LoadBalancerVmNicStatus.Active || nicRef.getStatus() == LoadBalancerVmNicStatus.Pending) {
                        activeNicUuids.add(nicRef.getVmNicUuid());
                    }
                }
                groupInventories.add(LoadBalancerServerGroupInventory.valueOf(groupVO));
            }
            struct.getListenerServerGroupMap().put(l.getUuid(), groupInventories);
        }

        if (activeNicUuids.isEmpty()) {
            struct.setVmNics(new HashMap<String, VmNicInventory>());
        } else {
            SimpleQuery<VmNicVO> nq = dbf.createQuery(VmNicVO.class);
            nq.add(VmNicVO_.uuid, SimpleQuery.Op.IN, activeNicUuids);
            List<VmNicVO> nicvos = nq.list();
            Map<String, VmNicInventory> m = new HashMap<>();
            for (VmNicVO n : nicvos) {
                m.put(n.getUuid(), VmNicInventory.valueOf(n));
            }
            struct.setVmNics(m);
        }

        Map<String, List<String>> systemTags = new HashMap<>();
        for (LoadBalancerListenerVO l : vo.getListeners()) {
            SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
            q.select(SystemTagVO_.tag);
            q.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, l.getUuid());
            q.add(SystemTagVO_.resourceType, SimpleQuery.Op.EQ, LoadBalancerListenerVO.class.getSimpleName());
            systemTags.put(l.getUuid(), q.listValue());
        }
        struct.setTags(systemTags);

        struct.setListeners(LoadBalancerListenerInventory.valueOf(vo.getListeners()));

        return struct;
    }

    @Override
    public LoadBalancerServerGroupVO getDefaultServerGroup(LoadBalancerVO vo) {
        if (vo.getServerGroupUuid() == null) {
            return null;
        }
        return dbf.findByUuid(vo.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
    }

    @Override
    public LoadBalancerServerGroupVO getDefaultServerGroup(LoadBalancerListenerVO vo) {
        if (vo.getServerGroupUuid() == null) {
            return null;
        }

        return dbf.findByUuid(vo.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
    }

    @Override
    public List<String> getLoadBalancerListenterByVmNics(List<String> vmNicUuids) {
        List<String> listenerUuids = SQL.New("select distinct listener.uuid " +
                " from LoadBalancerServerGroupVO g, LoadBalancerListenerVO listener, VmNicVO nic, " +
                " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef" +
                " where listener.uuid = lgref.listenerUuid and lgref.serverGroupUuid = g.uuid " +
                " and g.uuid = nicRef.serverGroupUuid and nicRef.vmNicUuid in (:vmNicUuids)")
                .param("vmNicUuids", vmNicUuids).list();
        return listenerUuids;
    }


    @Override
    public void upgradeLoadBalancerServerGroup() {
        if (!LoadBalancerGlobalProperty.UPGRADE_LB_SERVER_GROUP) {
            /* lb upgrade failed check */
            List<LoadBalancerListenerVO> listenerVOS = Q.New(LoadBalancerListenerVO.class).list();
            for (LoadBalancerListenerVO vo : listenerVOS) {
                /* if listener has old vmnic attached directly, a related lb server group must has been crated  */
                List<String> nicUuids = vo.getVmNicRefs().stream().map(LoadBalancerListenerVmNicRefVO::getVmNicUuid)
                        .collect(Collectors.toList());
                if (!nicUuids.isEmpty()) {
                    List<String> uuids = new ArrayList<>();
                    for (LoadBalancerListenerServerGroupRefVO ref : vo.getServerGroupRefs()) {
                        LoadBalancerServerGroupVO groupVO = dbf.findByUuid(ref.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                        uuids.addAll(groupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                                .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList()));
                    }

                    if (!uuids.containsAll(nicUuids)) {
                        throw new CloudRuntimeException(String.format("load balancer listener [uuid:%s] upgraded failed," +
                                "vmnics directly attached are [%s], vmnic attached to its serverGroup are %s",
                                vo.getUuid(), nicUuids, uuids));
                    }
                }
            }
            return;
        }

        List<LoadBalancerVO> lbVos = Q.New(LoadBalancerVO.class).list();
        for (LoadBalancerVO vo : lbVos) {
            boolean isExist = Q.New(LoadBalancerServerGroupVO.class).eq(LoadBalancerServerGroupVO_.name, String.format("default-server-group-%s", vo.getName()))
                    .eq(LoadBalancerServerGroupVO_.description, String.format("default server group for load balancer %s", vo.getName()))
                    .eq(LoadBalancerServerGroupVO_.loadBalancerUuid, vo.getUuid()).isExists();
            if (isExist) {
                continue;
            }
            /* create a server group */
            LoadBalancerServerGroupVO groupVO = new LoadBalancerServerGroupVO();
            groupVO.setUuid(Platform.getUuid());
            groupVO.setAccountUuid(Account.getAccountUuidOfResource(vo.getUuid()));
            groupVO.setDescription(String.format("default server group for load balancer %s", vo.getName()));
            groupVO.setLoadBalancerUuid(vo.getUuid());
            groupVO.setName(String.format("default-server-group-%s", vo.getName()));
            dbf.persist(groupVO);

            vo.setServerGroupUuid(groupVO.getUuid());
            dbf.update(vo);
        }

        List<LoadBalancerListenerVO> listenerVOS = Q.New(LoadBalancerListenerVO.class).list();
        List<LoadBalancerServerGroupVmNicRefVO> vmNicRefVOS = new ArrayList<>();
        for (LoadBalancerListenerVO vo : listenerVOS) {
            boolean isExist = Q.New(LoadBalancerServerGroupVO.class).eq(LoadBalancerServerGroupVO_.name, String.format("default-server-group-%s-%s", vo.getName(), vo.getUuid().substring(0, 5)))
                    .eq(LoadBalancerServerGroupVO_.description, String.format("default server group for load balancer listener %s", vo.getName()))
                    .eq(LoadBalancerServerGroupVO_.loadBalancerUuid, vo.getLoadBalancerUuid()).isExists();
            if (isExist) {
                continue;
            }
            /* create a server group */
            LoadBalancerServerGroupVO groupVO = new LoadBalancerServerGroupVO();
            groupVO.setUuid(Platform.getUuid());
            groupVO.setAccountUuid(Account.getAccountUuidOfResource(vo.getUuid()));
            groupVO.setDescription(String.format("default server group for load balancer listener %s", vo.getName()));
            groupVO.setLoadBalancerUuid(vo.getLoadBalancerUuid());
            groupVO.setName(String.format("default-server-group-%s-%s", vo.getName(), vo.getUuid().substring(0, 5)));
            dbf.persist(groupVO);

            vo.setServerGroupUuid(groupVO.getUuid());
            dbf.update(vo);

            Map<String, Long> weight = new LoadBalancerWeightOperator().getWeight(vo.getUuid());
            for(LoadBalancerListenerVmNicRefVO ref : vo.getVmNicRefs()) {
                LoadBalancerServerGroupVmNicRefVO vmNicRef = new LoadBalancerServerGroupVmNicRefVO();
                vmNicRef.setServerGroupUuid(groupVO.getUuid());
                vmNicRef.setVmNicUuid(ref.getVmNicUuid());
                if (weight.get(ref.getVmNicUuid()) != null) {
                    vmNicRef.setWeight(weight.get(ref.getVmNicUuid()));
                } else {
                    vmNicRef.setWeight(LoadBalancerConstants.BALANCER_WEIGHT_default);
                }
                vmNicRef.setStatus(ref.getStatus());
                vmNicRefVOS.add(vmNicRef);
            }

            /* attach server group to listener */
            LoadBalancerListenerServerGroupRefVO ref = new LoadBalancerListenerServerGroupRefVO();
            ref.setListenerUuid(vo.getUuid());
            ref.setServerGroupUuid(groupVO.getUuid());
            dbf.persist(ref);

            /* remove vmnic directly attached load balaner */
            dbf.removeCollection(vo.getVmNicRefs(), LoadBalancerListenerVmNicRefVO.class);
        }

        dbf.persistCollection(vmNicRefVOS);
    }

    @Override
    public Map<String, String> getVmNicAttachedNetworkService(VmNicInventory nic) {
        List<LoadBalancerServerGroupVO> lbSgs = SQL.New("select lbSg from LoadBalancerServerGroupVmNicRefVO ref, LoadBalancerServerGroupVO lbSg where ref.serverGroupUuid = lbSg.uuid" +
                " and ref.vmNicUuid = :vmNicUuid")
                .param("vmNicUuid", nic.getUuid())
                .list();
        if (lbSgs.isEmpty()) {
            return null;
        }
        HashMap<String, String> ret = new HashMap<>();
        for (LoadBalancerServerGroupVO lbSg : lbSgs) {
            ret.put(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                    String.format("lb uuid is [%s], serverGroupUuid is [%s]",lbSg.getLoadBalancerUuid(), lbSg.getUuid()));
        }
        return ret;
    }
}
