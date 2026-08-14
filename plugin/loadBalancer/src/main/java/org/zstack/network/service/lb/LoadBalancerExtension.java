package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.acl.RefreshAccessControlListExtensionPoint;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.acl.AccessControlListEntryInventory;
import org.zstack.header.acl.AccessControlListInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipReleaseExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 8/13/2015.
 */
public class LoadBalancerExtension extends AbstractNetworkServiceExtension implements VipReleaseExtensionPoint, RefreshAccessControlListExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LoadBalancerExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private L3NetworkManager l3Mgr;

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE;
    }

    @Transactional(readOnly = true)
    private List<Tuple> getLbTuple(VmInstanceSpec servedVm) {
        String sql = "select grp.uuid, grp.loadBalancerUuid, ref.vmNicUuid, nic.l3NetworkUuid from " +
                "LoadBalancerServerGroupVmNicRefVO ref, LoadBalancerServerGroupVO grp, VmNicVO nic" +
                " where ref.serverGroupUuid = grp.uuid and ref.vmNicUuid = nic.uuid and nic.uuid in (:nicUuids)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("nicUuids", transformAndRemoveNull(servedVm.getDestNics(), VmNicInventory::getUuid));
        return q.getResultList();
    }

    private boolean isLbShouldBeAttachedToBackend(String vmUuid, String l3Uuid) {
        boolean ipChanged = new StaticIpOperator().isIpChange(vmUuid, l3Uuid);

        L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
        boolean l3Need = l3Mgr.applyNetworkServiceWhenVmStateChange(l3Vo.getType());
        return ipChanged || l3Need;
    }

    @Override
    public void applyNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final Completion completion) {
        List<Tuple> ts = getLbTuple(servedVm);
        if (ts.isEmpty()) {
            completion.success();
            return;
        }

        Map<String, LoadBalancerActiveVmNicMsg> m = new HashMap<String, LoadBalancerActiveVmNicMsg>();
        Map<String, L3NetworkVO> l3Map = new HashMap<>();
        for (Tuple t : ts) {
            String serverGroupUuid = t.get(0, String.class);
            String lbUuid =  t.get(1, String.class);
            String nicUuid = t.get(2, String.class);
            String l3Uuid = t.get(3, String.class);
            L3NetworkVO l3Vo = l3Map.get(l3Uuid);
            if (l3Vo == null) {
                l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
                l3Map.put(l3Uuid, l3Vo);
            }
            /* if l3 network doesn't want to apply lb for vm start, skip this nic */
            if (!isLbShouldBeAttachedToBackend(servedVm.getVmInventory().getUuid(), l3Uuid)) {
                continue;
            }

            LoadBalancerActiveVmNicMsg msg = m.get(serverGroupUuid);
            if (msg == null) {
                msg = new LoadBalancerActiveVmNicMsg();
                msg.setLoadBalancerUuid(lbUuid);
                msg.setServerGroupUuid(serverGroupUuid);
                msg.setVmNicUuids(new ArrayList<String>());
                bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, lbUuid);
                m.put(serverGroupUuid, msg);
            }
            msg.getVmNicUuids().add(nicUuid);
        }

        if (m.isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("active-nic-for-vm-%s-on-lb", servedVm.getVmInventory().getUuid()));
        for (final LoadBalancerActiveVmNicMsg msg : m.values()) {
            chain.then(new Flow() {
                boolean s = false;

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                s = true;
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                }

                @Override
                public void rollback(final FlowRollback trigger, Map data) {
                    if (!s) {
                        LoadBalancerDeactiveVmNicMsg dmsg = new LoadBalancerDeactiveVmNicMsg();
                        dmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
                        dmsg.setServerGroupUuids(Arrays.asList(msg.getServerGroupUuid()));
                        dmsg.setVmNicUuids(msg.getVmNicUuids());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, LoadBalancerConstants.SERVICE_ID, msg.getLoadBalancerUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    //TODO: add GC
                                    logger.warn(String.format("failed to deactive vm nics[uuids: %s] on the load balancer[uuid:%s]", msg.getVmNicUuids(), msg.getLoadBalancerUuid()));
                                }
                            }
                        });
                    }

                    trigger.rollback();
                }
            });
        }

        chain.done(new FlowDoneHandler(completion) {
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

    @Override
    public void releaseNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        List<Tuple> ts = getLbTuple(servedVm);
        if (ts.isEmpty()) {
            completion.done();
            return;
        }
        if (!Optional.ofNullable(servedVm.getDestHost()).isPresent()){
            completion.done();
            return;
        }
        class Triplet {
            String lbUuid;
            Set<String> serverGroupUuids;
            Set<String> vmNicUuids;
        }

        Map<String, Triplet> mt = new HashMap<String, Triplet>();
        Map<String, L3NetworkVO> l3Map = new HashMap<>();
        for (Tuple t : ts) {
            String lbUuid = t.get(1, String.class);
            Triplet tr = mt.get(lbUuid);
            if (tr == null) {
                tr = new Triplet();
                tr.serverGroupUuids = new HashSet<>();
                tr.lbUuid = t.get(1, String.class);
                tr.vmNicUuids = new HashSet<>();
                mt.put(tr.lbUuid, tr);
            }
            String l3Uuid = t.get(3, String.class);
            L3NetworkVO l3Vo = l3Map.get(l3Uuid);
            if (l3Vo == null) {
                l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
                l3Map.put(l3Uuid, l3Vo);
            }
            /* if l3 network doesn't want to apply lb for vm start, skip this nic */
            if (!isLbShouldBeAttachedToBackend(servedVm.getVmInventory().getUuid(), l3Uuid)
                    && !LoadBalancerConstants.vmOperationForDetachListener.contains(servedVm.getCurrentVmOperation())) {
                continue;
            }
            tr.vmNicUuids.add(t.get(2, String.class));
            tr.serverGroupUuids.add(t.get(0, String.class));
        }

        List<NeedReplyMessage> msgs = new ArrayList<>();
        if (servedVm.getCurrentVmOperation() == VmOperation.Destroy || servedVm.getCurrentVmOperation() == VmOperation.DetachNic) {
            msgs.addAll(transform(mt.entrySet(), arg -> {
                LoadBalancerRemoveVmNicMsg msg = new LoadBalancerRemoveVmNicMsg();
                msg.setVmNicUuids(new ArrayList<>(arg.getValue().vmNicUuids));
                msg.setServerGroupUuids(new ArrayList<>(arg.getValue().serverGroupUuids));
                msg.setLoadBalancerUuid(arg.getValue().lbUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, arg.getKey());
                return msg;
            }));
        } else {
            msgs.addAll(transform(mt.entrySet(), arg -> {
                LoadBalancerDeactiveVmNicMsg msg = new LoadBalancerDeactiveVmNicMsg();
                msg.setVmNicUuids(new ArrayList<>(arg.getValue().vmNicUuids));
                msg.setServerGroupUuids(new ArrayList<>(arg.getValue().serverGroupUuids));
                msg.setLoadBalancerUuid(arg.getValue().lbUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, arg.getKey());
                return msg;
            }));
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        //TODO
                        logger.warn(String.format("failed to deactive a vm nic on lb, need a cleanup, %s", r.getError()));
                    }
                }

                completion.done();
            }
        });
    }

    @Override
    public String getVipUse() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING;
    }

    private void releaseServicesOnVip(final Iterator<LoadBalancerVO> it, final Completion completion){
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        String lbUuid = it.next().getUuid();
        DeleteLoadBalancerOnlyMsg msg = new DeleteLoadBalancerOnlyMsg();
        msg.setLoadBalancerUuid(lbUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, lbUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    releaseServicesOnVip(it, completion);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void releaseServicesOnVip(VipInventory vip, final Completion completion) {
        SimpleQuery<LoadBalancerVO> q = dbf.createQuery(LoadBalancerVO.class);
        q.add(LoadBalancerVO_.vipUuid, Op.EQ, vip.getUuid());
        List<LoadBalancerVO> rules = q.list();
        releaseServicesOnVip(rules.iterator(), completion);
    }

    @Override
    public void beforeAddIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry) {
        return;
    }

    private void refreshLoadBalancerByAcl(String aclUuid) {
        List<String> listenerUuids = Q.New(LoadBalancerListenerACLRefVO.class).select(LoadBalancerListenerACLRefVO_.listenerUuid)
                                      .eq(LoadBalancerListenerACLRefVO_.aclUuid, aclUuid).listValues();
        if (listenerUuids.isEmpty()) {
            return;
        }

        List<LoadBalancerListenerVO> listenerVOS = Q.New(LoadBalancerListenerVO.class)
                .in(LoadBalancerListenerVO_.uuid, listenerUuids).list();
        for (LoadBalancerListenerVO listenerVO : listenerVOS) {
            List<String> nicUuids = new ArrayList<>();
            List<String> serverIps = new ArrayList<>();
            for (LoadBalancerListenerServerGroupRefVO ref : listenerVO.getServerGroupRefs()) {
                LoadBalancerServerGroupVO groupVO = dbf.findByUuid(ref.getServerGroupUuid(), LoadBalancerServerGroupVO.class);
                nicUuids.addAll(groupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                        .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList()));
                serverIps.addAll(groupVO.getLoadBalancerServerGroupServerIps().stream()
                        .map(LoadBalancerServerGroupServerIpVO::getIpAddress).collect(Collectors.toList()));
            }
            if (nicUuids.isEmpty() && serverIps.isEmpty()) {
                /* listener is not bound any vmnic or server ips */
                listenerUuids.remove(listenerVO.getUuid());
                continue;
            }
        }
        if (listenerUuids.isEmpty()) {
            return;
        }

        List<String> lbUuids = Q.New(LoadBalancerListenerVO.class).select(LoadBalancerListenerVO_.loadBalancerUuid)
                                .in(LoadBalancerListenerVO_.uuid, listenerUuids).listValues();
        if (lbUuids.isEmpty()) {
            return;
        }

        List<RefreshLoadBalancerMsg> rmsgs = new ArrayList<>();
        for (String lbUuid : lbUuids) {

            RefreshLoadBalancerMsg msg = new RefreshLoadBalancerMsg();
            msg.setUuid(lbUuid);
            bus.makeLocalServiceId(msg, LoadBalancerConstants.SERVICE_ID);
            rmsgs.add(msg);
        }
        new While<>(rmsgs).each((msg, comp) -> {
            bus.send(msg, new CloudBusCallBack(comp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                    }
                    comp.done();
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }
    @Override
    public void afterAddIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry) {
        refreshLoadBalancerByAcl(acl.getUuid());
    }

    @Override
    public void beforeDeleteIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry) {
        return;
    }

    @Override
    public void afterDeleteIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry) {
        refreshLoadBalancerByAcl(acl.getUuid());
    }

    @Override
    public void beforeDeleteAcl(AccessControlListInventory acl) {
        return;
    }

    @Override
    public void afterDeleteAcl(AccessControlListInventory acl) {
        return;
    }


    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags,  Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
