package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.zone.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ZoneBase extends AbstractZone {
	protected final static CLogger logger = Utils.getLogger(ZoneBase.class);
	
	protected ZoneVO self;
	
	@Autowired
	protected DatabaseFacade dbf;
	@Autowired
	protected CloudBus bus;
	@Autowired
	protected ZoneExtensionPointEmitter extpEmitter;
	@Autowired
	protected JobQueueFacade jobf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;

	ZoneBase(ZoneVO self) {
		this.self = self;
	}
	
	protected void handle(APIChangeZoneStateMsg msg) {
        APIChangeZoneStateEvent evt = new APIChangeZoneStateEvent(msg.getId());

        ZoneStateEvent stateEvt = ZoneStateEvent.valueOf(msg.getStateEvent());
        try {
            extpEmitter.preChange(self, stateEvt);
        } catch (ZoneException e) {
            evt.setError(err(SysErrors.CHANGE_RESOURCE_STATE_ERROR, e.getMessage()));
            bus.publish(evt);
            return;
        }

        ZoneState formerState = self.getState();
        extpEmitter.beforeChange(self, stateEvt);
        ZoneState next = AbstractZone.getNextState(self.getState(), stateEvt);
        self.setState(next);
        self = dbf.updateAndRefresh(self);
        extpEmitter.afterChange(self, stateEvt, formerState);
        evt.setInventory(ZoneInventory.valueOf(self));
        logger.debug(String.format("Changed state of zone[uuid:%s] from %s to %s by event %s", self.getUuid(), formerState, self.getState(), stateEvt));
        bus.publish(evt);
	}
	
	protected void handleApiMessage(APIMessage msg) {
	    if (msg instanceof APIDeleteZoneMsg) {
	        handle((APIDeleteZoneMsg)msg);
	    } else if (msg instanceof APIChangeZoneStateMsg) {
            handle((APIChangeZoneStateMsg) msg);
        } else if (msg instanceof APIUpdateZoneMsg) {
            handle((APIUpdateZoneMsg) msg);
	    } else  {
	        bus.dealWithUnknownMessage(msg);
	    }
	}

    private void doUpdateZone(APIUpdateZoneMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                boolean update = false;

                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    update = true;
                }
                if (msg.getDefault() != null) {
                    if (msg.getDefault()) {
                        sql(ZoneVO.class)
                                .notEq(ZoneVO_.uuid, self.getUuid())
                                .set(ZoneVO_.isDefault, false)
                                .update();
                    }

                    self.setDefault(msg.getDefault());
                    update = true;
                }

                if (update) {
                    reload(merge(self));
                }
            }
        }.execute();
    }

    private void updateZone(APIUpdateZoneMsg msg, Completion completion) {
        if (msg.getDefault() == null || !msg.getDefault()) {
            doUpdateZone(msg);
            completion.success();
            return;
        }

        // queue default zone operation
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "default-zone-operation-queue";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doUpdateZone(msg);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("update-zone-%s", self.getUuid());
            }
        });
    }

    private void handle(APIUpdateZoneMsg msg) {
        APIUpdateZoneEvent evt = new APIUpdateZoneEvent(msg.getId());
        updateZone(msg, new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(ZoneInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    protected void handle(APIDeleteZoneMsg msg) {
        final APIDeleteZoneEvent evt = new APIDeleteZoneEvent(msg.getId());
        final String issuer = ZoneVO.class.getSimpleName();
        ZoneInventory zinv = ZoneInventory.valueOf(self);
        final List<ZoneInventory> ctx = Arrays.asList(zinv);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-zone-%s", msg.getUuid()));
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
			bus.replyErrorByMessageType(msg, e);
		}
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ZoneDeletionMsg) {
            handle((ZoneDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ZoneDeletionMsg msg) {
        ZoneInventory inv = ZoneInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        ZoneDeletionReply reply = new ZoneDeletionReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void deleteHook() {
    }
}
