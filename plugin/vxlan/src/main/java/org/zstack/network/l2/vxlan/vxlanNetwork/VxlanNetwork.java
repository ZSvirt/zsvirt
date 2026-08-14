package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.L2NetworkGlobalConfig;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by weiwang on 01/03/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VxlanNetwork extends L2NoVlanNetwork implements ReportQuotaExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VxlanNetwork.class);

    @Autowired
    protected L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L2NetworkManager l2Mgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;

    public VxlanNetwork(L2NetworkVO self) {
        super(self);
    }

    public VxlanNetwork() {
        super(null);
    }

    private VxlanNetworkVO getSelf() {
        return dbf.findByUuid(self.getUuid(), VxlanNetworkVO.class);
    }

    @Override
    public void deleteHook(Completion completion) {
        if (L2NetworkGlobalConfig.DeleteL2BridgePhysically.value(Boolean.class)) {
            L2NetworkInventory l2Inv = getSelfInventory();
            deleteL2Bridge(l2Inv.getAttachedClusterUuids(), completion);
        } else {
            completion.success();
        }
    }

    @Override
    protected L2NetworkInventory getSelfInventory() {
        return L2VxlanNetworkInventory.valueOf(getSelf());
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CheckL2NetworkOnHostMsg) {
            handle((CheckL2NetworkOnHostMsg) msg);
        } else if (msg instanceof PrepareL2NetworkOnHostsMsg) {
            handle((PrepareL2NetworkOnHostsMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
        final PrepareL2NetworkOnHostReply reply = new PrepareL2NetworkOnHostReply();
        prepareL2NetworkOnHosts(Collections.singletonList(msg.getHost()), new Completion(msg) {
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
    }

    private void handle(final PrepareL2NetworkOnHostsMsg msg) {
        final PrepareL2NetworkOnHostsReply reply = new PrepareL2NetworkOnHostsReply();
        List<HostInventory> hosts = HostInventory.valueOf(Q.New(HostVO.class).in(HostVO_.uuid, msg.getHosts()).list());
        prepareL2NetworkOnHosts(hosts, new Completion(msg) {
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
    }

    @Override
    protected Flow updateL2NetworkVirtualNetworkIdInDb(final APIUpdateL2NetworkVirtualNetworkIdMsg msg) {
        return new Flow() {
            String __name__ = "update-vxlan-vni-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!self.getVirtualNetworkId().equals(msg.getVirtualNetworkId())) {
                    AllocateVniMsg vniMsg = new AllocateVniMsg();
                    vniMsg.setL2NetworkUuid(getSelf().getPoolUuid());
                    vniMsg.setRequiredVni(msg.getVirtualNetworkId());
                    bus.makeTargetServiceIdByResourceUuid(vniMsg, L2NetworkConstant.SERVICE_ID, getSelf().getPoolUuid());
                    MessageReply reply = bus.call(vniMsg);
                    if (!reply.isSuccess()) {
                        trigger.fail(reply.getError());
                        return;
                    }
                    AllocateVniReply r = reply.castReply();

                    VxlanNetworkVO vo = getSelf();
                    data.put(L2NetworkConstant.Param.OLD_VIRTUAL_NETWORK_ID.toString(), vo.getVirtualNetworkId());
                    vo.setVni(r.getVni());
                    vo.setVirtualNetworkId(vo.getVni());
                    dbf.updateAndRefresh(vo);
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                Integer oldVni = (Integer) data.get(L2NetworkConstant.Param.OLD_VIRTUAL_NETWORK_ID.toString());
                if (oldVni == null) {
                    trigger.rollback();
                    return;
                }

                VxlanNetworkVO vo = getSelf();
                vo.setVni(oldVni);
                vo.setVirtualNetworkId(vo.getVni());
                dbf.updateAndRefresh(vo);
                trigger.rollback();
            }
        };
    }

    private void prepareL2NetworkOnHosts(final List<HostInventory> hosts, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<CheckL2NetworkOnHostMsg> cmsgs = new ArrayList<CheckL2NetworkOnHostMsg>();
                for (HostInventory h : hosts) {
                    CheckL2NetworkOnHostMsg cmsg = new CheckL2NetworkOnHostMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setL2NetworkUuid(self.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, self.getUuid());
                    cmsgs.add(cmsg);
                }

                if (cmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(cmsgs, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                trigger.fail(r.getError());
                                return;
                            }
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            private void realize(final Iterator<HostInventory> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                HostInventory host = it.next();
                realizeNetwork(host.getUuid(), host.getHypervisorType(), new Completion(trigger) {
                    @Override
                    public void success() {
                        realize(it, trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                realize(hosts.iterator(), trigger);
            }
        }).done(new FlowDoneHandler(completion) {
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

    protected void realizeNetwork(String hostUuid, String htype, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType);
        ext.realize(getSelfInventory(), hostUuid, completion);
    }

    private void handle(final CheckL2NetworkOnHostMsg msg) {
        superHandle((L2NetworkMessage) msg);
    }

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        deleteHook(new Completion(msg) {
            @Override
            public void success() {
                dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
                extpEmitter.afterDelete(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        throw new CloudRuntimeException("VxlanNetwork can not detach from cluster which VxlanNetworkPool should be used");
    }

    protected void handle(final APIAttachL2NetworkToClusterMsg msg) {
        throw new CloudRuntimeException("VxlanNetwork can not attach to cluster which VxlanNetworkPool should be used");
    }

    private void superHandle(L2NetworkMessage msg) {
        super.handleMessage((Message) msg);
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new VxlanNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateL2VxlanNetworkMsg.class)
                .addCounterQuota(VxlanNetworkQuotaConstant.VXLAN_NUM));

        return list(quota);
    }

}
