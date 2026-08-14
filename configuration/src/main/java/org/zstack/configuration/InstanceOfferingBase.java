package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.workflow.*;
import org.zstack.header.configuration.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:09 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class InstanceOfferingBase implements InstanceOffering {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;

    protected InstanceOfferingVO self;

    public InstanceOfferingBase(InstanceOfferingVO vo) {
        self = vo;
    }

    protected InstanceOfferingInventory getInventory() {
        return InstanceOfferingInventory.valueOf(self);
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage)msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg ,e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    @Override
    public void deleteHook() {
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof InstanceOfferingDeletionMsg) {
            handle((InstanceOfferingDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(InstanceOfferingDeletionMsg msg) {
        deleteHook();
        InstanceOfferingDeletionReply reply = new InstanceOfferingDeletionReply();
        dbf.removeByPrimaryKey(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeInstanceOfferingStateMsg) {
            handle((APIChangeInstanceOfferingStateMsg)msg);
        } else if (msg instanceof APIDeleteInstanceOfferingMsg) {
            handle((APIDeleteInstanceOfferingMsg) msg);
        } else if (msg instanceof APIUpdateInstanceOfferingMsg) {
            handle((APIUpdateInstanceOfferingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected InstanceOfferingVO updateInstanceOffering(APIUpdateInstanceOfferingMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getAllocatorStrategy() != null) {
            self.setAllocatorStrategy(msg.getAllocatorStrategy());
            update = true;
        }

        return update ? self : null;
    }

    private void handle(APIUpdateInstanceOfferingMsg msg) {
        InstanceOfferingVO vo = updateInstanceOffering(msg);
        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }

        APIUpdateInstanceOfferingEvent evt = new APIUpdateInstanceOfferingEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    private void handle(APIDeleteInstanceOfferingMsg msg) {
        final APIDeleteInstanceOfferingEvent evt = new APIDeleteInstanceOfferingEvent(msg.getId());
        final String issuer = InstanceOfferingVO.class.getSimpleName();
        final List<InstanceOfferingInventory> ctx = InstanceOfferingInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-instance-offering-%s", msg.getUuid()));
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
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIChangeInstanceOfferingStateMsg msg) {
        InstanceOfferingStateEvent sevt = InstanceOfferingStateEvent.valueOf(msg.getStateEvent());
        if (sevt == InstanceOfferingStateEvent.disable) {
            self.setState(InstanceOfferingState.Disabled);
        } else {
            self.setState(InstanceOfferingState.Enabled);
        }

        self = dbf.updateAndRefresh(self);
        APIChangeInstanceOfferingStateEvent evt = new APIChangeInstanceOfferingStateEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }
}
