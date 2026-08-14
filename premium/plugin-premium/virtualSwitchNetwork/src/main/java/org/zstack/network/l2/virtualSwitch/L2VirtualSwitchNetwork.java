package org.zstack.network.l2.virtualSwitch;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.compute.bonding.HostNetworkBondingUtils;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.bonding.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.network.l2.L2NetworkHostHelper;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.pciDevice.PciDeviceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;


/**
 * Created by shixin.ruan on 2023/09/01.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2VirtualSwitchNetwork extends L2NoVlanNetwork {
    private static final CLogger logger = Utils.getLogger(L2VirtualSwitchNetwork.class);
    private static final L2NetworkHostHelper l2NetworkHostHelper = new L2NetworkHostHelper();
    private static final VirtualSwitchHelper virtualSwitchHelper = new VirtualSwitchHelper();

    public L2VirtualSwitchNetwork(L2NetworkVO self) {
        super(self);
    }

    public L2VirtualSwitchNetwork() {
    }

    private L2VirtualSwitchNetworkVO getSelf() {
        return (L2VirtualSwitchNetworkVO) self;
    }

    @Override
    protected L2NetworkInventory getSelfInventory() {
        return L2VirtualSwitchNetworkInventory.valueOf(getSelf());
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToHostMsg) {
            handle((APIAttachL2NetworkToHostMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromHostMsg) {
            handle((APIDetachL2NetworkFromHostMsg) msg);
        } else if (msg instanceof APIUpdateVirtualSwitchUplinkBondingsMsg) {
            handle((APIUpdateVirtualSwitchUplinkBondingsMsg) msg);
        } else if (msg instanceof APIUpdateVirtualSwitchUplinkGroupMsg) {
            handle((APIUpdateVirtualSwitchUplinkGroupMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private Flow deleteVirtualSwitchBondingFlow(List<HostNetworkBondingVO> bondingVOs) {
        return new NoRollbackFlow() {
            String __name__ = "delete-bonding";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (CollectionUtils.isEmpty(bondingVOs)) {
                    trigger.next();
                    return;
                }

                new While<>(bondingVOs).each((vo, wcomp) -> {
                    ErrorCode err = HostNetworkBondingUtils.validateDeleteBonding(vo);
                    if (err != null) {
                        logger.warn(String.format("delete bonding[uuid:%s] failed:%s", vo.getUuid(), err));
                        wcomp.done();
                        return;
                    }

                    BondingDeletionMsg dmsg = new BondingDeletionMsg();
                    dmsg.setBondingUuid(vo.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, HostNetworkBondingConstant.SERVICE_ID, vo.getUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("delete bonding[uuid:%s] failed:%s", vo.getUuid(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    private static class AttachContext {
        final List<String> attachedPortGroupUuids = Collections.synchronizedList(new ArrayList<>());
    }

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        AttachContext ctx = new AttachContext();
        final APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-virtual-switch-%s-to-cluster-%s", msg.getL2NetworkUuid(), msg.getClusterUuid()));
        chain.then(new Flow() {
            String __name__ = "attach-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                AttachL2NetworkToClusterMsg amsg = new AttachL2NetworkToClusterMsg();
                amsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                amsg.setClusterUuid(msg.getClusterUuid());
                amsg.setL2ProviderType(msg.getL2ProviderType());
                amsg.setHostParams(msg.getHostParams());
                bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
                bus.send(amsg, new CloudBusCallBack(amsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("attach virtual switch[uuid:%s] to cluster[uuid:%s] failed:%s",
                                    msg.getL2NetworkUuid(), msg.getClusterUuid(), reply.getError().toString()));
                            trigger.fail(reply.getError());
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                dmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                dmsg.setClusterUuid(msg.getClusterUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("detach virtual switch[uuid:%s] for cluster[uuid:%s] failed:%s",
                                    msg.getL2NetworkUuid(), msg.getClusterUuid(), reply.getError().toString()));
                        }
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "attach-no-vlan-port-group-on-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String uuid = Q.New(L2PortGroupNetworkVO.class)
                        .select(L2PortGroupNetworkVO_.uuid)
                        .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid())
                        .eq(L2PortGroupNetworkVO_.vlanId, 0)
                        .findValue();

                if (uuid == null) {
                    trigger.next();
                    return;
                }

                AttachL2NetworkToClusterMsg amsg = new AttachL2NetworkToClusterMsg();
                amsg.setL2NetworkUuid(uuid);
                amsg.setClusterUuid(msg.getClusterUuid());
                amsg.setL2ProviderType(msg.getL2ProviderType());
                amsg.setHostParams(msg.getHostParams());
                bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("attach port group[uuid:%s] to cluster[uuid:%s] failed:%s",
                                    msg.getL2NetworkUuid(), msg.getClusterUuid(), reply.getError().toString()));
                            trigger.fail(reply.getError());
                        } else {
                            ctx.attachedPortGroupUuids.add(uuid);
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                new While<>(ctx.attachedPortGroupUuids).step((uuid, wcomp) -> {
                    L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                    dmsg.setL2NetworkUuid(uuid);
                    dmsg.setClusterUuid(msg.getClusterUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("detach port group[uuid:%s] from cluster[uuid:%s] failed:%s",
                                        uuid, msg.getClusterUuid(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "attach-vlan-port-group-on-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> uuids = Q.New(L2PortGroupNetworkVO.class)
                        .select(L2PortGroupNetworkVO_.uuid)
                        .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid())
                        .notEq(L2PortGroupNetworkVO_.vlanId, 0)
                        .listValues();

                if (uuids.isEmpty()) {
                    logger.info(String.format("There is no vlan port group on virtual switch[uuid:%s]", msg.getL2NetworkUuid()));
                    trigger.next();
                    return;
                }

                new While<>(uuids).step((uuid, wcomp) -> {
                    AttachL2NetworkToClusterMsg amsg = new AttachL2NetworkToClusterMsg();
                    amsg.setL2NetworkUuid(uuid);
                    amsg.setClusterUuid(msg.getClusterUuid());
                    amsg.setL2ProviderType(msg.getL2ProviderType());
                    amsg.setHostParams(msg.getHostParams());
                    bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
                    bus.send(amsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("attach port group[uuid:%s] to cluster[uuid:%s] failed:%s",
                                        msg.getL2NetworkUuid(), msg.getClusterUuid(), reply.getError().toString()));
                                wcomp.addError(reply.getError());
                                wcomp.allDone();
                            } else {
                                ctx.attachedPortGroupUuids.add(uuid);
                                wcomp.done();
                            }
                        }
                    });
                }, 10).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(multiErr(errorCodeList));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(final APIAttachL2NetworkToHostMsg msg) {
        AttachContext ctx = new AttachContext();
        final APIAttachL2NetworkToHostEvent evt = new APIAttachL2NetworkToHostEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-virtual-switch-%s-to-host-%s", msg.getL2NetworkUuid(), msg.getHostUuid()));
        chain.then(new Flow() {
            String __name__ = "attach-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                AttachL2NetworkToHostMsg amsg = new AttachL2NetworkToHostMsg();
                amsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                amsg.setHostUuid(msg.getHostUuid());
                amsg.setL2ProviderType(msg.getL2ProviderType());
                amsg.setHostParam(msg.getHostParam());
                bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
                bus.send(amsg, new CloudBusCallBack(amsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("attach virtual switch[uuid:%s] to host[uuid:%s] failed:%s",
                                    msg.getL2NetworkUuid(), msg.getHostUuid(), reply.getError().toString()));
                            trigger.fail(reply.getError());
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                L2NetworkDetachFromHostMsg dmsg = new L2NetworkDetachFromHostMsg();
                dmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                dmsg.setHostUuid(msg.getHostUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("detach virtual switch[uuid:%s] from host[uuid:%s] failed:%s",
                                    msg.getL2NetworkUuid(), msg.getHostUuid(), reply.getError().toString()));
                        }
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "attach-no-vlan-port-group-on-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String uuid = Q.New(L2PortGroupNetworkVO.class)
                        .select(L2PortGroupNetworkVO_.uuid)
                        .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid())
                        .eq(L2PortGroupNetworkVO_.vlanId, 0)
                        .findValue();

                if (uuid == null) {
                    trigger.next();
                    return;
                }

                AttachL2NetworkToHostMsg amsg = new AttachL2NetworkToHostMsg();
                amsg.setL2NetworkUuid(uuid);
                amsg.setHostUuid(msg.getHostUuid());
                amsg.setL2ProviderType(msg.getL2ProviderType());
                amsg.setHostParam(msg.getHostParam());
                bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("attach port group[uuid:%s] to host[uuid:%s] failed:%s",
                                    msg.getL2NetworkUuid(), msg.getHostUuid(), reply.getError().toString()));
                            trigger.fail(reply.getError());
                        } else {
                            ctx.attachedPortGroupUuids.add(uuid);
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                new While<>(ctx.attachedPortGroupUuids).step((uuid, wcomp) -> {
                    L2NetworkDetachFromHostMsg dmsg = new L2NetworkDetachFromHostMsg();
                    dmsg.setL2NetworkUuid(uuid);
                    dmsg.setHostUuid(msg.getHostUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("detach port group[uuid:%s] from host[uuid:%s] failed:%s",
                                        uuid, msg.getHostUuid(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "attach-vlan-port-group-on-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> uuids = Q.New(L2PortGroupNetworkVO.class)
                        .select(L2PortGroupNetworkVO_.uuid)
                        .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid())
                        .notEq(L2PortGroupNetworkVO_.vlanId, 0)
                        .listValues();

                if (uuids.isEmpty()) {
                    logger.info(String.format("There is no vlan port group on virtual switch[uuid:%s]", msg.getL2NetworkUuid()));
                    trigger.next();
                    return;
                }

                new While<>(uuids).step((uuid, wcomp) -> {
                    AttachL2NetworkToHostMsg amsg = new AttachL2NetworkToHostMsg();
                    amsg.setL2NetworkUuid(uuid);
                    amsg.setHostUuid(msg.getHostUuid());
                    amsg.setL2ProviderType(msg.getL2ProviderType());
                    amsg.setHostParam(msg.getHostParam());
                    bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
                    bus.send(amsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("attach port group[uuid:%s] to host[uuid:%s] failed:%s",
                                        msg.getL2NetworkUuid(), msg.getHostUuid(), reply.getError().toString()));
                                wcomp.addError(reply.getError());
                                wcomp.allDone();
                            } else {
                                ctx.attachedPortGroupUuids.add(uuid);
                                wcomp.done();
                            }
                        }
                    });
                }, 10).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(multiErr(errorCodeList));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        final APIDetachL2NetworkFromClusterEvent evt = new APIDetachL2NetworkFromClusterEvent(msg.getId());

        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .listValues();

        List<HostNetworkBondingVO> bondingVOs = VirtualSwitchUtils.getUplinkBondingOfvSwitch(msg.getL2NetworkUuid(), hostUuids);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-virtual-switch-%s-from-cluster-%s", msg.getL2NetworkUuid(), msg.getClusterUuid()));
        chain.then(new NoRollbackFlow() {
                    String __name__ = "detach-port-group-on-virtual-switch";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> uuids = Q.New(L2PortGroupNetworkVO.class)
                                .select(L2PortGroupNetworkVO_.uuid).eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid()).listValues();

                        if (uuids.isEmpty()) {
                            logger.info(String.format("there is no port group on virtual switch[uuid:%s]", msg.getL2NetworkUuid()));
                            trigger.next();
                            return;
                        }

                        logger.info(String.format("detach port group %s from virtual switch %s", uuids, msg.getL2NetworkUuid()));

                        new While<>(uuids).step((uuid, wcomp) -> {
                            L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                            dmsg.setL2NetworkUuid(uuid);
                            dmsg.setClusterUuid(msg.getClusterUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                            bus.send(dmsg, new CloudBusCallBack(wcomp) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("detach port group[uuid:%s] from cluster[uuid:%s] failed: %s",
                                                uuid, msg.getClusterUuid(), reply.getError().toString()));
                                    }
                                    wcomp.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(msg) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "detach-virtual-switch";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                        dmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                        dmsg.setClusterUuid(msg.getClusterUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                        bus.send(dmsg, new CloudBusCallBack(dmsg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(reply.getError().toString());
                                }
                                trigger.next();
                            }
                        });
                    }
                }).then(deleteVirtualSwitchBondingFlow(bondingVOs))
                .done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                        evt.setInventory(self.toInventory());
                        bus.publish(evt);
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                }).start();
    }

    private void handle(final APIDetachL2NetworkFromHostMsg msg) {
        final APIDetachL2NetworkFromHostEvent evt = new APIDetachL2NetworkFromHostEvent(msg.getId());

        List<HostNetworkBondingVO> bondingVOs = VirtualSwitchUtils.getUplinkBondingOfvSwitch(msg.getL2NetworkUuid(), Collections.singletonList(msg.getHostUuid()));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-virtual-switch-%s-from-host-%s", msg.getL2NetworkUuid(), msg.getHostUuid()));
        chain.then(new NoRollbackFlow() {
                    String __name__ = "detach-port-group-on-virtual-switch-from-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> l2Uuids = Q.New(L2PortGroupNetworkVO.class).select(L2PortGroupNetworkVO_.uuid)
                                .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid()).listValues();

                        if (l2Uuids.isEmpty()) {
                            logger.info(String.format("there is no port group on virtual switch[uuid:%s]", msg.getL2NetworkUuid()));
                            trigger.next();
                            return;
                        }

                        new While<>(l2Uuids).step((uuid, wcomp) -> {
                            L2NetworkDetachFromHostMsg dmsg = new L2NetworkDetachFromHostMsg();
                            dmsg.setL2NetworkUuid(uuid);
                            dmsg.setHostUuid(msg.getHostUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                            bus.send(dmsg, new CloudBusCallBack(wcomp) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("detach port group[uuid:%s] from host[uuid:%s] failed: %s",
                                                uuid, msg.getHostUuid(), reply.getError().toString()));
                                    }
                                    wcomp.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(msg) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "detach-virtual-switch-from-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        L2NetworkDetachFromHostMsg dmsg = new L2NetworkDetachFromHostMsg();
                        dmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                        dmsg.setHostUuid(msg.getHostUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                        bus.send(dmsg, new CloudBusCallBack(dmsg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(reply.getError().toString());
                                }
                                trigger.next();
                            }
                        });
                    }
                }).then(deleteVirtualSwitchBondingFlow(bondingVOs))
                .done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                        evt.setInventory(self.toInventory());
                        bus.publish(evt);
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                }).start();
    }

    private void handle(APIUpdateVirtualSwitchUplinkBondingsMsg msg) {
        class Context {
            final List<String> failedHostUuids = new ArrayList<>();
        }
        Context ctx = new Context();
        final APIUpdateVirtualSwitchUplinkBondingsEvent evt = new APIUpdateVirtualSwitchUplinkBondingsEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("update-virtual-switch-%s-uplink-bondings", msg.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "create-or-update-uplink-bonding-systemTag";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VirtualSwitchUtils.createUplinkBondingSystemTag(msg.getUuid(), msg.getMode(), msg.getXmitHashPolicy());
                trigger.next();
            }
        });

        if (msg.getBondingName() != null) {
            chain.then(new NoRollbackFlow() {
                String __name__ = "update-uplink-bonding-name";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    VirtualSwitchUtils.changeVirtualSwitchUplinkBondingName(msg.getUuid(), msg.getBondingName());
                    trigger.next();
                }
            }).done(new FlowDoneHandler(msg) {
                @Override
                public void handle(Map data) {
                    bus.publish(evt);
                }
            }).error(new FlowErrorHandler(msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    evt.setError(errCode);
                    bus.publish(evt);
                }
            }).start();
            return;
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "update-bondings-on-hosts";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HostNetworkBondingVO> bondingVOs = SQL.New("select bonding from" +
                                " HostNetworkBondingVO bonding, L2VirtualSwitchNetworkVO l2, L2NetworkHostRefVO ref where" +
                                " bonding.bondingName = l2.physicalInterface" +
                                " and l2.uuid = ref.l2NetworkUuid" +
                                " and l2.uuid = :l2NetworkUuid" +
                                " and ref.hostUuid = bonding.hostUuid")
                        .param("l2NetworkUuid", msg.getUuid())
                        .list();

                if (bondingVOs.isEmpty()) {
                    logger.info(String.format("virtual switch[uuid:%s] has not attached to any hosts yet", msg.getUuid()));
                    bus.publish(evt);
                    return;
                }

                List<UpdateBondingMsg> msgs = new ArrayList<>();
                bondingVOs.forEach(bondingVO -> {
                    UpdateBondingMsg updateBondingMsg = new UpdateBondingMsg();
                    updateBondingMsg.setUuid(bondingVO.getUuid());
                    updateBondingMsg.setSlaveUuids(bondingVO.getSlaves().stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toList()));
                    updateBondingMsg.setType(bondingVO.getType());
                    updateBondingMsg.setMode(msg.getMode());
                    updateBondingMsg.setXmitHashPolicy(msg.getXmitHashPolicy());
                    updateBondingMsg.setDescription(bondingVO.getDescription());

                    bus.makeLocalServiceId(updateBondingMsg, HostNetworkBondingConstant.SERVICE_ID);
                    msgs.add(updateBondingMsg);
                });

                List<HostNetworkBondingInventory> inventories = Collections.synchronizedList(new ArrayList<>());
                List<String> failedBondingUuids = Collections.synchronizedList(new ArrayList<>());
                new While<>(msgs).step((umsg, wcomp) ->
                        bus.send(umsg, new CloudBusCallBack(wcomp) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    UpdateBondingReply uReply = reply.castReply();
                                    inventories.add(uReply.getInventory());
                                } else {
                                    ErrorCode err = reply.getError();
                                    logger.warn(String.format("failed to update bonding[uuid:%s]: %s", umsg.getUuid(), err));
                                    failedBondingUuids.add(umsg.getUuid());
                                }
                                wcomp.done();
                            }
                        }), 10).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!failedBondingUuids.isEmpty()) {
                            ctx.failedHostUuids.addAll(Q.New(HostNetworkBondingVO.class)
                                    .select(HostNetworkBondingVO_.hostUuid)
                                    .in(HostNetworkBondingVO_.uuid, failedBondingUuids)
                                    .listValues());
                            VirtualSwitchUtils.deleteUplinkGroup(msg.getL2NetworkUuid(), ctx.failedHostUuids);

                            List<String> portGroupUuids = Q.New(L2PortGroupNetworkVO.class)
                                    .select(L2PortGroupNetworkVO_.uuid)
                                    .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid())
                                    .listValues();
                            ctx.failedHostUuids.forEach(uuid -> {
                                portGroupUuids.forEach(puuid -> {
                                    L2NetworkHostUtils.deleteL2NetworkHostRef(puuid, uuid);
                                });
                            });
                        }

                        evt.setInventories(inventories);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "detach-failed-hosts-of-update-bondings";

            @Override
            public boolean skip(Map data) {
                return ctx.failedHostUuids.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> l2Uuids = Q.New(L2PortGroupNetworkVO.class).select(L2PortGroupNetworkVO_.uuid)
                        .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getL2NetworkUuid()).listValues();

                if (l2Uuids.isEmpty()) {
                    logger.info(String.format("there is no port group on virtual switch[uuid:%s]", msg.getL2NetworkUuid()));
                    trigger.next();
                    return;
                }

                List<BatchL2NetworkDetachFromHostMsg> msgs = new ArrayList<>();
                for (String l2Uuid : l2Uuids) {
                    BatchL2NetworkDetachFromHostMsg dmsg = new BatchL2NetworkDetachFromHostMsg();
                    dmsg.setL2NetworkUuid(l2Uuid);
                    dmsg.setHostUuids(ctx.failedHostUuids);

                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, l2Uuid);
                    msgs.add(dmsg);
                }
                new While<>(msgs).step((dmsg, wcomp) -> {
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("detach port group[uuid:%s] from host %s failed:%s",
                                        dmsg.getL2NetworkUuid(), dmsg.getHostUuids(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });

            }
        }).then(new NoRollbackFlow() {
            String __name__ = "detach-virtual-switch-from-host";

            @Override
            public boolean skip(Map data) {
                return ctx.failedHostUuids.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BatchL2NetworkDetachFromHostMsg dmsg = new BatchL2NetworkDetachFromHostMsg();
                dmsg.setL2NetworkUuid(msg.getUuid());
                dmsg.setHostUuids(ctx.failedHostUuids);
                bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("detach virtual switch[uuid:%s] from host %s failed: %s",
                                    dmsg.getL2NetworkUuid(), dmsg.getHostUuids(), reply.getError().toString()));
                        }
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIUpdateVirtualSwitchUplinkGroupMsg msg) {
        class Context {
            String oldInterfaceName;
            String oldInterfaceUuid;
            String oldInterfaceType;
            HostNetworkBondingInventory createdBondingInv;
            List<HostNetworkBondingVO> bondings;
        }
        final Context ctx = new Context();
        final APIUpdateVirtualSwitchUplinkGroupEvent evt = new APIUpdateVirtualSwitchUplinkGroupEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("update-virtual-switch-%s-uplink-group-of-host-%s", msg.getUuid(), msg.getHostUuid()));

        UplinkGroupVO ug = VirtualSwitchUtils.getUplinkGroup(msg.getUuid(), msg.getHostUuid());

        boolean fromBonding = UplinkGroupType.Bonding.equals(ug.getType());
        boolean toBonding = msg.getSlaveUuids().size() > 1;

        if (fromBonding && toBonding) {
            chain.then(new NoRollbackFlow() {
                String __name__ = "update-uplink-bonding-slaves";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    String bondingUuid = Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.uuid)
                            .eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid())
                            .eq(HostNetworkBondingVO_.bondingName, ug.getInterfaceName())
                            .findValue();

                    if (bondingUuid == null) {
                        trigger.fail(operr("The uplink bonding[%s] is not found on host[uuid:%s] for virtual switch[uuid:%s]",
                                ug.getInterfaceName(), msg.getHostUuid(), msg.getUuid()));
                        return;
                    }

                    UpdateBondingMsg umsg = new UpdateBondingMsg();
                    umsg.setUuid(bondingUuid);
                    umsg.setSlaveUuids(msg.getSlaveUuids());
                    umsg.setType(msg.getType());

                    bus.makeTargetServiceIdByResourceUuid(umsg, HostNetworkBondingConstant.SERVICE_ID, bondingUuid);
                    bus.send(umsg, new CloudBusCallBack(umsg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                }
            });
        } else {
            String bondingUuid = Q.New(HostNetworkBondingVO.class)
                    .select(HostNetworkBondingVO_.uuid)
                    .eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid())
                    .eq(HostNetworkBondingVO_.bondingName, self.getPhysicalInterface())
                    .findValue();

            if (!fromBonding) {
                chain.then(new Flow() {
                    String __name__ = "update-old-interface-type-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HostNetworkInterfaceVO oldInterface = Q.New(HostNetworkInterfaceVO.class)
                                .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                                .eq(HostNetworkInterfaceVO_.interfaceName, ug.getInterfaceName())
                                .find();

                        ctx.oldInterfaceType = oldInterface.getInterfaceType();
                        oldInterface.setInterfaceType(NetworkInterfaceType.noMaster.toString());
                        dbf.updateAndRefresh(oldInterface);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        HostNetworkInterfaceVO oldInterface = Q.New(HostNetworkInterfaceVO.class)
                                .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                                .eq(HostNetworkInterfaceVO_.interfaceName, ug.getInterfaceName())
                                .find();

                        if (ctx.oldInterfaceType != null) {
                            oldInterface.setInterfaceType(ctx.oldInterfaceType);
                            dbf.updateAndRefresh(oldInterface);
                        }
                        trigger.rollback();
                    }
                });
            }

            if (toBonding) {
                String mode = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByResourceUuid(msg.getUuid(),
                        VirtualSwitchSystemTags.BONDING_MODE_TOKEN);
                String xmitHashPolicy;
                if (HostNetworkBondingConstant.BONDING_MODE_AB.equals(mode)) {
                    xmitHashPolicy = null;
                } else {
                    xmitHashPolicy = VirtualSwitchSystemTags.UPLINK_BONDING.getTokenByResourceUuid(msg.getUuid(),
                            VirtualSwitchSystemTags.XMIT_HASH_POLICY_TOKEN);
                }

                chain.then(new Flow() {
                    String __name__ = "create-bonding-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateBondingMsg cmsg = new CreateBondingMsg();
                        cmsg.setHostUuid(msg.getHostUuid());
                        cmsg.setBondingName(self.getPhysicalInterface());
                        cmsg.setSlaveUuids(msg.getSlaveUuids());
                        cmsg.setType(msg.getType());
                        cmsg.setMode(mode);
                        cmsg.setXmitHashPolicy(xmitHashPolicy);
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        cmsg.setDbOnly(true);

                        bus.makeLocalServiceId(cmsg, HostNetworkBondingConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(cmsg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateBondingReply r = reply.castReply();
                                    ctx.createdBondingInv = r.getInventory();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (ctx.createdBondingInv == null) {
                            logger.warn("bonding is not created, skip deleting");
                            trigger.rollback();
                            return;
                        }

                        BondingDeletionMsg dmsg = new BondingDeletionMsg();
                        dmsg.setBondingUuid(ctx.createdBondingInv.getUuid());
                        dmsg.setDbOnly(true);

                        bus.makeTargetServiceIdByResourceUuid(dmsg, HostNetworkBondingConstant.SERVICE_ID, ctx.createdBondingInv.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("delete bonding[uuid:%s] in database failed: %s", ctx.createdBondingInv.getUuid(), reply.getError().toString()));
                                }
                                trigger.rollback();
                            }
                        });
                    }
                });
            }

            chain.then(new Flow() {
                String __name__ = "update-UplinkGroupVO";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    ctx.oldInterfaceName = ug.getInterfaceName();
                    if (fromBonding) {
                        ctx.oldInterfaceUuid = ug.getBondingUuid();
                    } else {
                        ctx.oldInterfaceUuid = ug.getInterfaceUuid();
                    }

                    if (toBonding) {
                        if (ctx.createdBondingInv == null) {
                            trigger.fail(operr("an unexpected error caused the bonding to not be created on" +
                                            " host[uuid:%s] for virtual switch[uuid:%s]",
                                    msg.getHostUuid(), msg.getUuid()));
                            return;
                        }

                        ug.setInterfaceName(self.getPhysicalInterface());
                        ug.setType(UplinkGroupType.Bonding);
                        ug.setBondingUuid(ctx.createdBondingInv.getUuid());
                        ug.setInterfaceUuid(null);
                    } else {
                        Tuple tuple = Q.New(HostNetworkInterfaceVO.class)
                                .select(HostNetworkInterfaceVO_.interfaceName, HostNetworkInterfaceVO_.uuid)
                                .eq(HostNetworkInterfaceVO_.uuid, msg.getSlaveUuids().get(0))
                                .findTuple();

                        if (tuple == null) {
                            trigger.fail(operr("interface[uuid:%s] is not found on host[uuid:%s] for virtual switch[uuid:%s]",
                                    msg.getSlaveUuids().get(0), msg.getHostUuid(), msg.getUuid()));
                            return;
                        }

                        ug.setInterfaceName((String) tuple.get(0));
                        ug.setType(UplinkGroupType.PhysicalInterface);
                        ug.setInterfaceUuid((String) tuple.get(1));
                        ug.setBondingUuid(null);
                    }
                    dbf.updateAndRefresh(ug);
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (ctx.oldInterfaceName != null) {
                        ug.setInterfaceName(ctx.oldInterfaceName);
                        if (fromBonding) {
                            ug.setType(UplinkGroupType.Bonding);
                            ug.setBondingUuid(ctx.oldInterfaceUuid);
                            ug.setInterfaceUuid(null);
                        } else {
                            ug.setType(UplinkGroupType.PhysicalInterface);
                            ug.setInterfaceUuid(ctx.oldInterfaceUuid);
                            ug.setBondingUuid(null);
                        }
                        dbf.updateAndRefresh(ug);
                    }
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "apply-to-backend";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    L2NetworkInventory fakeInv = new L2NetworkInventory();
                    fakeInv.setPhysicalInterface(ctx.oldInterfaceName);
                    L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());
                    L2ProviderType providerType = L2ProviderType.valueOf(getL2ProviderTypeByHostUuid(msg.getHostUuid()));

                    L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, providerType);
                    ext.update(fakeInv, getSelfInventory(), msg.getHostUuid(), new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                String __name__ = "remove-bonding-from-db";

                @Override
                public boolean skip(Map data) {
                    return !fromBonding;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    if (bondingUuid == null) {
                        logger.warn(String.format("The uplink bonding[%s] is not found on host[uuid:%s] for virtual switch[uuid:%s]",
                                ug.getInterfaceName(), msg.getHostUuid(), msg.getUuid()));
                        trigger.next();
                        return;
                    }

                    BondingDeletionMsg dmsg = new BondingDeletionMsg();
                    dmsg.setBondingUuid(bondingUuid);
                    dmsg.setDbOnly(true);

                    bus.makeTargetServiceIdByResourceUuid(dmsg, HostNetworkBondingConstant.SERVICE_ID, bondingUuid);
                    bus.send(dmsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("delete bonding[uuid:%s] in database failed: %s", bondingUuid, reply.getError().toString()));
                            }
                            trigger.next();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "update-new-interface-type-in-db";

                @Override
                public boolean skip(Map data) {
                    return toBonding;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    HostNetworkInterfaceVO newInterface = Q.New(HostNetworkInterfaceVO.class)
                            .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                            .eq(HostNetworkInterfaceVO_.interfaceName, ug.getInterfaceName())
                            .find();

                    if (VirtualSwitchUtils.hasNoVlanPortGroup(self.getUuid())) {
                        newInterface.setInterfaceType(NetworkInterfaceType.bridgeSlave.toString());
                    }
                    newInterface.setSlaveActive(Boolean.TRUE);
                    dbf.updateAndRefresh(newInterface);
                    trigger.next();
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(ug.toInventory());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void deleteL2Network(Boolean forceDelete, Completion completion) {
        List<HostNetworkBondingVO> bondingVOs = VirtualSwitchUtils.getUplinkBondingOfvSwitch(self.getUuid());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-virtual-switch-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
                    String __name__ = "delete-port-group";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> uuids = Q.New(L2PortGroupNetworkVO.class)
                                .select(L2PortGroupNetworkVO_.uuid).eq(L2PortGroupNetworkVO_.vSwitchUuid, self.getUuid()).listValues();

                        new While<>(uuids).step((uuid, wcomp) -> {
                            DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
                            dmsg.setUuid(uuid);
                            dmsg.setForceDelete(forceDelete);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                            bus.send(dmsg, new CloudBusCallBack(wcomp) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(reply.getError().toString());
                                    }
                                    wcomp.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                }).then(deleteVirtualSwitchBondingFlow(bondingVOs))
                .done(new FlowDoneHandler(completion) {
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

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();

        deleteL2Network(msg.isForceDelete(), new Completion(msg) {
            @Override
            public void success() {
                dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
                extpEmitter.afterDelete(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
                extpEmitter.afterDelete(inv);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void deleteL2Bridge(List<String> clusterUuids, Completion completion) {
        completion.success();
    }

    @Override
    protected String getInterfaceNameOfHost(String hostUuid) {
        return VirtualSwitchUtils.getInterfaceNameOfvSwitchOnHost(getSelfInventory(), hostUuid);
    }

    @Override
    protected List<HostInventory> getAttachableHostsInCluster(String clusterUuid, List<HostParam> hostParams) {
        List<HostInventory> oldHosts = super.getAttachableHostsInCluster(clusterUuid, hostParams);
        if (oldHosts.isEmpty()) {
            return oldHosts;
        }

        List<HostInventory> ret = new ArrayList<>();
        for (HostInventory host : oldHosts) {
            HostParam hostParam = hostParams.stream().filter(it -> host.getUuid().equals(it.getHostUuid())).findFirst().orElse(null);
            if (hostParam != null && VirtualSwitchUtils.isPhysicalInterfaceValid(hostParam.getPhysicalInterface(), hostParam.getHostUuid())) {
                if (PciDeviceUtils.checkIfPciDevicePassThroughStateIsEnabled(hostParam.getPhysicalInterface(), hostParam.getHostUuid())) {
                    logger.debug(String.format("pass-through state of uplink[%s] on host[uuid:%s, name:%s] is [Enabled], skip attaching",
                            hostParam.getPhysicalInterface(), host.getUuid(), host.getName()));
                    continue;
                }
                ret.add(host);
                continue;
            }

            if (hostParam != null) {
                logger.debug(String.format("there's no valid uplink[%s] on host[uuid:%s, name:%s]",
                        hostParam.getPhysicalInterface(), host.getUuid(), host.getName()));
                continue;
            }

            if (VirtualSwitchUtils.isUpLinkBondingExist(getSelfInventory().getUuid(), host.getUuid(),
                    getSelfInventory().getPhysicalInterface())) {
                ret.add(host);
                continue;
            }

            logger.debug(String.format("host[uuid:%s, name:%s] do not have uplink bonding[%s] configured for the virtual switch",
                    host.getUuid(), host.getName(), getSelfInventory().getPhysicalInterface()));
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeAttachL2NetworkToCluster(AttachL2NetworkToClusterMsg msg, List<HostInventory> hosts) {
        List<String> attachableHosts = hosts.stream().map(HostInventory::getUuid).collect(Collectors.toList());
        List<HostParam> hostParams = new ArrayList<>();
        if (!StringUtils.isEmpty(msg.getHostParams())) {
            hostParams.addAll(JSONObjectUtil.toCollection(msg.getHostParams(), ArrayList.class, HostParam.class));
        }
        virtualSwitchHelper.initUplinkGroup(getSelf(), attachableHosts, msg.getL2ProviderType(), hostParams);
    }

    @Override
    protected void afterAttachL2NetworkToCluster(AttachL2NetworkToClusterMsg msg, List<HostInventory> hosts) {
        List<String> attachableHosts = hosts.stream().map(HostInventory::getUuid).collect(Collectors.toList());
        List<UplinkGroupVO> ugs = VirtualSwitchUtils.getUplinkGroups(msg.getL2NetworkUuid(), attachableHosts, UplinkGroupType.PhysicalInterface);
        for (UplinkGroupVO ug : ugs) {
            PciDeviceUtils.updatePciDevicePassThroughStateFromAvailableToDisabled(ug.getInterfaceName(), ug.getHostUuid());
        }
    }

    @Override
    protected void afterAttachL2NetworkToClusterFailed(AttachL2NetworkToClusterMsg msg) {
        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
        VirtualSwitchUtils.deleteUplinkGroup(msg.getL2NetworkUuid(), hostUuids);
    }

    @Override
    protected void afterDetachL2NetworkFromCluster(DetachL2NetworkFromClusterMsg msg) {
        SQL.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .delete();

        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
        List<UplinkGroupVO> ugs = VirtualSwitchUtils.getUplinkGroups(msg.getL2NetworkUuid(), hostUuids, UplinkGroupType.PhysicalInterface);
        for (UplinkGroupVO ug : ugs) {
            PciDeviceUtils.updatePciDevicePassThroughStateFromDisabledToAvailable(ug.getInterfaceName(), ug.getHostUuid());
        }
        VirtualSwitchUtils.deleteUplinkGroup(msg.getL2NetworkUuid(), hostUuids);
    }

    @Override
    protected void afterDetachL2NetworkFromHost(final DetachL2NetworkFromHostMsg msg) {
        UplinkGroupVO ug = VirtualSwitchUtils.getUplinkGroup(msg.getL2NetworkUuid(), msg.getHostUuid(), UplinkGroupType.PhysicalInterface);
        if (ug != null) {
            PciDeviceUtils.updatePciDevicePassThroughStateFromDisabledToAvailable(ug.getInterfaceName(), ug.getHostUuid());
        }

        VirtualSwitchUtils.deleteUplinkGroup(msg.getL2NetworkUuid(), msg.getHostUuid());
    }

    @Override
    protected void beforeAttachL2NetworkToHost(AttachL2NetworkToHostMsg msg) {
        HostParam hostParam = new HostParam();
        if (!StringUtils.isEmpty(msg.getHostParam())) {
            hostParam = JSONObjectUtil.toObject(msg.getHostParam(), HostParam.class);
        }
        virtualSwitchHelper.initUplinkGroup(getSelf(), msg.getHostUuid(), msg.getL2ProviderType(), hostParam);
    }

    @Override
    protected void afterAttachL2NetworkToHost(AttachL2NetworkToHostMsg msg) {
        UplinkGroupVO ug = VirtualSwitchUtils.getUplinkGroup(msg.getL2NetworkUuid(), msg.getHostUuid(), UplinkGroupType.PhysicalInterface);
        if (ug != null) {
            PciDeviceUtils.updatePciDevicePassThroughStateFromAvailableToDisabled(ug.getInterfaceName(), msg.getHostUuid());
        }
    }

    @Override
    protected void afterAttachL2NetworkToHostFailed(AttachL2NetworkToHostMsg msg) {
        VirtualSwitchUtils.deleteUplinkGroup(msg.getL2NetworkUuid(), msg.getHostUuid());
    }
}