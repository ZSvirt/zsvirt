package org.zstack.routeProtocol;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.protocol.*;
import org.zstack.header.vpc.ha.VpcHaGroupConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;


@Configurable(preConstruction=true,autowire=Autowire.BY_TYPE)
public class RouterAreaBase {
    protected static final CLogger logger = Utils.getLogger(RouterAreaBase.class);

    protected RouterAreaVO self;

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected RouterProtocolConfigProxy proxy;

    protected String getThreadSyncSignature() {
        return String.format("RouterArea-%s-%s", self.getAreaId(), self.getUuid());
    }

    protected RouterAreaVO getSelf() {
        return self;
    }

    protected RouterAreaInventory getSelfInventory() {
        return RouterAreaInventory.valueOf(getSelf());
    }

    RouterAreaBase(RouterAreaVO self) {
        this.self = self;
    }

    protected void refresh() {
        RouterAreaVO vo = dbf.reload(self);
        if (vo == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find the router area[areaId:%s, uuid:%s], it may have been deleted",
                            self.getAreaId(), self.getUuid())
            ));
        }

        self = vo;
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateVRouterOspfAreaMsg) {
            handle((APIUpdateVRouterOspfAreaMsg) msg);
        } else if (msg instanceof APIDeleteVRouterOspfAreaMsg) {
            handle((APIDeleteVRouterOspfAreaMsg) msg);
        } else{
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVRouterOspfAreaMsg msg) {
        APIUpdateVRouterOspfAreaEvent evt = new APIUpdateVRouterOspfAreaEvent(msg.getId());
        RouterAreaVO area = dbf.findByUuid(msg.getAreaUuid(), RouterAreaVO.class);
        if (msg.getAreaType() != null) {
            area.setType(RouterAreaType.valueOf(msg.getAreaType()));
        }

        if (msg.getAreaAuth() != null) {
            area.setAuthentication(RouterAreaAuthType.valueOf(msg.getAreaAuth()));
            area.setPassword(msg.getPassword());
            if (RouterAreaAuthType.MD5.toString().equals(msg.getAreaAuth())) {
                area.setKeyId(msg.getKeyId());
            }
        }

        area = dbf.updateAndRefresh(area);
        evt.setInventory(RouterAreaInventory.valueOf(area));
        List<String> vRouterUuids = new ArrayList<>();
        List<NetworkRouterAreaRefVO> refs = Q.New(NetworkRouterAreaRefVO.class)
                .eq(NetworkRouterAreaRefVO_.routerAreaUuid, msg.getUuid()).list();
        for (NetworkRouterAreaRefVO ref : refs) {
            if (ref.getApplianceVmType().equals(VpcHaGroupConstants.VPCHA_GROUP_VROUTER_VM_TYPE)) {
                vRouterUuids.add(proxy.getMasterVrUuid(ref.getvRouterUuid()));
            } else {
                vRouterUuids.add(ref.getvRouterUuid());
            }
        }
        if (vRouterUuids.isEmpty()) {
            bus.publish(evt);
            return;
        }

        RefreshRouterProtocolMsg rmsg = new RefreshRouterProtocolMsg();
        rmsg.setvRouterUuids(vRouterUuids.stream().distinct().collect(Collectors.toList()));
        bus.makeTargetServiceIdByResourceUuid(rmsg, RouteProtocolConstants.SERVICE_ID, vRouterUuids.get(0));
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }

                bus.publish(evt);
            }
        });
    }

    private void handle(APIDeleteVRouterOspfAreaMsg msg) {
        APIDeleteVRouterOspfAreaEvent evt = new APIDeleteVRouterOspfAreaEvent(msg.getId());
        final String issuer = RouterAreaVO.class.getSimpleName();
        final List<RouterAreaInventory> ctx = RouterAreaInventory.valueOf(Arrays.asList(self));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-router-area-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
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
            }).then(new NoRollbackFlow() {
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
            chain.then(new NoRollbackFlow() {
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

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails())
                        .withCause(errCode));
                bus.publish(evt);
            }
        }).start();
    }

}
