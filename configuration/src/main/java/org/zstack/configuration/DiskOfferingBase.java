package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.configuration.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DiskOfferingBase implements DiskOffering {
    private static final CLogger logger = Utils.getLogger(DiskOfferingBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private ErrorFacade errf;

    protected DiskOfferingVO self;

    DiskOfferingBase(DiskOfferingVO self) {
        this.self = self;
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
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg ,e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DiskOfferingDeletionMsg) {
            handle((DiskOfferingDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DiskOfferingDeletionMsg msg) {
        DiskOfferingDeletionReply reply = new DiskOfferingDeletionReply();
        dbf.remove(self);
        logger.debug(String.format("deleted disk offering [uuid:%s]", self.getUuid()));
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeDiskOfferingStateMsg) {
            handle((APIChangeDiskOfferingStateMsg)msg);
        } else if (msg instanceof APIDeleteDiskOfferingMsg) {
            handle((APIDeleteDiskOfferingMsg) msg);
        } else if (msg instanceof APIUpdateDiskOfferingMsg) {
            handle((APIUpdateDiskOfferingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateDiskOfferingMsg msg) {
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
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateDiskOfferingEvent evt = new APIUpdateDiskOfferingEvent(msg.getId());
        evt.setInventory(DiskOfferingInventory.valueOf(self));
        bus.publish(evt);
    }

    private void handle(APIChangeDiskOfferingStateMsg msg) {
        DiskOfferingStateEvent sevt = DiskOfferingStateEvent.valueOf(msg.getStateEvent());
        if (sevt == DiskOfferingStateEvent.disable) {
            self.setState(DiskOfferingState.Disabled);
        } else {
            self.setState(DiskOfferingState.Enabled);
        }

        self = dbf.updateAndRefresh(self);
        DiskOfferingInventory inv = DiskOfferingInventory.valueOf(self);

        APIChangeDiskOfferingStateEvent evt = new APIChangeDiskOfferingStateEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(APIDeleteDiskOfferingMsg msg) {
        final APIDeleteDiskOfferingEvent evt = new APIDeleteDiskOfferingEvent(msg.getId());
        final String issuer = DiskOfferingVO.class.getSimpleName();
        final List<DiskOfferingInventory> ctx = DiskOfferingInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-disk-offering-%s", msg.getUuid()));
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
}
