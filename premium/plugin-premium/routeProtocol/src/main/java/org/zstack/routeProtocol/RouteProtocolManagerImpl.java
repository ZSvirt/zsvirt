package org.zstack.routeProtocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.protocol.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.ha.VpcHaGroupConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.vpc.VpcSystemTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RouteProtocolManagerImpl extends AbstractService implements RouteProtocolManager, Component {
    private static final CLogger logger = Utils.getLogger(RouteProtocolManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected RESTFacade restf;
    /*to be done, it can be gotten from factory with type ospf*/
    @Autowired
    private RouteProtocolOspfBackend ospfBackend;
    @Autowired
    private RouterProtocolConfigProxy proxy;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof AreaMessage) {
            passThough((AreaMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThough(AreaMessage msg) {
        RouterAreaVO area = dbf.findByUuid(msg.getAreaUuid(), RouterAreaVO.class);
        if ( area == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find the router area[uuid:%s], it may have been deleted", msg.getAreaUuid())
            ));
        }

        RouterAreaBase base = new RouterAreaBase(area);
        base.handleMessage((Message) msg);
    }

    private void handle(RefreshRouterProtocolMsg msg) {
        RefreshRouterProtocolReply reply = new RefreshRouterProtocolReply();
        List<String> virtualRouterUuids = msg.getvRouterUuids();
        virtualRouterUuids = virtualRouterUuids.stream().distinct().collect(Collectors.toList());

        if (virtualRouterUuids.isEmpty() ) {
            bus.reply(msg, reply);
            return;
        }

        ErrorCodeList errorCodes = new ErrorCodeList();
        new While<>(virtualRouterUuids).all((uuid,  whileCompletion) -> ospfBackend.applyOspf(uuid, true, new Completion(whileCompletion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully applied ospf on virtual router[uuid:%s]", uuid));
                whileCompletion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to applied ospf on virtual router[uuid:%s]", uuid));
                errorCodes.getCauses().add(errorCode);
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodes.getCauses().isEmpty()) {
                    logger.error(String.format("failed to applied ospf on virtual router: %d", errorCodes.getCauses().size()));
                    reply.setError(errorCodes.getCauses().get(0));
                }
                bus.reply(msg, reply);
            }
        });

    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof RefreshRouterProtocolMsg) {
            handle((RefreshRouterProtocolMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleAPIMessage(APIMessage msg) {

        if (msg instanceof APICreateVRouterOspfAreaMsg) {
            handle((APICreateVRouterOspfAreaMsg) msg);
        } else if (msg instanceof APISetVRouterRouterIdMsg) {
            handle((APISetVRouterRouterIdMsg) msg);
        } else if (msg instanceof APIGetVRouterRouterIdMsg) {
            handle((APIGetVRouterRouterIdMsg) msg);
        } else if (msg instanceof APIAddVRouterNetworksToOspfAreaMsg) {
            handle((APIAddVRouterNetworksToOspfAreaMsg) msg);
        } else if (msg instanceof APIRemoveVRouterNetworksFromOspfAreaMsg) {
            handle((APIRemoveVRouterNetworksFromOspfAreaMsg) msg);
        } else if (msg instanceof APIGetVRouterOspfNeighborMsg) {
            handle((APIGetVRouterOspfNeighborMsg) msg);
        } else if (msg instanceof  APIGetVpcAttachedOspfMsg){
            handle((APIGetVpcAttachedOspfMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(RouteProtocolConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void handle(APICreateVRouterOspfAreaMsg msg) {
        RouterAreaVO vo = new RouterAreaVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setAreaId(msg.getAreaId());
        vo.setType(msg.getAreaType() == null? RouterAreaType.Standard: RouterAreaType.valueOf(msg.getAreaType()));
        vo.setAuthentication(msg.getAreaAuth() == null? RouterAreaAuthType.None:RouterAreaAuthType.valueOf(msg.getAreaAuth()));
        vo.setPassword(msg.getPassword());
        vo.setKeyId(msg.getKeyId());
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                reload(vo);
            }
        }.execute();

        APICreateVRouterOspfAreaEvent evt = new APICreateVRouterOspfAreaEvent(msg.getId());
        evt.setInventory(RouterAreaInventory.valueOf(vo));
        bus.publish(evt);
    }

    /*
    1. remove all the l3networks that have existed in the ref table.
    2. add the l3networks into db
     */
    private List<NetworkRouterAreaRefVO> addNetworksToRouterArea(final APIAddVRouterNetworksToOspfAreaMsg msg) {
        logger.debug(String.format("Start add networks on vRouter: %s to area: %s", msg.getvRouterUuid(), msg.getRouterAreaUuid()));
        List<NetworkRouterAreaRefVO> vos = new ArrayList<>();
        List<String> addL3networks = msg.getL3NetworkUuids();

        String vrouterUuid = msg.getvRouterUuid();
        VpcRouterVmVO vpcVo = dbf.findByUuid(msg.getvRouterUuid(), VpcRouterVmVO.class);
        if (vpcVo.isHaEnabled()) {
            vrouterUuid = proxy.getHaUuidOfVpcRouter(msg.getvRouterUuid());
        }

        List<String> originalL3networks = Q.New(NetworkRouterAreaRefVO.class).select(NetworkRouterAreaRefVO_.l3NetworkUuid)
                .eq(NetworkRouterAreaRefVO_.vRouterUuid, vrouterUuid)
                .eq(NetworkRouterAreaRefVO_.routerAreaUuid, msg.getRouterAreaUuid()).listValues();
        addL3networks.removeAll(originalL3networks);

        if (addL3networks.isEmpty()) {
            return vos;
        }

        for (String uuid: addL3networks) {
            NetworkRouterAreaRefVO vo = new NetworkRouterAreaRefVO();
            vo.setUuid(Platform.getUuid());
            vo.setL3NetworkUuid(uuid);
            vo.setRouterAreaUuid(msg.getRouterAreaUuid());
            vo.setvRouterUuid(vrouterUuid);
            if (vpcVo.isHaEnabled()) {
                vo.setApplianceVmType(VpcHaGroupConstants.VPCHA_GROUP_VROUTER_VM_TYPE);
            } else {
                vo.setApplianceVmType(VpcConstants.VPC_VROUTER_VM_TYPE);
            }
            vos.add(vo);
        }

        dbf.persistCollection(vos);

        return vos;
    }

    private void setRouterId(final String virtualRouterUuid, final String routerId) {
        logger.debug(String.format("Start set routerId on vRouter: %s, routerId: %s", virtualRouterUuid, routerId));
        proxy.setOspfRouterId(virtualRouterUuid, routerId);
    }

    private void handle(APISetVRouterRouterIdMsg msg) {
        APISetVRouterRouterIdEvent evt = new APISetVRouterRouterIdEvent(msg.getId());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("set-router-%s-routerId", msg.getvRouterUuid()));

        chain.then(new ShareFlow() {
            String routerId = null;
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-router-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        routerId = GetRouterId(msg.getvRouterUuid());
                        if (!msg.getRouterId().equals(routerId)) {
                            setRouterId(msg.getvRouterUuid(), msg.getRouterId());
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (routerId != null) {
                            setRouterId(msg.getvRouterUuid(), routerId);
                        }
                        trigger.rollback();
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (msg.getRouterId().equals(routerId) ) {
                            trigger.next();
                            return;
                        }
                        ospfBackend.applyOspf(msg.getvRouterUuid(), new Completion(trigger) {
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
                        evt.setRouterId(VpcSystemTags.VROUTER_ROUTER_ID
                                .getTokenByResourceUuid(msg.getvRouterUuid(), VpcSystemTags.VROUTER_ROUTER_ID_TOKEN));
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

    private String GetRouterId(String virtualRouterUuid) {
        return proxy.getOspfRouterId(virtualRouterUuid);
    }

    private void handle(APIGetVRouterRouterIdMsg msg) {
        APIGetVRouterRouterIdReply reply = new APIGetVRouterRouterIdReply();
        reply.setRouterId(GetRouterId(msg.getvRouterUuid()));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVRouterOspfNeighborMsg msg) {
        APIGetVRouterOspfNeighborReply reply = new APIGetVRouterOspfNeighborReply();
        ospfBackend.getOspfNeighbor(msg.getvRouterUuid(), new ReturnValueCompletion< List<Neighbor> >(null) {
            @Override
            public void success(List<Neighbor>  returnValue) {
                reply.setNeighbors(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIAddVRouterNetworksToOspfAreaMsg msg) {
        APIAddVRouterNetworksToOspfAreaEvent evt = new APIAddVRouterNetworksToOspfAreaEvent(msg.getId());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-router-%s-network-to-area-%s", msg.getvRouterUuid(), msg.getRouterAreaUuid()));

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-ospfnetworks-to-db";
                    List<NetworkRouterAreaRefVO> vos;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vos = addNetworksToRouterArea(msg);
                        trigger.next();
                    }
                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (vos != null && !vos.isEmpty()) {
                            dbf.removeCollection(vos,NetworkRouterAreaRefVO.class);
                        }
                        trigger.rollback();
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ospfBackend.applyOspf(msg.getvRouterUuid(), new Completion(trigger) {
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
                        List<NetworkRouterAreaRefVO> vos = proxy.getNetworkRouterAreaRef(msg.getL3NetworkUuids(), msg.getvRouterUuid());
                        evt.setInventories(NetworkRouterAreaRefInventory.valueOf(vos));
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

    private void handle(APIRemoveVRouterNetworksFromOspfAreaMsg msg) {
        APIRemoveVRouterNetworksFromOspfAreaEvent evt = new APIRemoveVRouterNetworksFromOspfAreaEvent(msg.getId());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-router-network-%s-from-area", msg.getUuids()));

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "remove-ospfnetwork-from-db";
                    List<NetworkRouterAreaRefVO> vos;
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vos = Q.New(NetworkRouterAreaRefVO.class).in(NetworkRouterAreaRefVO_.uuid, msg.getUuids()).list();
                        dbf.removeByPrimaryKeys(msg.getUuids(), NetworkRouterAreaRefVO.class);
                        data.put(APIRemoveVRouterNetworksFromOspfAreaMsg.class.getSimpleName(), vos);
                        trigger.next();
                    }
                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (vos != null && !vos.isEmpty()) {
                            dbf.persistCollection(vos);
                        }
                        trigger.rollback();
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> virtualRouterUuids = new ArrayList<>();
                        List<NetworkRouterAreaRefVO> vos = (List<NetworkRouterAreaRefVO>)
                                data.get(APIRemoveVRouterNetworksFromOspfAreaMsg.class.getSimpleName());
                        for (NetworkRouterAreaRefVO vo : vos) {
                            if (vo.getApplianceVmType().equals(VpcConstants.VPC_VROUTER_VM_TYPE)) {
                                virtualRouterUuids.add(vo.getvRouterUuid());
                            } else {
                                virtualRouterUuids.add(proxy.getMasterVrUuid(vo.getvRouterUuid()));
                            }
                        }
                        if (virtualRouterUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        RefreshRouterProtocolMsg rmsg = new RefreshRouterProtocolMsg();
                        rmsg.setvRouterUuids(virtualRouterUuids);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, RouteProtocolConstants.SERVICE_ID, virtualRouterUuids.get(0));
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
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


    private void handle(APIGetVpcAttachedOspfMsg msg) {
        APIGetVpcAttachedOspfReply reply = new APIGetVpcAttachedOspfReply();
        String vrouterUuid = msg.getVpcRouterUuid();
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrouterUuid, VpcRouterVmVO.class);
        if (vpcVo.isHaEnabled()) {
            vrouterUuid = proxy.getHaUuidOfVpcRouter(vrouterUuid);
        }

        List<NetworkRouterAreaRefVO> networkRouterAreaRefVOs = Q.New(NetworkRouterAreaRefVO.class)
                .eq(NetworkRouterAreaRefVO_.vRouterUuid, vrouterUuid)
                .limit(msg.getLimit()).start(msg.getStart()).list();

        reply.setInventories(NetworkRouterAreaRefInventory.valueOf(networkRouterAreaRefVOs));
        bus.reply(msg, reply);
    }


}
