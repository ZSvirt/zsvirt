package org.zstack.zwatch.alarm;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.core.db.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.message.EventSubscriptionDeletionMsg;
import org.zstack.zwatch.message.EventSubscriptionDeletionReply;
import org.zstack.zwatch.message.UpdateEventSubscriptionLabelMsg;
import org.zstack.zwatch.message.UpdateEventSubscriptionLabelReply;
import org.zstack.zwatch.namespace.NamespaceEventManager;

import java.util.ArrayList;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class EventSubscriptionBase implements EventSubscription {
    private final static CLogger logger = Utils.getLogger(EventSubscriptionBase.class);

    protected EventSubscriptionVO self;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private NamespaceEventManager eventMgr;
    @Autowired
    private AlarmManager alarmManager;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;


    public EventSubscriptionBase(EventSubscriptionVO self) {
        this.self = self;
    }

    protected EventSubscriptionInventory getSelfInventory() {
        return EventSubscriptionInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof EventSubscriptionDeletionMsg) {
            handle((EventSubscriptionDeletionMsg) msg);
        } else if (msg instanceof ReloadEventSubscriptionMsg) {
            handle((ReloadEventSubscriptionMsg) msg);
        } else if (msg instanceof DeleteEventSubscriptionMsg) {
            handle((DeleteEventSubscriptionMsg) msg);
        } else if (msg instanceof UpdateEventSubscriptionLabelMsg) {
            handle((UpdateEventSubscriptionLabelMsg) msg);
        } else if (msg instanceof UpdateEventSubscriptionMsg){
            handle((UpdateEventSubscriptionMsg) msg);
        }else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ReloadEventSubscriptionMsg msg) {
        if (destinationMaker.isManagedByUs(self.getUuid())) {
            thdf.syncSubmit(new SyncTask<Void>() {
                private final String name = String.format("sync-reload-event-subscription-%s", msg.getSubscriptionUuid());

                @Override
                public String getSyncSignature() {
                    return name;
                }

                @Override
                public int getSyncLevel() {
                    return 1;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Void call() {
                    eventMgr.getEventDatabaseDriver().unsubscribeEvent(self.getUuid());
                    alarmManager.loadEventSubscription(self.getUuid());
                    return null;
                }
            });
        }
        bus.reply(msg, new ReloadEventSubscriptionReply());
    }

    private void handle(DeleteEventSubscriptionMsg msg) {
        if (destinationMaker.isManagedByUs(self.getUuid())) {
            eventMgr.getEventDatabaseDriver().unsubscribeEvent(self.getUuid());
        }
        bus.reply(msg, new DeleteEventSubscriptionReply());
    }

    private void handle(UpdateEventSubscriptionLabelMsg msg) {
        UpdateEventSubscriptionLabelReply reply = new UpdateEventSubscriptionLabelReply();

        EventSubscriptionLabelVO labelVO = dbf.findByUuid(msg.getUuid(), EventSubscriptionLabelVO.class);

        if (msg.getKey() != null) {
            labelVO.setKey(msg.getKey());
        }
        if (msg.getOperator() != null) {
            labelVO.setOperator(Label.Operator.valueOf(msg.getOperator()));
        }
        if (msg.getValue() != null) {
            labelVO.setValue(msg.getValue());
        }

        dbf.updateAndRefresh(labelVO);

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(EventSubscriptionDeletionMsg msg) {
        delete(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                bus.reply(msg, new EventSubscriptionDeletionReply());
            }
        });

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUnsubscribeEventMsg) {
            handle((APIUnsubscribeEventMsg) msg);
        } else if (msg instanceof APIAddActionToEventSubscriptionMsg) {
            handle((APIAddActionToEventSubscriptionMsg) msg);
        } else if (msg instanceof APIRemoveActionFromEventSubscriptionMsg) {
            handle((APIRemoveActionFromEventSubscriptionMsg) msg);
        } else if (msg instanceof APIAddLabelToEventSubscriptionMsg) {
            handle((APIAddLabelToEventSubscriptionMsg) msg);
        } else if (msg instanceof APIRemoveLabelFromEventSubscriptionMsg) {
            handle((APIRemoveLabelFromEventSubscriptionMsg) msg);
        } else if (msg instanceof APIUpdateEventSubscriptionLabelMsg) {
            handle((APIUpdateEventSubscriptionLabelMsg) msg);
        } else if (msg instanceof APIChangeEventSubscriptionStateMsg) {
            handle((APIChangeEventSubscriptionStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeEventSubscriptionStateMsg msg) {
        EventSubscriptionVO vo = self;
        self.setState(msg.getState());
        self = dbf.updateAndRefresh(self);

        logger.info(String.format("event subscription state from %s to %s", vo.getState().toString(), msg.getState()));

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                APIChangeEventSubscriptionStateEvent event = new APIChangeEventSubscriptionStateEvent(msg.getId());
                event.setInventory(EventSubscriptionInventory.valueOf(self));
                bus.publish(event);
            }
        });
    }

    private void handle(APIRemoveLabelFromEventSubscriptionMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(EventSubscriptionLabelVO.class)
                        .eq(EventSubscriptionLabelVO_.uuid, msg.getUuid()).hardDelete();
                self = findByUuid(self.getUuid(), self.getClass());
            }
        }.execute();
        APIRemoveLabelFromEventSubscriptionEvent evt = new APIRemoveLabelFromEventSubscriptionEvent(msg.getId());

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAddLabelToEventSubscriptionMsg msg) {
        EventSubscriptionLabelInventory inv = new SQLBatchWithReturn<EventSubscriptionLabelInventory>() {
            @Override
            protected EventSubscriptionLabelInventory scripts() {
                EventSubscriptionLabelVO vo = new EventSubscriptionLabelVO();
                vo.setUuid(Platform.getUuid());
                vo.setSubscriptionUuid(self.getUuid());
                vo.setKey(msg.getKey());
                vo.setOperator(msg.getLabelOperator());
                vo.setValue(msg.getValue());
                persist(vo);
                self = findByUuid(self.getUuid(), self.getClass());

                return EventSubscriptionLabelInventory.valueOf(vo);
            }
        }.execute();

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                APIAddLabelToEventSubscriptionEvent evt = new APIAddLabelToEventSubscriptionEvent(msg.getId());
                evt.setInventory(inv);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUpdateEventSubscriptionLabelMsg msg) {
        SQL.New(EventSubscriptionLabelVO.class).eq(EventSubscriptionLabelVO_.uuid, msg.getUuid())
                .set(EventSubscriptionLabelVO_.key, msg.getKey())
                .set(EventSubscriptionLabelVO_.operator, msg.getLabelOperator())
                .set(EventSubscriptionLabelVO_.value, msg.getValue())
                .update();

        reload(new NoErrorCompletion() {
            @Override
            public void done() {
                APIAddLabelToEventSubscriptionEvent evt = new APIAddLabelToEventSubscriptionEvent(msg.getId());
                evt.setInventory(EventSubscriptionLabelInventory.valueOf(dbf.findByUuid(msg.getUuid(), EventSubscriptionLabelVO.class)));
                bus.publish(evt);
            }
        });

    }

    private void handle(UpdateEventSubscriptionMsg msg){

        UpdateEventSubscriptionReply reply = new UpdateEventSubscriptionReply();

        if(msg.getName() != null){
            self.setName(msg.getName());
        }

        if (msg.getEmergencyLevel() != null){
            self.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
        }

        EventSubscriptionVO subscriptionVO = new EventSubscriptionVO();
        BeanUtils.copyProperties(self,subscriptionVO);
        self = dbf.updateAndRefresh(self);

        if (msg.getActions()!=null && !msg.getActions().isEmpty()){
            new SQLBatch(){
                @Override
                protected void scripts() {
                    sql(EventSubscriptionActionVO.class)
                            .eq(EventSubscriptionActionVO_.subscriptionUuid,self.getUuid())
                            .hardDelete();
                    msg.getActions().forEach(action -> {
                        EventSubscriptionActionVO subscriptionActionVO = new EventSubscriptionActionVO();
                        subscriptionActionVO.setSubscriptionUuid(self.getUuid());
                        subscriptionActionVO.setActionType(action.actionType);
                        subscriptionActionVO.setActionUuid(action.actionUuid);
                        persist(subscriptionActionVO);
                    });
                    self = findByUuid(self.getUuid(),EventSubscriptionVO.class);
                }
            }.execute();
        }

        dbf.updateAndRefresh(self);

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                bus.reply(msg,reply);
            }
        });
    }

    private void handle(APIRemoveActionFromEventSubscriptionMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(EventSubscriptionActionVO.class).eq(EventSubscriptionActionVO_.actionUuid, msg.getActionUuid())
                        .eq(EventSubscriptionActionVO_.subscriptionUuid, msg.getSubscriptionUuid()).hardDelete();
                self = findByUuid(self.getUuid(), self.getClass());
            }
        }.execute();
        APIRemoveActionFromEventSubscriptionEvent evt = new APIRemoveActionFromEventSubscriptionEvent(msg.getId());

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAddActionToEventSubscriptionMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                EventSubscriptionActionVO avo = new EventSubscriptionActionVO();
                avo.setActionType(msg.getActionType());
                avo.setActionUuid(msg.getActionUuid());
                avo.setSubscriptionUuid(self.getUuid());
                persist(avo);
                self = findByUuid(self.getUuid(), self.getClass());
            }
        }.execute();

        reload(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                APIAddActionToEventSubscriptionEvent evt = new APIAddActionToEventSubscriptionEvent(msg.getId());
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }
        });
    }

    private void reload(NoErrorCompletion completion) {
        List<String> managementNodeUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();
        List<ReloadEventSubscriptionMsg> rmsgs = new ArrayList<>();
        for (String nodeUuid : managementNodeUuids) {
            ReloadEventSubscriptionMsg rmsg = new ReloadEventSubscriptionMsg();
            rmsg.setSubscriptionUuid(self.getUuid());
            rmsg.setManagementNodeUuid(nodeUuid);
            bus.makeServiceIdByManagementNodeId(rmsg, AlarmConstants.SERVICE_ID, nodeUuid);
            rmsgs.add(rmsg);
        }

        new While<>(rmsgs).each((rmsg, c) -> bus.send(rmsg, new CloudBusCallBack(c) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("Failed to reload no node[uuid:%s]", rmsg.getManagementNodeUuid()));
                }

                c.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    protected void delete(NoErrorCompletion completion) {
        List<String> managementNodeUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();
        List<DeleteEventSubscriptionMsg> dmsgs = new ArrayList<>();
        for (String nodeUuid : managementNodeUuids) {
            DeleteEventSubscriptionMsg dmsg = new DeleteEventSubscriptionMsg();
            dmsg.setSubscriptionUuid(self.getUuid());
            dmsg.setManagementNodeUuid(nodeUuid);
            bus.makeServiceIdByManagementNodeId(dmsg, AlarmConstants.SERVICE_ID, nodeUuid);
            dmsgs.add(dmsg);
        }

        new While<>(dmsgs).each((dmsg, c) -> bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("Failed to delete no node[uuid:%s]", dmsg.getManagementNodeUuid()));
                }

                c.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                dbf.remove(self);
                completion.done();
            }
        });
    }

    private void handle(APIUnsubscribeEventMsg msg) {
        APIUnsubscribeEventEvent evt = new APIUnsubscribeEventEvent(msg.getId());

        EventSubscriptionDeletionMsg innerMsg = new EventSubscriptionDeletionMsg();
        innerMsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AlarmConstants.SERVICE_ID, msg.getUuid());

        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }

                bus.publish(evt);
            }
        });
    }
}
