package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.identity.Account;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 8/8/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LoadBalancerBase {
    private static final CLogger logger = Utils.getLogger(LoadBalancerBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private LoadBalancerManager lbMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected L3NetworkManager l3Mgr;

    @Autowired
    private PluginRegistry pluginRgty;

    public static String getSyncId(String vipUuid) {
        return String.format("operate-lb-with-vip-%s", vipUuid);
    }

    private String getSyncId() {
        return getSyncId(self.getVipUuid());
    }

    private LoadBalancerVO self;

    protected LoadBalancerInventory getInventory() {
        return LoadBalancerInventory.valueOf(self);
    }

    private LoadBalancerInventory reloadAndGetInventory() {
        self = dbf.reload(self);
        return getInventory();
    }

    public LoadBalancerBase(LoadBalancerVO self) {
        this.self = self;
    }

    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof LoadBalancerActiveVmNicMsg) {
            handle((LoadBalancerActiveVmNicMsg) msg);
        } else if (msg instanceof LoadBalancerDeactiveVmNicMsg) {
            handle((LoadBalancerDeactiveVmNicMsg) msg);
        } else if (msg instanceof LoadBalancerRemoveVmNicMsg) {
            handle((LoadBalancerRemoveVmNicMsg) msg);
        } else if (msg instanceof RefreshLoadBalancerMsg) {
            handle((RefreshLoadBalancerMsg) msg);
        } else if (msg instanceof DeleteLoadBalancerMsg) {
            handle((DeleteLoadBalancerMsg) msg);
        } else if (msg instanceof DeleteLoadBalancerOnlyMsg) {
            handle((DeleteLoadBalancerOnlyMsg) msg);
        } else if (msg instanceof LoadBalancerChangeCertificateMsg) {
            handle((LoadBalancerChangeCertificateMsg) msg);
        } else if (msg instanceof AddVmNicToLoadBalancerMsg) {
            handle((AddVmNicToLoadBalancerMsg) msg);
        } else if (msg instanceof LoadBalancerGetPeerL3NetworksMsg) {
            handle((LoadBalancerGetPeerL3NetworksMsg) msg);
        } else if (msg instanceof RemoveAccessControlListFromLoadBalancerMsg) {
            handle((RemoveAccessControlListFromLoadBalancerMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteLoadBalancerOnlyMsg msg) {
        DeleteLoadBalancerOnlyReply reply = new DeleteLoadBalancerOnlyReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (dbf.reload(self) == null) {
                    /*the lb has been deleted by  the previous task*/
                    chain.next();
                    return;
                }

                LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(self.getType().toString());
                if (self.getProviderType() == null) {
                    // not initialized yet
                    deleteListenersForLoadBalancer(msg.getLoadBalancerUuid());
                    SQL.New(LoadBalancerServerGroupVO.class).eq(LoadBalancerServerGroupVO_.loadBalancerUuid, self.getUuid()).delete();
                    f.deleteLoadBalancer(self);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                LoadBalancerBackend bkd = getBackend();
                bkd.destroyLoadBalancer(lbMgr.makeStruct(self), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        deleteListenersForLoadBalancer(msg.getLoadBalancerUuid());
                        SQL.New(LoadBalancerServerGroupVO.class).eq(LoadBalancerServerGroupVO_.loadBalancerUuid, self.getUuid()).delete();
                        f.deleteLoadBalancer(self);
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
                return "delete-load-balancer-only";
            }
        });
    }

    private void deleteListenersForLoadBalancer(String lbUuid) {
        List<LoadBalancerListenerVO> listenerVOS = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.loadBalancerUuid, lbUuid)
                .list();
        if (listenerVOS != null && !listenerVOS.isEmpty()) {
            logger.debug(String.format("delete loadBalancerListeners[%s] for loadBalancer[uuid:%s]",
                    listenerVOS.stream().map(ResourceVO::getUuid).collect(Collectors.toList()), lbUuid));
            listenerVOS.forEach(vo -> {
                /*there is no cascade deleting configure for acl ref in db */
                if (!vo.getAclRefs().isEmpty()) {
                    dbf.removeCollection(vo.getAclRefs(), LoadBalancerListenerACLRefVO.class);
                }
                dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerListenerVO.class);
            });
        }
    }

    private void handle(final DeleteLoadBalancerMsg msg) {
        final DeleteLoadBalancerReply reply = new DeleteLoadBalancerReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                self = dbf.reload(self);
                if (self == null) {
                    /*the lb has been deleted by  the previous task*/
                    chain.next();
                    return;
                }

                delete(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg ,reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg ,reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-lb";
            }
        });
    }

    private void handle(final RefreshLoadBalancerMsg msg) {
        final RefreshLoadBalancerReply reply = new RefreshLoadBalancerReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refresh(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        reply.setInventory(getInventory());
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
                return "refresh-lb";
            }
        });
    }

    private void refresh(final Completion completion) {
        LoadBalancerBackend bkd = getBackend();
        if (bkd == null) {
            completion.success();
            return;
        }

        bkd.refresh(lbMgr.makeStruct(self), completion);
    }

    private void handle(final LoadBalancerRemoveVmNicMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final LoadBalancerRemoveVmNicReply reply = new LoadBalancerRemoveVmNicReply();
                removeNics(msg.getServerGroupUuids(), null, msg.getVmNicUuids(), new ArrayList<>(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SQL.New(LoadBalancerServerGroupVmNicRefVO.class)
                                .in(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, msg.getServerGroupUuids())
                                .in(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid, msg.getVmNicUuids()).delete();
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
                return "remove-nic-from-lb";
            }
        });
    }

    private void checkIfNicIsAdded(List<String> nicUuids) {
        List<String> allNicUuids = new ArrayList<String>();
        List<LoadBalancerServerGroupVO> groupVOS = Q.New(LoadBalancerServerGroupVO.class)
                .eq(LoadBalancerServerGroupVO_.loadBalancerUuid, self.getUuid()).list();
        for (LoadBalancerServerGroupVO group : groupVOS) {
            allNicUuids.addAll(group.getLoadBalancerServerGroupVmNicRefs().stream()
                    .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList()));
        }

        for (String nicUuid : nicUuids) {
            if (!allNicUuids.contains(nicUuid)) {
                throw new CloudRuntimeException(String.format("the load balancer[uuid: %s] doesn't have a vm nic[uuid: %s] added", self.getUuid(), nicUuid));
            }
        }
    }

    private void handle(final LoadBalancerDeactiveVmNicMsg msg) {
        checkIfNicIsAdded(msg.getVmNicUuids());

        List<LoadBalancerServerGroupVO> groupVOS = Q.New(LoadBalancerServerGroupVO.class)
                .in(LoadBalancerServerGroupVO_.uuid, msg.getServerGroupUuids()).list();

        final List<LoadBalancerServerGroupVmNicRefVO> refs = new ArrayList<>();
        for (LoadBalancerServerGroupVO group : groupVOS) {
            refs.addAll(group.getLoadBalancerServerGroupVmNicRefs().stream().filter(ref -> msg.getVmNicUuids().contains(ref.getVmNicUuid())).collect(Collectors.toList()));
        }

        final LoadBalancerDeactiveVmNicReply reply = new LoadBalancerDeactiveVmNicReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("deactive-vm-nics-on-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "set-nics-to-inactive-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (LoadBalancerServerGroupVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Inactive);
                            dbf.update(ref);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (LoadBalancerServerGroupVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Active);
                            dbf.update(ref);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "deactive-nics-on-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<VmNicVO> nicvos = Q.New(VmNicVO.class)
                                .in(VmNicVO_.uuid, transformAndRemoveNull(refs, LoadBalancerServerGroupVmNicRefVO::getVmNicUuid))
                                .list();

                        LoadBalancerBackend bkd = getBackend();
                        bkd.removeVmNics(lbMgr.makeStruct(self), VmNicInventory.valueOf(nicvos), new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void activeVmNic(final LoadBalancerActiveVmNicMsg msg, final NoErrorCompletion completion) {
        checkIfNicIsAdded(msg.getVmNicUuids());

        List<LoadBalancerServerGroupVO> groupVOS = Q.New(LoadBalancerServerGroupVO.class)
                .eq(LoadBalancerServerGroupVO_.uuid, msg.getServerGroupUuid()).list();

        final List<LoadBalancerServerGroupVmNicRefVO> refs = new ArrayList<>();
        for (LoadBalancerServerGroupVO group : groupVOS) {
            refs.addAll(group.getLoadBalancerServerGroupVmNicRefs().stream().filter(ref -> msg.getVmNicUuids().contains(ref.getVmNicUuid())).collect(Collectors.toList()));
        }

        final LoadBalancerActiveVmNicReply reply = new LoadBalancerActiveVmNicReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("active-vm-nics-on-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "set-nics-to-active-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (LoadBalancerServerGroupVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Active);
                            dbf.update(ref);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (LoadBalancerServerGroupVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Inactive);
                            dbf.update(ref);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "active-nics-on-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<VmNicVO> nicvos = Q.New(VmNicVO.class)
                                .in(VmNicVO_.uuid, transformAndRemoveNull(refs, LoadBalancerServerGroupVmNicRefVO::getVmNicUuid))
                                .list();

                        LoadBalancerBackend bkd = getBackend();
                        bkd.addVmNics(lbMgr.makeStruct(self), VmNicInventory.valueOf(nicvos), new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final LoadBalancerActiveVmNicMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                activeVmNic(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "deactive-nic";
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateLoadBalancerListenerMsg) {
            handle((APICreateLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddVmNicToLoadBalancerMsg) {
            handle((APIAddVmNicToLoadBalancerMsg) msg);
        } else if (msg instanceof APIRemoveVmNicFromLoadBalancerMsg) {
            handle((APIRemoveVmNicFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerListenerMsg) {
            handle((APIDeleteLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerMsg) {
            handle((APIDeleteLoadBalancerMsg) msg);
        } else if (msg instanceof APIRefreshLoadBalancerMsg) {
            handle((APIRefreshLoadBalancerMsg) msg);
        } else if (msg instanceof APIGetCandidateVmNicsForLoadBalancerMsg) {
            handle((APIGetCandidateVmNicsForLoadBalancerMsg) msg);
        } else if (msg instanceof APIGetCandidateL3NetworksForLoadBalancerMsg) {
            handle((APIGetCandidateL3NetworksForLoadBalancerMsg) msg);
        } else if (msg instanceof APIGetCandidateL3NetworksForServerGroupMsg) {
            handle((APIGetCandidateL3NetworksForServerGroupMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerMsg) {
            handle((APIUpdateLoadBalancerMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerListenerMsg) {
            handle((APIUpdateLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddCertificateToLoadBalancerListenerMsg) {
            handle((APIAddCertificateToLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIRemoveCertificateFromLoadBalancerListenerMsg) {
            handle((APIRemoveCertificateFromLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIRemoveAccessControlListFromLoadBalancerMsg) {
            handle((APIRemoveAccessControlListFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIChangeAccessControlListServerGroupMsg) {
            handle((APIChangeAccessControlListServerGroupMsg)msg);
        } else if (msg instanceof APIAddAccessControlListToLoadBalancerMsg) {
            handle((APIAddAccessControlListToLoadBalancerMsg) msg);
        } else if (msg instanceof APIChangeLoadBalancerListenerMsg) {
            handle((APIChangeLoadBalancerListenerMsg) msg);
        } else if (msg instanceof  APICreateLoadBalancerServerGroupMsg) {
            handle((APICreateLoadBalancerServerGroupMsg) msg);
        } else if (msg instanceof APIAddServerGroupToLoadBalancerListenerMsg){
            handle((APIAddServerGroupToLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddBackendServerToServerGroupMsg){
            handle((APIAddBackendServerToServerGroupMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerServerGroupMsg) {
            handle((APIUpdateLoadBalancerServerGroupMsg) msg);
        } else if (msg instanceof APIRemoveServerGroupFromLoadBalancerListenerMsg){
            handle((APIRemoveServerGroupFromLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIRemoveBackendServerFromServerGroupMsg) {
            handle((APIRemoveBackendServerFromServerGroupMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerServerGroupMsg) {
            handle((APIDeleteLoadBalancerServerGroupMsg) msg);
        } else if (msg instanceof APIGetCandidateVmNicsForLoadBalancerServerGroupMsg) {
            handle((APIGetCandidateVmNicsForLoadBalancerServerGroupMsg) msg);
        } else if(msg instanceof APIChangeLoadBalancerBackendServerMsg){
            handle((APIChangeLoadBalancerBackendServerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetCandidateVmNicsForLoadBalancerServerGroupMsg msg) {
        APIGetCandidateVmNicsForLoadBalancerServerGroupReply reply = new APIGetCandidateVmNicsForLoadBalancerServerGroupReply();
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(self.getType().toString());
        LoadBalancerServerGroupVO groupVO = null;
        if (msg.getServergroupUuid() != null) {
            groupVO = dbf.findByUuid(msg.getServergroupUuid(), LoadBalancerServerGroupVO.class);
        }
        List<VmNicVO> nicVOS = f.getAttachableVmNicsForServerGroup(self, groupVO);
        reply.setInventories(VmNicInventory.valueOf(nicVOS));
        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateL3NetworksForLoadBalancerMsg msg) {
        APIGetCandidateL3NetworksForLoadBalancerReply reply = new APIGetCandidateL3NetworksForLoadBalancerReply();
        LoadBalancerGetPeerL3NetworksMsg amsg = new LoadBalancerGetPeerL3NetworksMsg();
        amsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, LoadBalancerConstants.SERVICE_ID, msg.getLoadBalancerUuid());
        MessageReply messageReply = bus.call(amsg);
        if(messageReply.isSuccess()){
            reply.setInventories(msg.filter(((LoadBalancerGetPeerL3NetworksReply)messageReply).getInventories()));
        }else{
            reply.setError(messageReply.getError());
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateL3NetworksForServerGroupMsg msg) {
        APIGetCandidateL3NetworksForServerGroupReply reply = new APIGetCandidateL3NetworksForServerGroupReply();
        LoadBalancerGetPeerL3NetworksMsg amsg = new LoadBalancerGetPeerL3NetworksMsg();
        amsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, LoadBalancerConstants.SERVICE_ID, msg.getLoadBalancerUuid());
        MessageReply messageReply = bus.call(amsg);
        if(messageReply.isSuccess()){
            reply.setInventories(msg.filter(((LoadBalancerGetPeerL3NetworksReply)messageReply).getInventories()));
        }else{
            reply.setError(messageReply.getError());
        }

        bus.reply(msg, reply);
    }

    private void handle(final LoadBalancerGetPeerL3NetworksMsg msg) {
        LoadBalancerGetPeerL3NetworksReply reply = new LoadBalancerGetPeerL3NetworksReply();
        String sql = "select l3" +
                " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                " where l3.uuid = ref.l3NetworkUuid" +
                " and ref.networkServiceType = :type" ;
        TypedQuery<L3NetworkVO> q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
        q.setParameter("type",LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        List<L3NetworkVO> guestNetworks = q.getResultList();

        List<L3NetworkInventory> ret = L3NetworkInventory.valueOf(guestNetworks);
        if (ret != null && !ret.isEmpty()) {
            for (GetPeerL3NetworksForLoadBalancerExtensionPoint extp : pluginRgty.getExtensionList(GetPeerL3NetworksForLoadBalancerExtensionPoint.class)) {
                ret = extp.getPeerL3NetworksForLoadBalancer(self.getUuid(), ret);
            }
        }
        reply.setInventories(ret);
        bus.reply(msg, reply);
    }

    private void handle(RemoveAccessControlListFromLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                RemoveAccessControlListFromLoadBalancerReply reply = new RemoveAccessControlListFromLoadBalancerReply();
                List<LoadBalancerListenerACLRefVO> refs;

                if (msg.getServerGroupUuids() == null || msg.getServerGroupUuids().isEmpty()) {
                    refs = Q.New(LoadBalancerListenerACLRefVO.class).in(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuids())
                            .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).list();
                } else {
                    refs = Q.New(LoadBalancerListenerACLRefVO.class).in(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuids())
                            .in(LoadBalancerListenerACLRefVO_.serverGroupUuid, msg.getServerGroupUuids())
                            .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).list();
                }

                if (refs.isEmpty()) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                dbf.removeCollection(refs, LoadBalancerListenerACLRefVO.class);

                final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);

                boolean refresh = isListenerNeedRefresh(lblVo, msg.getServerGroupUuids());
                if (refresh) {
                    RefreshLoadBalancerMsg rmsg = new RefreshLoadBalancerMsg();
                    rmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(rmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(rmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                                reply.setError(reply.getError());
                                for (LoadBalancerListenerACLRefVO refVO : refs) {
                                    refVO.setId(null);
                                }
                                dbf.persistCollection(refs);
                            }
                            bus.reply(msg, reply);
                        }
                    });

                    chain.next();
                    return;
                }

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "remove-acl-lb-listener";
            }
        });
    }

    private void handle(APIGetCandidateVmNicsForLoadBalancerMsg msg) {
        APIGetCandidateVmNicsForLoadBalancerReply reply = new APIGetCandidateVmNicsForLoadBalancerReply();

        new SQLBatch(){
            @Override
            protected void scripts() {
                List<String> guestNetworks = sql("select l3.uuid" +
                        " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                        " where l3.uuid = ref.l3NetworkUuid" +
                        " and ref.networkServiceType = :type")
                        .param("type", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)
                        .list();

                List<VmNicVO> nics = new ArrayList<>();
                if (guestNetworks != null && !guestNetworks.isEmpty()) {
                    nics = sql("select nic" +
                            " from VmNicVO nic, VmInstanceVO vm" +
                            " where nic.l3NetworkUuid in (:guestNetworks)" +
                            " and nic.vmInstanceUuid = vm.uuid" +
                            " and vm.type = :vmType" +
                            " and vm.state in (:vmStates)")
                            .param("guestNetworks",guestNetworks)
                            .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                            .param("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped))
                            .list();
                }

                /* TODO: lb only support ipv4 */
                LoadBalancerVO lbVO = dbf.findByUuid(msg.getLoadBalancerUuid(), LoadBalancerVO.class);
                VipVO vipVO = dbf.findByUuid(lbVO.getVipUuid(), VipVO.class);
                List<VmNicInventory> nicInvs;
                if (IPv6NetworkUtils.isIpv6Address(vipVO.getIp())) {
                    nicInvs = new ArrayList<>();
                } else {
                    nicInvs = l3Mgr.filterVmNicByIpVersion(VmNicInventory.valueOf(nics), IPv6Constants.IPv4);
                }

                reply.setInventories(callGetCandidateVmNicsForLoadBalancerExtensionPoint(msg, nicInvs));
            }
        }.execute();

        bus.reply(msg, reply);
    }

    private List<VmNicInventory> callGetCandidateVmNicsForLoadBalancerExtensionPoint(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates) {
        if(candidates.isEmpty()){
            return candidates;
        }

        List<VmNicInventory> ret = candidates;
        for (GetCandidateVmNicsForLoadBalancerExtensionPoint extp : pluginRgty.getExtensionList(GetCandidateVmNicsForLoadBalancerExtensionPoint.class)) {
            ret = extp.getCandidateVmNicsForLoadBalancerInVirtualRouter(msg, ret);
        }
        return ret;
    }

    private void handle(final APIRefreshLoadBalancerMsg msg) {
        final APIRefreshLoadBalancerEvent evt = new APIRefreshLoadBalancerEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refresh(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory(getInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "refresh-lb";
            }
        });
    }

    private void handle(final APIDeleteLoadBalancerMsg msg) {
        final APIDeleteLoadBalancerEvent evt = new APIDeleteLoadBalancerEvent(msg.getId());

        final String issuer = LoadBalancerVO.class.getSimpleName();
        final List<LoadBalancerInventory> ctx = Arrays.asList(LoadBalancerInventory.valueOf(self));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-loadBalancer-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-loadBalancer-permissive-check";

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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-loadBalancer-permissive-delete";

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
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-loadBalancer-force-delete";

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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
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

    private void delete(final Completion completion) {
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(self.getType().toString());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (self.getProviderType() == null) {
                            trigger.next();
                            // not initialized yet
                            return;
                        }

                        LoadBalancerBackend bkd = getBackend();
                        bkd.destroyLoadBalancer(lbMgr.makeStruct(self), new Completion(trigger) {
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
                    String __name__ = "release-vip";
                    @Transactional(readOnly = true)
                    private List<String> getGuestNetworkUuids() {
                        List<String> vmNicUuids = new ArrayList<>();
                        List<LoadBalancerServerGroupVO> groupVOS = Q.New(LoadBalancerServerGroupVO.class)
                                .eq(LoadBalancerServerGroupVO_.loadBalancerUuid, self.getUuid()).list();
                        for (LoadBalancerServerGroupVO groupVO : groupVOS) {
                            vmNicUuids.addAll(groupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                                    .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList()));
                        }
                        if (vmNicUuids.isEmpty()) {
                            return new ArrayList<>();
                        }

                        List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid)
                                .in(VmNicVO_.uuid, vmNicUuids).listValues();
                        return l3Uuids.stream().distinct().collect(Collectors.toList());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(f.getNetworkServiceType());
                        struct.setServiceUuid(self.getUuid());
                        if (self.getProviderType() != null) {
                            /* release vip peer networks */
                            struct.setServiceProvider(self.getProviderType());
                            List<String> guestNetworkUuids = getGuestNetworkUuids();
                            if (!guestNetworkUuids.isEmpty()) {
                                struct.setPeerL3NetworkUuids(guestNetworkUuids);
                            }
                        }
                        Vip v = new Vip(self.getVipUuid());
                        v.setStruct(struct);
                        v.release(new Completion(trigger) {
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

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        new SQLBatch(){

                            @Override
                            protected void scripts() {
                                for(LoadBalancerListenerVO lbListener :self.getListeners()){
                                    /*there is no cascade deleting configure for acl ref in db */
                                    for(LoadBalancerListenerACLRefVO acl: lbListener.getAclRefs()) {
                                        sql(LoadBalancerListenerACLRefVO.class)
                                                .eq(LoadBalancerListenerACLRefVO_.id, acl.getId())
                                                .delete();
                                    }
                                    sql(LoadBalancerListenerVO.class)
                                            .eq(LoadBalancerListenerVO_.uuid,lbListener.getUuid())
                                            .delete();
                                }

                                List<String> serverGroupUuids = q(LoadBalancerServerGroupVO.class)
                                        .eq(LoadBalancerServerGroupVO_.loadBalancerUuid, self.getUuid())
                                        .select(LoadBalancerServerGroupVO_.uuid).listValues();
                                if (serverGroupUuids != null && !serverGroupUuids.isEmpty()) {
                                    sql(LoadBalancerServerGroupVmNicRefVO.class)
                                            .in(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, serverGroupUuids)
                                            .delete();
                                    sql(LoadBalancerServerGroupServerIpVO.class)
                                            .in(LoadBalancerServerGroupServerIpVO_.serverGroupUuid, serverGroupUuids)
                                            .delete();
                                    sql(LoadBalancerServerGroupVO.class)
                                            .in(LoadBalancerServerGroupVO_.uuid, serverGroupUuids)
                                            .delete();
                                }
                            }
                        }.execute();
                        f.deleteLoadBalancer(self);
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

    private void handle(final APIDeleteLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deleteListener(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-listener";
            }
        });
    }

    private LoadBalancerStruct removeListenerStruct(LoadBalancerListenerInventory listener) {
        LoadBalancerStruct s = lbMgr.makeStruct(self);
        s.getDeletedListenerServerGroupMap().put(listener.getUuid(), s.getListenerServerGroupMap().remove(listener.getUuid()));

        return s;
    }

    private void deleteListener(APIDeleteLoadBalancerListenerMsg msg, final NoErrorCompletion completion) {
        final APIDeleteLoadBalancerListenerEvent evt = new APIDeleteLoadBalancerListenerEvent(msg.getId());

        final LoadBalancerListenerVO vo = dbf.findByUuid(msg.getUuid(), LoadBalancerListenerVO.class);
        if (vo == null) {
            evt.setInventory(getInventory());
            bus.publish(evt);
            completion.done();
            return;
        }

        if (!needAction(vo)) {
            /*there is no cascade deleting configure for acl ref in db */
            if (!vo.getAclRefs().isEmpty()) {
                dbf.removeCollection(vo.getAclRefs(), LoadBalancerListenerACLRefVO.class);
            }
            /*http://jira.zstack.io/browse/ZSTAC-27065*/
            dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerListenerVO.class);
            if(vo.getServerGroupUuid() !=null && !vo.getServerGroupUuid().isEmpty()){
                dbf.removeByPrimaryKey(vo.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
            }
            evt.setInventory(reloadAndGetInventory());
            bus.publish(evt);
            completion.done();
            return;
        }

        LoadBalancerListenerInventory listener = LoadBalancerListenerInventory.valueOf(vo);
        LoadBalancerBackend bkd = getBackend();
        bkd.removeListener(removeListenerStruct(listener), listener, new Completion(msg, completion) {
            @Override
            public void success() {
                /*there is no cascade deleting configure for acl ref in db */
                if (!vo.getAclRefs().isEmpty()) {
                    dbf.removeCollection(vo.getAclRefs(), LoadBalancerListenerACLRefVO.class);
                }
                dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerListenerVO.class);
                if(vo.getServerGroupUuid() !=null && !vo.getServerGroupUuid().isEmpty()){
                    dbf.removeByPrimaryKey(vo.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                }
                evt.setInventory(reloadAndGetInventory());
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(final APIRemoveVmNicFromLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                removeNic(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "remove-nic";
            }
        });
    }

    /* this api can only be called by remove nic from listener */
    private LoadBalancerStruct removeNicStruct(List<String> serverGroupUuids, List<String> listenerUuids, List<String> nicUuids, List<String> serverIps) {
        LoadBalancerStruct s = lbMgr.makeStruct(self);
        for (LoadBalancerListenerInventory l : s.getListeners()) {
            if (listenerUuids != null) {
                if (listenerUuids.contains(l.getUuid())) {
                    /* detach server group from listener */
                    boolean remove = s.getListenerServerGroupMap().get(l.getUuid()).removeIf(g -> serverGroupUuids.contains(g.getUuid()));
                    if (remove) {
                        l.getAclRefs().removeIf(ref -> serverGroupUuids.contains(ref.getServerGroupUuid()));
                    }
                }
            } else {
                for (LoadBalancerServerGroupInventory groupInv : s.getListenerServerGroupMap().get(l.getUuid())) {
                    if (!serverGroupUuids.contains(groupInv.getUuid())) {
                        continue;
                    }

                    /* remove the vmnic to be deleted */
                    groupInv.getVmNicRefs().removeIf(nicRef -> nicUuids.contains(nicRef.getVmNicUuid()));
                    groupInv.getServerIps().removeIf(ipRef -> serverIps.contains(ipRef.getIpAddress()));
                }
            }
        }

        return s;
    }

    private void removeNics(List<String> serverGroupUuids, List<String> listenerUuids, final List<String> vmNicUuids, final List<String> serverIps, final Completion completion) {
        List<VmNicInventory> nics = new ArrayList<>();
        if (vmNicUuids != null && !vmNicUuids.isEmpty()) {
            SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
            q.add(VmNicVO_.uuid, Op.IN, vmNicUuids);
            List<VmNicVO> vos = q.list();
            nics = VmNicInventory.valueOf(vos);
        }

        LoadBalancerBackend bkd = getBackend();
        if ((vmNicUuids != null && vmNicUuids.isEmpty() && serverIps.isEmpty()) || bkd == null) {
            completion.success();
        } else {
            bkd.removeVmNics(removeNicStruct(serverGroupUuids, listenerUuids, vmNicUuids, serverIps), nics, completion);
        }
    }

    private void removeNic(APIRemoveVmNicFromLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final APIRemoveVmNicFromLoadBalancerEvent evt = new APIRemoveVmNicFromLoadBalancerEvent(msg.getId());
        LoadBalancerListenerVO listenerVO = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
        LoadBalancerServerGroupVO groupVO = lbMgr.getDefaultServerGroup(listenerVO);

        removeNics(Arrays.asList(groupVO.getUuid()), null, msg.getVmNicUuids(), new ArrayList<>(), new Completion(msg, completion) {
            @Override
            public void success() {
                SQL.New(LoadBalancerServerGroupVmNicRefVO.class)
                        .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, groupVO.getUuid())
                        .in(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid, msg.getVmNicUuids()).delete();
                SQL.New(LoadBalancerListenerVmNicRefVO.class)
                        .eq(LoadBalancerListenerVmNicRefVO_.listenerUuid, msg.getListenerUuid())
                        .in(LoadBalancerListenerVmNicRefVO_.vmNicUuid, msg.getVmNicUuids())
                        .delete();
                evt.setInventory(reloadAndGetInventory());
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(final APIAddVmNicToLoadBalancerMsg msg) {
        final APIAddVmNicToLoadBalancerEvent evt = new APIAddVmNicToLoadBalancerEvent(msg.getId());

        AddVmNicToLoadBalancerMsg addVmNicToLoadBalancerMsg = new AddVmNicToLoadBalancerMsg();
        addVmNicToLoadBalancerMsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        addVmNicToLoadBalancerMsg.setListenerUuid(msg.getListenerUuid());
        addVmNicToLoadBalancerMsg.setVmNicUuids(msg.getVmNicUuids());
        addVmNicToLoadBalancerMsg.setSystemTags(msg.getSystemTags());
        bus.makeLocalServiceId(addVmNicToLoadBalancerMsg, LoadBalancerConstants.SERVICE_ID);

        bus.send(addVmNicToLoadBalancerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class)));
                    bus.publish(evt);
                    return;
                }

                evt.setError(reply.getError());
                bus.publish(evt);
            }
        });
    }

    private void handle(final AddVmNicToLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                AddVmNicToLoadBalancerReply reply = new AddVmNicToLoadBalancerReply();

                LoadBalancerServerGroupVO groupVO = null;
                LoadBalancerListenerVO listenerVO = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
                if (listenerVO.getServerGroupUuid() == null) {
                    groupVO = new LoadBalancerServerGroupVO();
                    groupVO.setUuid(Platform.getUuid());
                    String accountUuid = Account.getAccountUuidOfResource(listenerVO.getUuid());
                    groupVO.setAccountUuid(accountUuid);
                    groupVO.setDescription(String.format("default server group for load balancer listener %s", listenerVO.getName()));
                    groupVO.setLoadBalancerUuid(listenerVO.getLoadBalancerUuid());
                    groupVO.setName(String.format("default-server-group-%s-%s", listenerVO.getName(), listenerVO.getUuid().substring(0, 5)));
                    dbf.persist(groupVO);

                    listenerVO.setServerGroupUuid(groupVO.getUuid());
                    listenerVO = dbf.updateAndRefresh(listenerVO);

                    LoadBalancerListenerServerGroupRefVO ref = new LoadBalancerListenerServerGroupRefVO();
                    ref.setListenerUuid(msg.getListenerUuid());
                    ref.setServerGroupUuid(groupVO.getUuid());
                    dbf.persist(ref);
                } else {
                    groupVO = dbf.findByUuid(listenerVO.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                }

                List<LoadBalancerServerGroupVmNicRefVO> refs = new ArrayList<>();
                Map<String, Long> weight = new LoadBalancerWeightOperator().getWeight(msg.getSystemTags());

                for (String nicUuid : msg.getVmNicUuids()) {
                    LoadBalancerServerGroupVmNicRefVO ref = new LoadBalancerServerGroupVmNicRefVO();
                    ref.setServerGroupUuid(groupVO.getUuid());
                    ref.setVmNicUuid(nicUuid);
                    ref.setStatus(LoadBalancerVmNicStatus.Active);
                    if (weight.get(nicUuid) != null) {
                        ref.setWeight(weight.get(nicUuid));
                    } else {
                        ref.setWeight(LoadBalancerConstants.BALANCER_WEIGHT_default);
                    }
                    refs.add(ref);
                }

                dbf.persistCollection(refs);

                AttachServerGroupToListenerStruct struct = new AttachServerGroupToListenerStruct();
                struct.setListenerUuid(msg.getListenerUuid());
                for (String nicUuid : msg.getVmNicUuids()) {
                    if (weight.get(nicUuid) != null) {
                        struct.getVmNicWeight().put(nicUuid, weight.get(nicUuid));
                    } else {
                        struct.getVmNicWeight().put(nicUuid, LoadBalancerConstants.BALANCER_WEIGHT_default);
                    }

                }

                LoadBalancerServerGroupVO finalVO = groupVO;
                addVmNicToListener(struct, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        SQL.New(LoadBalancerServerGroupVmNicRefVO.class)
                                .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, finalVO.getUuid())
                                .in(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid, msg.getVmNicUuids()).delete();
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

    private void addVmNicToListener(final AttachServerGroupToListenerStruct struct, final Completion completion) {
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(self.getType().toString());
        List<String> nicUuids = new ArrayList<>(struct.getVmNicWeight().keySet());
        final String providerType = f.getProviderTypeByVmNicUuid(nicUuids.isEmpty() ? null : nicUuids.get(0));
        if (providerType == null) {
            throw new OperationFailureException(operr("can not get service providerType for load balancer listener [uuid:%s]", struct.listenerUuid));
        }

        final List<VmNicInventory> nics = new ArrayList<>();
        if (!nicUuids.isEmpty()) {
            SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
            q.add(VmNicVO_.uuid, Op.IN, nicUuids);
            List<VmNicVO> nicVOs = q.list();
            nics.addAll(VmNicInventory.valueOf(nicVOs));
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-vm-nic-to-lb-listener-%s", struct.listenerUuid));
        chain.then(new ShareFlow() {
            boolean init = false;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "check-provider-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (self.getProviderType() == null) {
                            self.setProviderType(providerType);
                            self = dbf.updateAndRefresh(self);
                            init = true;
                        } else {
                            if (!providerType.equals(self.getProviderType())) {
                                throw new OperationFailureException(operr("service provider type mismatching. The load balancer[uuid:%s] is provided by the service provider[type:%s]," +
                                                " but new service provider is [type: %s]", self.getUuid(), self.getProviderType(),providerType));
                            }
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (init) {
                            self = dbf.reload(self);
                            self.setProviderType(null);
                            dbf.update(self);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "add-nic-to-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LoadBalancerBackend bkd = getBackend();
                        LoadBalancerStruct s = lbMgr.makeStruct(self);
                        s.setInit(init);
                        bkd.addVmNics(s, nics, new Completion(trigger) {
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

    private boolean needAction(LoadBalancerListenerVO listenerVO) {
        if (self.getProviderType() == null) {
            return false;
        }

        for (LoadBalancerListenerServerGroupRefVO ref : listenerVO.getServerGroupRefs()) {
            LoadBalancerServerGroupVO groupVO = dbf.findByUuid(ref.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
            for (LoadBalancerServerGroupVmNicRefVO nicRef : groupVO.getLoadBalancerServerGroupVmNicRefs()) {
                if (nicRef.getStatus() == LoadBalancerVmNicStatus.Active || nicRef.getStatus() == LoadBalancerVmNicStatus.Pending) {
                    return true;
                }
            }

            for (LoadBalancerServerGroupServerIpVO serverIp : groupVO.getLoadBalancerServerGroupServerIps()) {
                if (serverIp.getStatus() == LoadBalancerBackendServerStatus.Active) {
                    return true;
                }
            }
        }

        return false;
    }


    private LoadBalancerBackend getBackend() {
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(self.getType().toString());
        return f.getLoadBalancerBackend(self);
    }

    private void handle(final APICreateLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                if (Q.New(LoadBalancerListenerVO.class)
                        .eq(LoadBalancerListenerVO_.loadBalancerUuid, msg.getLoadBalancerUuid())
                        .eq(LoadBalancerListenerVO_.protocol, msg.getProtocol())
                        .eq(LoadBalancerListenerVO_.loadBalancerPort, msg.getLoadBalancerPort()).isExists()) {
                    APICreateLoadBalancerListenerEvent evt = new APICreateLoadBalancerListenerEvent(msg.getId());
                    evt.setError(argerr("there is listener with same port [%s] and same load balancer [uuid:%s]",
                            msg.getLoadBalancerPort(), msg.getLoadBalancerUuid()));
                    bus.publish(evt);

                    chain.next();
                    return;
                }

                createListener(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "create-listener";
            }
        });
    }

    private void createListener(final APICreateLoadBalancerListenerMsg msg, final NoErrorCompletion completion) {
        final APICreateLoadBalancerListenerEvent evt = new APICreateLoadBalancerListenerEvent(msg.getId());
        LoadBalancerListenerVO vo = new LoadBalancerListenerVO();
        vo.setLoadBalancerUuid(self.getUuid());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setInstancePort(msg.getInstancePort());
        vo.setLoadBalancerPort(msg.getLoadBalancerPort());
        vo.setProtocol(msg.getProtocol());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setSecurityPolicyType(msg.getSecurityPolicyType());
        vo = dbf.persistAndRefresh(vo);
        if (msg.getCertificateUuid() != null) {
            LoadBalancerListenerCertificateRefVO ref = new LoadBalancerListenerCertificateRefVO();
            ref.setListenerUuid(vo.getUuid());
            ref.setCertificateUuid(msg.getCertificateUuid());
            dbf.persist(ref);
        }

        if (msg.getAclUuids() != null) {
            final String listenerUuid = vo.getUuid();
            List<LoadBalancerListenerACLRefVO> refs = msg.getAclUuids().stream().map(aclUuid -> {
                LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                ref.setAclUuid(aclUuid);
                ref.setType(LoadBalancerAclType.valueOf(msg.getAclType()));
                ref.setListenerUuid(listenerUuid);
                return ref;
            }).collect(Collectors.toList());
            dbf.persistCollection(refs);
        }

        tagMgr.createNonInherentSystemTags(msg.getSystemTags(), vo.getUuid(), LoadBalancerListenerVO.class.getSimpleName());
        vo = dbf.updateAndRefresh(vo);
        evt.setInventory(LoadBalancerListenerInventory.valueOf(vo));
        bus.publish(evt);
        completion.done();
    }

    private void handle(APIUpdateLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateLoadBalancerEvent evt = new APIUpdateLoadBalancerEvent(msg.getId());

                boolean update = false;
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    update = true;
                }

                if (update) {
                    dbf.update(self);
                }

                evt.setInventory(getInventory());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-lb-info";
            }
        });
    }

    private void handle(APIUpdateLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateLoadBalancerListenerEvent evt = new APIUpdateLoadBalancerListenerEvent(msg.getId());
                LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getUuid(), LoadBalancerListenerVO.class);
                boolean update = false;
                if (msg.getName() != null) {
                    lblVo.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    lblVo.setDescription(msg.getDescription());
                    update = true;
                }
                if (update) {
                    dbf.update(lblVo);
                }

                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-lb-listener";
            }
        });
    }

    private void handle(APIChangeAccessControlListServerGroupMsg msg) {
        APIChangeAccessControlListServerGroupEvent evt = new APIChangeAccessControlListServerGroupEvent(msg.getId());

        List<String> currentSgUuids= Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuid())
                .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).notNull(LoadBalancerListenerACLRefVO_.serverGroupUuid).select(LoadBalancerListenerACLRefVO_.serverGroupUuid).listValues();

        APIChangeAccessControlListServerGroupEvent.LoadBalancerListerAcl loadBalancerListerAcl = new APIChangeAccessControlListServerGroupEvent.LoadBalancerListerAcl();
        loadBalancerListerAcl.setAclUuid(msg.getAclUuid());
        loadBalancerListerAcl.setServerGroupUuids(msg.getServerGroupUuids());
        loadBalancerListerAcl.setListenerUuid(msg.getListenerUuid());
        evt.setInventory(loadBalancerListerAcl);

        List<String> tmpDetachSgUuids = new ArrayList<>();
        List<String> tmpAttachSgUuids = new ArrayList<>();
        if (currentSgUuids.isEmpty()) {
            tmpAttachSgUuids.addAll(msg.getServerGroupUuids());
        } else {
            tmpDetachSgUuids = currentSgUuids.stream().filter(sgUuid -> !msg.getServerGroupUuids().contains(sgUuid)).collect(Collectors.toList());
            tmpAttachSgUuids = msg.getServerGroupUuids().stream().filter(sgUuid -> !currentSgUuids.contains(sgUuid)).collect(Collectors.toList());
        }
        if (tmpDetachSgUuids.isEmpty() && tmpAttachSgUuids.isEmpty()) {
            bus.publish(evt);
            return;
        }

        final List<String> detachSgUuids = tmpDetachSgUuids;
        final List<String> attachSgUuids = tmpAttachSgUuids;

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {

                boolean needRefresh = false;
                if (!detachSgUuids.isEmpty()) {
                    SQL.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuid()).in(LoadBalancerListenerACLRefVO_.serverGroupUuid, detachSgUuids).delete();
                    needRefresh = isServerGroupsOwnActiveBackenServer(detachSgUuids);
                }
                if (!attachSgUuids.isEmpty()) {
                    List<LoadBalancerListenerACLRefVO> refs = new ArrayList<>();
                    for (String sgUuid : attachSgUuids) {
                        LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                        ref.setAclUuid(msg.getAclUuid());
                        ref.setType(LoadBalancerAclType.redirect);
                        ref.setListenerUuid(msg.getListenerUuid());
                        ref.setServerGroupUuid(sgUuid);
                        refs.add(ref);
                    }
                    dbf.persistCollection(refs);
                    needRefresh = needRefresh | isServerGroupsOwnActiveBackenServer(attachSgUuids);
                }
                if (needRefresh) {
                    RefreshLoadBalancerMsg rmsg = new RefreshLoadBalancerMsg();
                    rmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(rmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(rmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                if (!detachSgUuids.isEmpty()) {
                                    List<LoadBalancerListenerACLRefVO> refs = new ArrayList<>();
                                    for (String sgUuid : detachSgUuids) {
                                        LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                                        ref.setAclUuid(msg.getAclUuid());
                                        ref.setType(LoadBalancerAclType.redirect);
                                        ref.setListenerUuid(msg.getListenerUuid());
                                        ref.setServerGroupUuid(sgUuid);
                                        refs.add(ref);
                                    }
                                    dbf.persistCollection(refs);
                                }
                                if (!attachSgUuids.isEmpty()) {
                                    SQL.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuid()).in(LoadBalancerListenerACLRefVO_.serverGroupUuid, attachSgUuids).delete();
                                }
                                logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                                evt.setError(reply.getError());
                            } else {
                                evt.getInventory().setServerGroupUuids(msg.getServerGroupUuids());
                            }
                            bus.publish(evt);
                        }
                    });
                    chain.next();
                    return;
                }
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "remove-acl-lb-listener";
            }
        });
    }

    private void handle(APIAddAccessControlListToLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                DebugUtils.Assert(msg.getAclType() != null && msg.getAclUuids() != null, "parameters cannot be null");
                APIAddAccessControlListToLoadBalancerEvent evt = new APIAddAccessControlListToLoadBalancerEvent(msg.getId());
                List<LoadBalancerListenerACLRefVO> refs = new ArrayList<>();
                final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
                if (msg.getServerGroupUuids() != null && !msg.getServerGroupUuids().isEmpty()) {
                    for (String aclUuid : msg.getAclUuids()) {
                        List<String> sgUuids = lblVo.getAclRefs().stream().filter(ref -> ref.getAclUuid().equals(aclUuid))
                                .map(LoadBalancerListenerACLRefVO::getServerGroupUuid).collect(Collectors.toList());
                        List<String> serverGroupUuids = msg.getServerGroupUuids().stream().filter(sgUuid -> !sgUuids.contains(sgUuid)).collect(Collectors.toList());
                        for (String sgUuid : serverGroupUuids) {
                            LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                            ref.setAclUuid(aclUuid);
                            ref.setType(LoadBalancerAclType.valueOf(msg.getAclType()));
                            ref.setListenerUuid(msg.getListenerUuid());
                            ref.setServerGroupUuid(sgUuid);
                            refs.add(ref);
                        }
                    }
                } else {
                    refs = msg.getAclUuids().stream().map(aclUuid -> {
                        LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                        ref.setAclUuid(aclUuid);
                        ref.setType(LoadBalancerAclType.valueOf(msg.getAclType()));
                        ref.setListenerUuid(msg.getListenerUuid());
                        return ref;
                    }).collect(Collectors.toList());
                }
                dbf.persistCollection(refs);

                final List<LoadBalancerListenerACLRefVO> refVOS = refs;
                boolean refresh = isListenerNeedRefresh(lblVo, msg.getServerGroupUuids());
                if (refresh) {
                    RefreshLoadBalancerMsg rmsg = new RefreshLoadBalancerMsg();
                    rmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(rmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(rmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                                evt.setError(reply.getError());
                                dbf.removeCollection(refVOS, LoadBalancerListenerACLRefVO.class);
                            } else {
                                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                            }
                            bus.publish(evt);
                        }
                    });
                    chain.next();
                    return;
                }

                evt.setInventory( LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "add-acl-lb-listener";
            }
        });
    }

    private void handle(APIRemoveAccessControlListFromLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIRemoveAccessControlListFromLoadBalancerEvent evt = new APIRemoveAccessControlListFromLoadBalancerEvent(msg.getId());
                List<LoadBalancerListenerACLRefVO> refs;

                if (msg.getServerGroupUuids() == null || msg.getServerGroupUuids().isEmpty()) {
                    refs = Q.New(LoadBalancerListenerACLRefVO.class).in(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuids())
                            .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).list();
                } else {
                    refs = Q.New(LoadBalancerListenerACLRefVO.class).in(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuids())
                            .in(LoadBalancerListenerACLRefVO_.serverGroupUuid, msg.getServerGroupUuids())
                            .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).list();
                }

                if (refs.isEmpty()) {
                    final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
                    evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                dbf.removeCollection(refs, LoadBalancerListenerACLRefVO.class);

                final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);

                boolean refresh = isListenerNeedRefresh(lblVo, msg.getServerGroupUuids());
                if (refresh) {
                    RefreshLoadBalancerMsg rmsg = new RefreshLoadBalancerMsg();
                    rmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(rmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(rmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                                evt.setError(reply.getError());
                                for (LoadBalancerListenerACLRefVO refVO : refs) {
                                    refVO.setId(null);
                                }
                                dbf.persistCollection(refs);
                            } else {
                                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                            }
                            bus.publish(evt);
                        }
                    });

                    chain.next();
                    return;
                }

                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "remove-acl-lb-listener";
            }
        });
    }

    private String[] getHeathCheckTarget(String ListenerUuid) {
        String target = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(ListenerUuid,
                LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);
        DebugUtils.Assert(target != null, String.format("the health target not exist, please check if the listener[%s] exist", ListenerUuid));

        String[] ts = target.split(":");
        if (ts.length != 2) {
            throw new OperationFailureException(argerr("invalid health target[%s], the format is targetCheckProtocol:port, for example, tcp:default", target));
        }
        return ts;
    }

    private boolean isListenerNeedRefresh(LoadBalancerListenerVO lblVo, List<String> serverGroupUuids) {
        List<String> sgUuids = new ArrayList<>();
        if (serverGroupUuids == null || serverGroupUuids.isEmpty()) {
            sgUuids = lblVo.getServerGroupRefs().stream().map(LoadBalancerListenerServerGroupRefVO::getServerGroupUuid).collect(Collectors.toList());
        } else {
            sgUuids = serverGroupUuids;
        }

        for (String serverGroupUuid : sgUuids) {
            LoadBalancerServerGroupVO groupVO = dbf.findByUuid(serverGroupUuid, LoadBalancerServerGroupVO.class);
            if (groupVO.getLoadBalancerServerGroupVmNicRefs().stream().anyMatch(r -> r.getStatus() == LoadBalancerVmNicStatus.Active)) {
                return true;
            }

            if (groupVO.getLoadBalancerServerGroupServerIps().stream().anyMatch(r -> r.getStatus() == LoadBalancerBackendServerStatus.Active)) {
                return true;
            }
        }
        return false;
    }

    private boolean isServerGroupsOwnActiveBackenServer(List<String> serverGroupUuids) {
        if (serverGroupUuids == null || serverGroupUuids.isEmpty()) {
            return false;
        }
        List<LoadBalancerServerGroupVO> groupVOs = Q.New(LoadBalancerServerGroupVO.class).in(LoadBalancerServerGroupVO_.uuid, serverGroupUuids).list();
        for (LoadBalancerServerGroupVO sg : groupVOs) {
            if (sg.getLoadBalancerServerGroupVmNicRefs().stream().anyMatch(r -> r.getStatus() == LoadBalancerVmNicStatus.Active)) {
                return true;
            }

            if (sg.getLoadBalancerServerGroupServerIps().stream().anyMatch(r -> r.getStatus() == LoadBalancerBackendServerStatus.Active)) {
                return true;
            }
        }
        return false;
    }

    private void handle(APIChangeLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIChangeLoadBalancerListenerEvent evt = new APIChangeLoadBalancerListenerEvent(msg.getId());
                LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getUuid(), LoadBalancerListenerVO.class);

                if (msg.getBalancerAlgorithm() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.BALANCER_ALGORITHM, msg.getUuid(), LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN, msg.getBalancerAlgorithm());
                }

                if (msg.getSessionPersistence() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.SESSION_PERSISTENCE, msg.getUuid(), LoadBalancerSystemTags.SESSION_PERSISTENCE_TOKEN, msg.getSessionPersistence());
                }

                if (msg.getSessionIdleTimeout() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.SESSION_IDLE_TIMEOUT, msg.getUuid(), LoadBalancerSystemTags.SESSION_IDLE_TIMEOUT_TOKEN, msg.getSessionIdleTimeout());
                }

                if (msg.getCookieName() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.COOKIE_NAME, msg.getUuid(), LoadBalancerSystemTags.COOKIE_NAME_TOKEN, msg.getCookieName());
                }

                if (msg.getHttpRedirectHttps() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.HTTP_REDIRECT_HTTPS, msg.getUuid(), LoadBalancerSystemTags.HTTP_REDIRECT_HTTPS_TOKEN, msg.getHttpRedirectHttps());
                }

                if (msg.getRedirectPort() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.REDIRECT_PORT, msg.getUuid(), LoadBalancerSystemTags.REDIRECT_PORT_TOKEN, msg.getRedirectPort());
                }

                if (msg.getStatusCode() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.STATUS_CODE, msg.getUuid(), LoadBalancerSystemTags.STATUS_CODE_TOKEN, msg.getStatusCode());
                }

                if (msg.getConnectionIdleTimeout() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT, msg.getUuid(), LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN, msg.getConnectionIdleTimeout());
                }

                if (msg.getHealthCheckInterval() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.HEALTH_INTERVAL, msg.getUuid(), LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN, msg.getHealthCheckInterval());
                }

                if (msg.getNbprocess() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.NUMBER_OF_PROCESS, msg.getUuid(), LoadBalancerSystemTags.NUMBER_OF_PROCESS_TOKEN, msg.getNbprocess());
                }

                if (msg.getHttpMode() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.HTTP_MODE, msg.getUuid(), LoadBalancerSystemTags.HTTP_MODE_TOKEN, msg.getHttpMode());
                }

                if (msg.getHealthCheckTarget() != null) {
                    String[] ts = getHeathCheckTarget(msg.getLoadBalancerListenerUuid());
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.HEALTH_TARGET, msg.getUuid(), LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, String.format("%s:%s", ts[0], msg.getHealthCheckTarget()));
                }

                if (msg.getHealthyThreshold() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.HEALTHY_THRESHOLD, msg.getUuid(), LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN, msg.getHealthyThreshold());
                }

                if (msg.getUnhealthyThreshold() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD, msg.getUuid(), LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN, msg.getUnhealthyThreshold());
                }

                if (msg.getMaxConnection() != null) {
                    updateLoadBalancerListenerSystemTag(LoadBalancerSystemTags.MAX_CONNECTION, msg.getUuid(), LoadBalancerSystemTags.MAX_CONNECTION_TOKEN, msg.getMaxConnection());
                }

                String[] ts = getHeathCheckTarget(msg.getUuid());
                if (msg.getHealthCheckProtocol() != null && !msg.getHealthCheckProtocol().equals(ts[0])) {
                    if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP.equals(ts[0]) &&
                            LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(msg.getHealthCheckProtocol())) {
                        DebugUtils.Assert(msg.getHealthCheckMethod() != null && msg.getHealthCheckURI() != null,
                                "the http health check protocol must be specified its healthy checking parameters including healthCheckMethod and healthCheckURI");
                        String code = LoadBalancerConstants.HealthCheckStatusCode.http_2xx.toString();
                        if (msg.getHealthCheckHttpCode() != null) {
                            code = msg.getHealthCheckHttpCode();
                        }
                        SystemTagCreator creator = LoadBalancerSystemTags.HEALTH_PARAMETER.newSystemTagCreator(msg.getUuid());
                        creator.setTagByTokens(map(e(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN,
                                String.format("%s:%s:%s", msg.getHealthCheckMethod(), msg.getHealthCheckURI(), code))
                            ));
                        creator.create();
                    }

                    if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(ts[0]) &&
                            LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP.equals(msg.getHealthCheckProtocol())) {
                        LoadBalancerSystemTags.HEALTH_PARAMETER.delete(msg.getUuid());
                    }

                    LoadBalancerSystemTags.HEALTH_TARGET.update(msg.getUuid(),
                                LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(map(
                                        e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, String.format("%s:%s", msg.getHealthCheckProtocol(), ts[1])))
                                ));
                    ts = getHeathCheckTarget(msg.getUuid());
                }

                if (msg.getHealthCheckHttpCode() != null || msg.getHealthCheckMethod() != null || msg.getHealthCheckURI() != null) {
                    if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(ts[0])) {
                        String param = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokenByResourceUuid(msg.getLoadBalancerListenerUuid(),
                                LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN);
                        String[] pm = param.split(":");
                        if (pm.length != 3) {
                            throw new OperationFailureException(argerr("invalid health checking parameters[%s], the format is method:URI:code, for example, GET:/index.html:http_2xx", param));
                        }

                        if (msg.getHealthCheckMethod() != null) {
                            pm[0] = msg.getHealthCheckMethod();
                        }
                        if (msg.getHealthCheckURI() != null) {
                            pm[1] = msg.getHealthCheckURI();
                        }
                        if (msg.getHealthCheckHttpCode() != null) {
                             pm[2] = msg.getHealthCheckHttpCode();
                        }
                        LoadBalancerSystemTags.HEALTH_PARAMETER.update(msg.getUuid(),
                                LoadBalancerSystemTags.HEALTH_PARAMETER.instantiateTag(
                                        map(e(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN, String.format("%s:%s:%s", pm[0], pm[1], pm[2]))
                                        )));
                    }
                }

                if (msg.getAclStatus() != null) {
                    if (LoadBalancerSystemTags.BALANCER_ACL.hasTag(msg.getUuid())) {
                        LoadBalancerSystemTags.BALANCER_ACL.update(msg.getUuid(),
                                LoadBalancerSystemTags.BALANCER_ACL.instantiateTag(map(
                                        e(LoadBalancerSystemTags.BALANCER_ACL_TOKEN, msg.getAclStatus())
                                )));
                    } else {
                        SystemTagCreator creator = LoadBalancerSystemTags.BALANCER_ACL.newSystemTagCreator(msg.getUuid());
                        creator.setTagByTokens(map(
                                e(LoadBalancerSystemTags.BALANCER_ACL_TOKEN, msg.getAclStatus())
                        ));
                        creator.inherent = false;
                        creator.create();
                    }
                }

                if (msg.getSystemTags() != null) {
                    new LoadBalancerWeightOperator().setWeight(msg.getSystemTags(), msg.getLoadBalancerListenerUuid());
                }

                if (msg.getSecurityPolicyType() != null) {
                    lblVo.setSecurityPolicyType(msg.getSecurityPolicyType());
                    dbf.updateAndRefresh(lblVo);
                }

                boolean refresh = isListenerNeedRefresh(lblVo, null);
                if (refresh) {
                    RefreshLoadBalancerMsg msg = new RefreshLoadBalancerMsg();
                    msg.setUuid(lblVo.getLoadBalancerUuid());
                    bus.makeLocalServiceId(msg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(msg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format( "update listener [uuid:%s] failed", lblVo.getUuid()));
                                evt.setError(reply.getError());
                            } else {
                                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                            }
                            bus.publish(evt);
                        }
                    });

                    chain.next();
                    return;
                }
                evt.setInventory( LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "change-lb-listener";
            }
        });
    }

    private <K, V> void updateLoadBalancerListenerSystemTag(PatternedSystemTag systemTag, String resourceUuid, K token, V value) {
        SystemTagInventory systemTagInventory = systemTag.update(resourceUuid, systemTag.instantiateTag(map(e(token, value))));

        if (systemTagInventory == null) {
            SystemTagCreator creator = systemTag.newSystemTagCreator(resourceUuid);
            creator.setTagByTokens(map(e(token, value)));
            creator.create();
        }
    }

    private void handle(APIAddCertificateToLoadBalancerListenerMsg msg) {
        APIAddCertificateToLoadBalancerListenerEvent evt = new APIAddCertificateToLoadBalancerListenerEvent(msg.getId());

        LoadBalancerListenerCertificateRefVO ref = Q.New(LoadBalancerListenerCertificateRefVO.class)
                .eq(LoadBalancerListenerCertificateRefVO_.listenerUuid, msg.getListenerUuid())
                .eq(LoadBalancerListenerCertificateRefVO_.certificateUuid, msg.getCertificateUuid()).limit(1).find();
        if (ref != null) {
            LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
            evt.setInventory(inv);
            bus.publish(evt);
            return;
        } else {
            ref = new LoadBalancerListenerCertificateRefVO();
            ref.setListenerUuid(msg.getListenerUuid());
            ref.setCertificateUuid(msg.getCertificateUuid());
            dbf.persist(ref);
        }

        final LoadBalancerListenerCertificateRefVO original_ref = ref;
        LoadBalancerChangeCertificateMsg cmsg = new LoadBalancerChangeCertificateMsg();
        cmsg.setListenerUuid(msg.getListenerUuid());
        cmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        cmsg.setCertificateUuid(msg.getCertificateUuid());
        bus.makeLocalServiceId(cmsg, LoadBalancerConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
                    evt.setInventory(inv);
                } else {
                    dbf.remove(original_ref);
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIRemoveCertificateFromLoadBalancerListenerMsg msg) {
        APIRemoveCertificateFromLoadBalancerListenerEvent evt = new APIRemoveCertificateFromLoadBalancerListenerEvent(msg.getId());

        LoadBalancerListenerCertificateRefVO ref = Q.New(LoadBalancerListenerCertificateRefVO.class)
                .eq(LoadBalancerListenerCertificateRefVO_.listenerUuid, msg.getListenerUuid())
                .eq(LoadBalancerListenerCertificateRefVO_.certificateUuid, msg.getCertificateUuid()).limit(1).find();
        final LoadBalancerListenerCertificateRefVO original_ref = ref;
        if (ref == null) {
            LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
            evt.setInventory(inv);
            bus.publish(evt);
            return;
        } else {
            dbf.remove(ref);
        }

        LoadBalancerChangeCertificateMsg cmsg = new LoadBalancerChangeCertificateMsg();
        cmsg.setListenerUuid(msg.getListenerUuid());
        cmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        cmsg.setCertificateUuid(msg.getCertificateUuid());
        bus.makeLocalServiceId(cmsg, LoadBalancerConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
                    evt.setInventory(inv);
                } else {
                    dbf.persist(original_ref);
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(LoadBalancerChangeCertificateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                LoadBalancerChangeCertificateReply reply = new LoadBalancerChangeCertificateReply();

                /* there is no vm nic, can not installed to backend
                 * it will be installed to backend when binding vm nic */
                if (self.getProviderType() == null) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                LoadBalancerBackend bkd = getBackend();
                LoadBalancerStruct s = lbMgr.makeStruct(self);
                s.setInit(false);
                bkd.refresh(s, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });

                chain.next();
            }

            @Override
            public String getName() {
                return "change-lb-listener-certificate";
            }
        });
    }

    private void handle(final APICreateLoadBalancerServerGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createLoadBalanacerServerGroup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "create loadbalancer servergroup";
            }
        });

    }

    private void createLoadBalanacerServerGroup(final APICreateLoadBalancerServerGroupMsg msg, final NoErrorCompletion completion){
        final APICreateLoadBalancerServerGroupEvent evt = new APICreateLoadBalancerServerGroupEvent(msg.getId());
        LoadBalancerServerGroupVO vo = new LoadBalancerServerGroupVO();
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo = dbf.persistAndRefresh(vo);

        evt.setInventory(LoadBalancerServerGroupInventory.valueOf(vo));
        bus.publish(evt);
        completion.done();
    }

    private static class AttachServerGroupToListenerStruct {
        String listenerUuid;
        Map<String, Long> vmNicWeight = new HashMap<>();
        Map<String, Long> serverIpWeight = new HashMap<>();

        public AttachServerGroupToListenerStruct() {
        }

        public String getListenerUuid() {
            return listenerUuid;
        }

        public void setListenerUuid(String listenerUuid) {
            this.listenerUuid = listenerUuid;
        }

        public Map<String, Long> getVmNicWeight() {
            return vmNicWeight;
        }

        public void setVmNicWeight(Map<String, Long> vmNicWeight) {
            this.vmNicWeight = vmNicWeight;
        }

        public Map<String, Long> getServerIpWeight() {
            return serverIpWeight;
        }

        public void setServerIpWeight(Map<String, Long> serverIpWeight) {
            this.serverIpWeight = serverIpWeight;
        }
    }

    private void handle(final APIAddServerGroupToLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIAddServerGroupToLoadBalancerListenerEvent event = new APIAddServerGroupToLoadBalancerListenerEvent(msg.getId());

                LoadBalancerListenerServerGroupRefVO serverGroupRefVO = new LoadBalancerListenerServerGroupRefVO();
                serverGroupRefVO.setListenerUuid(msg.getlistenerUuid());
                serverGroupRefVO.setServerGroupUuid(msg.getServerGroupUuid());
                dbf.persist(serverGroupRefVO);

                LoadBalancerServerGroupVO groupVO = dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                if (groupVO.getLoadBalancerServerGroupServerIps().isEmpty() && groupVO.getLoadBalancerServerGroupVmNicRefs().isEmpty()) {
                    event.setInventory(LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getlistenerUuid(), LoadBalancerListenerVO.class)));
                    bus.publish(event);
                    chain.next();
                    return;
                }

                AttachServerGroupToListenerStruct struct = new AttachServerGroupToListenerStruct();
                struct.setListenerUuid(msg.getlistenerUuid());
                for (LoadBalancerServerGroupServerIpVO serverIpVO : groupVO.getLoadBalancerServerGroupServerIps()) {
                    struct.getServerIpWeight().put(serverIpVO.getIpAddress(), serverIpVO.getWeight());
                }
                for (LoadBalancerServerGroupVmNicRefVO nicRefVO : groupVO.getLoadBalancerServerGroupVmNicRefs()) {
                    struct.getVmNicWeight().put(nicRefVO.getVmNicUuid(), nicRefVO.getWeight());
                }

                addVmNicToListener(struct,  new Completion(chain) {
                    @Override
                    public void success() {
                        event.setInventory(LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getlistenerUuid(), LoadBalancerListenerVO.class)));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        SQL.New(LoadBalancerListenerServerGroupRefVO.class)
                                .eq(LoadBalancerListenerServerGroupRefVO_.listenerUuid, msg.getlistenerUuid())
                                .eq(LoadBalancerListenerServerGroupRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                .delete();
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "add-servergroup-to-loadbalancerlistener";
            }
        });
    }

    private void handle(final APIAddBackendServerToServerGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                addBackendServerToServergroup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "add-backendServer-to-servergroup";
            }
        });
    }

    private void addBackendServerToServergroup(final APIAddBackendServerToServerGroupMsg msg, final NoErrorCompletion completion){
        final APIAddBackendServerToServerGroupEvent event = new APIAddBackendServerToServerGroupEvent(msg.getId());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        LoadBalancerServerGroupVO groupVO = dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
//        Map<String, Long> weight = new LoadBalancerWeightOperator().getWeight(msg.getSystemTags());

        chain.setName("add-backendserver-to-servergroup");
        chain.then(new ShareFlow() {
            List<Map<String,String>> vmNics = msg.getVmNics();
            List<String> vmNicUuids = new ArrayList<>();
            List<Map<String,String>> servers = msg.getServers();
            List <String> serverIps = new ArrayList<>();

            List<LoadBalancerServerGroupVmNicRefVO> refVOs = new ArrayList<>();
            List<LoadBalancerServerGroupServerIpVO> serverIpVOs = new ArrayList();
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (vmNics != null) {
                            for (Map<String, String> vmNic : vmNics) {
                                String vmNicUuid = vmNic.get("uuid");
                                vmNicUuids.add(vmNicUuid);
                                LoadBalancerServerGroupVmNicRefVO refVO = new LoadBalancerServerGroupVmNicRefVO();
                                refVO.setServerGroupUuid(msg.getServerGroupUuid());
                                refVO.setVmNicUuid(vmNicUuid);
                                if (vmNic.containsKey("weight")) {
                                    Long vmNicWeight = Long.valueOf(vmNic.get("weight"));
                                    refVO.setWeight(vmNicWeight);
                                } else {
                                    refVO.setWeight(LoadBalancerConstants.BALANCER_WEIGHT_default);
                                }
                                refVO.setStatus(LoadBalancerVmNicStatus.Active);
                                refVOs.add(refVO);
                            }
                            dbf.persistCollection(refVOs);
                        }

                        if (servers != null) {
                            for (Map<String,String> server: servers) {
                                String ipAddr = server.get("ipAddress");
                                serverIps.add(ipAddr);
                                LoadBalancerServerGroupServerIpVO serverIpVO = new LoadBalancerServerGroupServerIpVO();
                                serverIpVO.setIpAddress(ipAddr);
                                serverIpVO.setServerGroupUuid(msg.getServerGroupUuid());
                                if(server.containsKey("weight")){
                                    Long ipWeight = Long.valueOf(server.get("weight"));
                                    serverIpVO.setWeight(ipWeight);
                                }else {
                                    serverIpVO.setWeight(LoadBalancerConstants.BALANCER_WEIGHT_default);
                                }
                                serverIpVO.setStatus(LoadBalancerBackendServerStatus.Active);
                                serverIpVOs.add(serverIpVO);
                            }
                            dbf.persistCollection(serverIpVOs);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if ( !vmNicUuids.isEmpty() ) {
                            SQL.New(LoadBalancerServerGroupVmNicRefVO.class)
                                    .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                    .in(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid,vmNicUuids).delete();
                        }

                        if ( !serverIps.isEmpty()) {
                            SQL.New(LoadBalancerServerGroupServerIpVO.class)
                                    .eq(LoadBalancerServerGroupServerIpVO_.serverGroupUuid, msg.getServerGroupUuid())
                                    .in(LoadBalancerServerGroupServerIpVO_.ipAddress,serverIps).delete();
                        }

                        trigger.rollback();
                    }

                });

                flow(new NoRollbackFlow() {
                    String __name__ = "add-to-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (groupVO.getLoadBalancerListenerServerGroupRefs().isEmpty()) {
                            trigger.next();
                            return;
                        }

                        LoadBalancerServerGroupInventory groupInv = LoadBalancerServerGroupInventory.valueOf(groupVO);
                        AttachServerGroupToListenerStruct struct = new AttachServerGroupToListenerStruct();
                        struct.setListenerUuid(groupInv.getListenerServerGroupRefs().get(0).getListenerUuid());

                       if(vmNics!=null){
                           for (Map<String, String> vmNic : vmNics) {
                               if(vmNic.containsKey("weight")){
                                   struct.getVmNicWeight().put(vmNic.get("uuid"), Long.valueOf(vmNic.get("weight")));
                               }else{
                                   struct.getVmNicWeight().put(vmNic.get("uuid"), LoadBalancerConstants.BALANCER_WEIGHT_default);
                               }
                           }
                       }

                        if(servers!=null){
                            for (Map<String,String> server: servers) {
                                if(server.containsKey("weight")){
                                    struct.getServerIpWeight().put(server.get("ipAddress"), Long.valueOf(server.get("weight")));
                                }else{
                                    struct.getServerIpWeight().put(server.get("ipAddress"), LoadBalancerConstants.BALANCER_WEIGHT_default);
                                }
                            }
                        }


                        /* addVmNicToListener will reresh all the listener, no need to call it for all listener */
                        addVmNicToListener(struct, new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        LoadBalancerServerGroupVO vo = dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                        event.setInventory(LoadBalancerServerGroupInventory.valueOf(vo));
                        completion.done();
                        bus.publish(event);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        event.setError(errCode);
                        completion.done();
                        bus.publish(event);
                    }
                });
            }
        }).start();
    }

    private void handle(APIUpdateLoadBalancerServerGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateLoadBalancerServerGroupEvent evt = new APIUpdateLoadBalancerServerGroupEvent(msg.getId());
                LoadBalancerServerGroupVO serverGroupVO = dbf.findByUuid(msg.getUuid(), LoadBalancerServerGroupVO.class);

                boolean update = false;
                if (msg.getName() != null && !msg.getName().equals(serverGroupVO.getName())) {
                    serverGroupVO.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null && !msg.getDescription().equals(serverGroupVO.getDescription())) {
                    serverGroupVO.setDescription(msg.getDescription());
                    update = true;
                }

                if (update) {
                    dbf.update(serverGroupVO);
                }

                evt.setInventory(LoadBalancerServerGroupInventory.valueOf(serverGroupVO));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-server-group";
            }
        });
    }

    private void handle(APIRemoveServerGroupFromLoadBalancerListenerMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIRemoveServerGroupFromLoadBalancerListenerEvent event = new APIRemoveServerGroupFromLoadBalancerListenerEvent(msg.getId());
                LoadBalancerServerGroupVO serverGroupVO = dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                List<String> nicUuids = serverGroupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                        .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList());
                List<String> serverIps = serverGroupVO.getLoadBalancerServerGroupServerIps().stream()
                        .map(LoadBalancerServerGroupServerIpVO::getIpAddress).collect(Collectors.toList());

                removeNics(asList(msg.getServerGroupUuid()), asList(msg.getListenerUuid()), nicUuids, serverIps, new Completion(chain) {
                    @Override
                    public void success() {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                SQL.New(LoadBalancerListenerVO.class)
                                        .eq(LoadBalancerListenerVO_.uuid, msg.getListenerUuid())
                                        .eq(LoadBalancerListenerVO_.serverGroupUuid, msg.getServerGroupUuid())
                                        .set(LoadBalancerListenerVO_.serverGroupUuid, null)
                                        .update();
                                SQL.New(LoadBalancerListenerACLRefVO.class)
                                        .eq(LoadBalancerListenerACLRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                        .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).delete();
                                SQL.New(LoadBalancerListenerServerGroupRefVO.class)
                                        .eq(LoadBalancerListenerServerGroupRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                        .eq(LoadBalancerListenerServerGroupRefVO_.listenerUuid, msg.getListenerUuid()).delete();
                            }
                        }.execute();
                        event.setInventory(LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class)));
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
            public String getName() {
                return "remove-servergroup-from-listener";
            }
        });
    }

    private void handle(APIRemoveBackendServerFromServerGroupMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIRemoveBackendServerFromServerGroupEvent event = new APIRemoveBackendServerFromServerGroupEvent(msg.getId());
                LoadBalancerServerGroupVO groupVO = dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                List<String> listerUuids = groupVO.getLoadBalancerListenerServerGroupRefs().stream()
                        .map(LoadBalancerListenerServerGroupRefVO::getListenerUuid).collect(Collectors.toList());
                if (listerUuids.isEmpty()) {
                    if (!msg.getVmNicUuids().isEmpty()) {
                        SQL.New(LoadBalancerServerGroupVmNicRefVO.class)
                                .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                .in(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid, msg.getVmNicUuids()).delete();
                    }

                    if (!msg.getServerIps().isEmpty()) {
                        SQL.New(LoadBalancerServerGroupServerIpVO.class)
                                .eq(LoadBalancerServerGroupServerIpVO_.serverGroupUuid, msg.getServerGroupUuid())
                                .in(LoadBalancerServerGroupServerIpVO_.ipAddress, msg.getServerIps()).delete();
                    }
                    event.setInventory(LoadBalancerServerGroupInventory.valueOf(dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class)));
                    bus.publish(event);
                    chain.next();
                    return;
                }

                removeNics(asList(msg.getServerGroupUuid()), null, msg.getVmNicUuids(), msg.getServerIps(), new Completion(chain) {
                    @Override
                    public void success() {
                        if (msg.getVmNicUuids() != null && !msg.getVmNicUuids().isEmpty()) {
                            SQL.New(LoadBalancerServerGroupVmNicRefVO.class)
                                    .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                    .in(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid, msg.getVmNicUuids()).delete();
                        }

                        if (msg.getServerIps() != null && !msg.getServerIps().isEmpty()) {
                            SQL.New(LoadBalancerServerGroupServerIpVO.class)
                                    .eq(LoadBalancerServerGroupServerIpVO_.serverGroupUuid, msg.getServerGroupUuid())
                                    .in(LoadBalancerServerGroupServerIpVO_.ipAddress, msg.getServerIps()).delete();
                        }
                        event.setInventory(LoadBalancerServerGroupInventory.valueOf(dbf.findByUuid(msg.getServerGroupUuid(), LoadBalancerServerGroupVO.class)));
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
            public String getName() {
                return "remove-backendserver-from-servergroup";
            }
        });
    }

    private void handle(final APIDeleteLoadBalancerServerGroupMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIDeleteLoadBalancerServerGroupEvent event = new APIDeleteLoadBalancerServerGroupEvent(msg.getId());
                LoadBalancerServerGroupVO groupVO = dbf.findByUuid(msg.getUuid(), LoadBalancerServerGroupVO.class);
                List<String> listerUuids = groupVO.getLoadBalancerListenerServerGroupRefs().stream()
                        .map(LoadBalancerListenerServerGroupRefVO::getListenerUuid).collect(Collectors.toList());
                if (listerUuids.isEmpty()) {
                    dbf.removeByPrimaryKey(msg.getUuid(), LoadBalancerServerGroupVO.class);
                    bus.publish(event);
                    chain.next();
                    return;
                }

                List<String> vmNicUuids = groupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                        .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList());
                List<String> serverIps = groupVO.getLoadBalancerServerGroupServerIps().stream()
                        .map(LoadBalancerServerGroupServerIpVO::getIpAddress).collect(Collectors.toList());
                removeNics(asList(groupVO.getUuid()), null, vmNicUuids, serverIps, new Completion(chain) {
                    @Override
                    public void success() {
                        dbf.removeByPrimaryKey(msg.getUuid(), LoadBalancerServerGroupVO.class);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        dbf.removeByPrimaryKey(msg.getUuid(), LoadBalancerServerGroupVO.class);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-servergroup";
            }
        });
    }

    private void handle(APIChangeLoadBalancerBackendServerMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIChangeLoadBalancerBackendServerEvent evt = new APIChangeLoadBalancerBackendServerEvent(msg.getId());
                boolean canRefresh = false;
                List<Map<String,String>> vmNics = msg.getVmNics();
                List<Map<String,String>> servers = msg.getServers();


                if (vmNics != null) {
                    for (Map<String, String> vmNic : vmNics) {
                        String vmNicUuid = vmNic.get("uuid");
                        LoadBalancerServerGroupVmNicRefVO vmNicRefVO = Q.New(LoadBalancerServerGroupVmNicRefVO.class)
                                .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid, msg.getServerGroupUuid())
                                .eq(LoadBalancerServerGroupVmNicRefVO_.vmNicUuid, vmNicUuid)
                                .find();
                        if (vmNic.containsKey("weight")) {
                            Long vmNicWeight = Long.valueOf(vmNic.get("weight"));
                            if (!vmNicWeight.equals(vmNicRefVO.getWeight())) {
                                vmNicRefVO.setWeight(vmNicWeight);
                                dbf.update(vmNicRefVO);
                                canRefresh = true;
                            }
                        }
                    }
                }

                if (servers != null) {
                    for (Map<String, String> server : servers) {
                        String ipAddress = server.get("ipAddress");
                        LoadBalancerServerGroupServerIpVO serverIpVO = Q.New(LoadBalancerServerGroupServerIpVO.class)
                                .eq(LoadBalancerServerGroupServerIpVO_.serverGroupUuid,msg.getServerGroupUuid())
                                .eq(LoadBalancerServerGroupServerIpVO_.ipAddress, ipAddress)
                                .find();
                        if (server.containsKey("weight")) {
                            Long serverIpWeight = Long.valueOf(server.get("weight"));
                            if(!serverIpWeight.equals(serverIpVO.getWeight())){
                                serverIpVO.setWeight(serverIpWeight);
                                dbf.update(serverIpVO);
                                canRefresh = true;
                            }
                        }
                    }
                }

                LoadBalancerServerGroupVO serverGroupVO = Q.New(LoadBalancerServerGroupVO.class)
                        .eq(LoadBalancerServerGroupVO_.uuid,msg.getServerGroupUuid())
                        .find();

                if (canRefresh) {
                    RefreshLoadBalancerMsg refreshmsg = new RefreshLoadBalancerMsg();
                    refreshmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(refreshmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(refreshmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                evt.setError(reply.getError());
                            } else {
                                evt.setInventory(LoadBalancerServerGroupInventory.valueOf(serverGroupVO));
                            }
                            bus.publish(evt);
                        }
                    });

                    chain.next();
                    return;
                }
                evt.setInventory( LoadBalancerServerGroupInventory.valueOf(serverGroupVO));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "change-lb-listener";
            }
        });
    }


}
