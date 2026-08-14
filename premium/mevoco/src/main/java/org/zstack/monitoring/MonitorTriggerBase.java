package org.zstack.monitoring;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.monitoring.actions.MonitorTriggerActionFactory;
import org.zstack.monitoring.actions.MonitorTriggerActionState;
import org.zstack.monitoring.actions.MonitorTriggerActionVO;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.trigger.expression.TriggerExpression;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2017/6/10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MonitorTriggerBase implements MonitorTrigger {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private MonitorManager mmgr;
    @Autowired
    protected AccountManager acntMgr;

    private MonitorTriggerVO self;

    protected MonitorTriggerInventory getInventory() {
        return MonitorTriggerInventory.valueOf(self);
    }

    private static Map<String, Constructor> triggerEventConstructors = new HashMap<>();

    static {
        Platform.getReflections().getConstructorsAnnotatedWith(TriggerEventFactoryMethod.class)
                .forEach(c -> {
                    TriggerEventFactoryMethod at = (TriggerEventFactoryMethod) c.getAnnotation(TriggerEventFactoryMethod.class);
                    Constructor cs = triggerEventConstructors.get(at.value());
                    if (cs != null) {
                        throw new CloudRuntimeException(String.format("duplicate constructors[%s, %s] for the event media type[%s]",
                                cs.getClass(), c.getClass(), at.value()));
                    }

                    triggerEventConstructors.put(at.value(), c);
                });
    }


    public MonitorTriggerBase(MonitorTriggerVO self) {
        this.self = self;
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
        if (msg instanceof ChangeMonitorTriggerStatusMsg) {
            handle((ChangeMonitorTriggerStatusMsg) msg);
        } else if (msg instanceof MonitorTriggerDeletionMsg) {
            handle((MonitorTriggerDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(MonitorTriggerDeletionMsg msg) {
        MonitorTriggerDeletionReply reply = new MonitorTriggerDeletionReply();
        deleteTrigger(getInventory(), new Completion(msg) {
            @Override
            public void success() {
                dbf.remove(self);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Transactional
    private void changeStatus(MonitorTriggerStatus status) {
        if (self.getStatus() == status) {
            return;
        }

        // do not use dbf.updateAndRefresh, see issue for more details.
        SQL.New(MonitorTriggerVO.class).set(MonitorTriggerVO_.status, status)
                .set(MonitorTriggerVO_.lastStatusChangeTime, dbf.getCurrentSqlTime())
                .update();
        self = Q.New(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, self.getUuid()).find();
    }

    private void handle(ChangeMonitorTriggerStatusMsg msg) {
        ChangeMonitorTriggerStatusReply reply = new ChangeMonitorTriggerStatusReply();
        if (self.getStatus() == msg.getStatus()) {
            bus.reply(msg, reply);
            return;
        }

        Item item = mmgr.getItemByExpressionString(self.getExpression());

        AlertVO vo = new AlertVO();
        vo.setUuid(Platform.getUuid());
        vo.setTriggerStatus(msg.getStatus());
        vo.setTriggerUuid(self.getUuid());
        vo.setTargetResourceUuid(self.getTargetResourceUuid());
        if (msg.getStatus() == MonitorTriggerStatus.Problem) {
            vo.setContent(item.createAlertTextWriter().writeProblemAlertText(msg.getContext()));
        } else {
            vo.setContent(item.createAlertTextWriter().writeOkAlertText(msg.getContext()));
        }
        vo.setAccountUuid(Account.getAccountUuidOfResource(self.getAccountUuid()));

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                reload(vo);
            }
        }.execute();

        if (self.getState() == MonitorTriggerState.Disabled) {
            changeStatus(msg.getStatus());
            bus.reply(msg, reply);
            return;
        }

        List<String> actionUuids = Q.New(MonitorTriggerActionRefVO.class)
                .select(MonitorTriggerActionRefVO_.actionUuid)
                .eq(MonitorTriggerActionRefVO_.triggerUuid, self.getUuid()).listValues();

        List<TriggerEvent> events = new ArrayList<>();

        if (!actionUuids.isEmpty()) {
            events.addAll(new SQLBatchWithReturn<List<TriggerEvent>>() {
                @Override
                protected List<TriggerEvent> scripts() {
                    List<TriggerEvent> ret = new ArrayList<>();

                    List<MonitorTriggerActionVO> actions = sql("select a from MonitorTriggerActionVO a, MonitorTriggerActionRefVO ref where a.uuid = ref.actionUuid" +
                            " and ref.triggerUuid = :uuid and a.state = :state")
                            .param("uuid", self.getUuid())
                            .param("state", MonitorTriggerActionState.Enabled)
                            .list();

                    for (MonitorTriggerActionVO action : actions) {
                        Constructor cs = triggerEventConstructors.get(action.getType());
                        if (cs == null) {
                            throw new CloudRuntimeException(String.format("cannot find constructors of TriggerEvent for media[%s]", action.getType()));
                        }

                        MonitorTriggerActionFactory f = mmgr.getMonitorActionFactory(action.getType());

                        try {
                            ret.add((TriggerEvent) cs.newInstance(getInventory(), f.getInventory(action.getUuid()), msg.getContext()));
                        } catch (Exception e) {
                            throw new CloudRuntimeException(e);
                        }
                    }

                    return ret;
                }
            }.execute());
        }

        events.add(new NotificationTriggerEvent(getInventory(), null, msg.getContext()));

        new While<>(events).all((event, completion) -> {
            Completion c = new Completion(completion) {
                @Override
                public void success() {
                    completion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.done();
                }
            };

            if (msg.getStatus() == MonitorTriggerStatus.Problem) {
                event.triggerProblem(c);
            } else {
                event.triggerOK(c);
            }
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                changeStatus(msg.getStatus());
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteMonitorTriggerMsg) {
            handle((APIDeleteMonitorTriggerMsg) msg);
        } else if (msg instanceof APIUpdateMonitorTriggerMsg) {
            handle((APIUpdateMonitorTriggerMsg) msg);
        } else if (msg instanceof APIChangeMonitorTriggerStateMsg) {
            handle((APIChangeMonitorTriggerStateMsg) msg);
        } else if (msg instanceof APIAttachMonitorTriggerActionToTriggerMsg) {
            handle((APIAttachMonitorTriggerActionToTriggerMsg) msg);
        } else if (msg instanceof APIDetachMonitorTriggerActionFromTriggerMsg) {
            handle((APIDetachMonitorTriggerActionFromTriggerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDetachMonitorTriggerActionFromTriggerMsg msg) {
        APIDetachMonitorTriggerActionFromTriggerEvent evt = new APIDetachMonitorTriggerActionFromTriggerEvent(msg.getId());

        SQL.New(MonitorTriggerActionRefVO.class).eq(MonitorTriggerActionRefVO_.triggerUuid, self.getUuid())
                .eq(MonitorTriggerActionRefVO_.actionUuid, msg.getActionUuid()).delete();

        bus.publish(evt);
    }

    private void handle(APIAttachMonitorTriggerActionToTriggerMsg msg) {
        APIAttachMonitorTriggerActionToTriggerEvent evt = new APIAttachMonitorTriggerActionToTriggerEvent(msg.getId());
        MonitorTriggerActionRefVO ref = new MonitorTriggerActionRefVO();
        ref.setActionUuid(msg.getActionUuid());
        ref.setTriggerUuid(self.getUuid());
        dbf.persist(ref);
        bus.publish(evt);
    }

    private void handle(APIChangeMonitorTriggerStateMsg msg) {
        MonitorTriggerStateEvent stateEvent = MonitorTriggerStateEvent.valueOf(msg.getStateEvent());

        UpdateQuery sql = SQL.New(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, self.getUuid());
        if (stateEvent == MonitorTriggerStateEvent.enable) {
            sql.set(MonitorTriggerVO_.state, MonitorTriggerState.Enabled).update();
        } else {
            sql.set(MonitorTriggerVO_.state, MonitorTriggerState.Disabled).update();
        }

        self = dbf.reload(self);
        APIChangeMonitorTriggerStateEvent evt = new APIChangeMonitorTriggerStateEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    private void handle(APIUpdateMonitorTriggerMsg msg) {
        MonitorTriggerInventory old = getInventory();
        MonitorTriggerInventory n = getInventory();

        if (msg.getExpression() != null) {
            n.setExpression(msg.getExpression());
        }
        if (msg.getDuration() != null) {
            n.setDuration(msg.getDuration());
        }

        APIUpdateMonitorTriggerEvent evt = new APIUpdateMonitorTriggerEvent(msg.getId());

        boolean recreate = msg.getExpression() != null || msg.getDuration() != null;
        if (recreate) {
            TriggerExpression triggerExpression = TriggerExpression.expressionFromString(n.getExpression());
            Item item = mmgr.getItem(n.getTargetResourceUuid(), triggerExpression);
            item.validateTriggerAndExpression(n, triggerExpression);
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("update-monitor-trigger-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-old-trigger";

                    @Override
                    public boolean skip(Map data) {
                        return !recreate;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteTrigger(old, new Completion(trigger) {
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
                    String __name__ = "create-new-trigger";

                    @Override
                    public boolean skip(Map data) {
                        return !recreate;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        createTrigger(n, new Completion(trigger) {
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
                    String __name__ = "update-trigger-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        UpdateQuery sql = SQL.New(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, self.getUuid());
                        if (msg.getName() != null) {
                            sql.set(MonitorTriggerVO_.name, msg.getName());
                        }

                        if (msg.getDescription() != null) {
                            sql.set(MonitorTriggerVO_.description, msg.getDescription());
                        }

                        if (msg.getExpression() != null) {
                            sql.set(MonitorTriggerVO_.expression, msg.getExpression());
                        }

                        if (msg.getDuration() != null) {
                            sql.set(MonitorTriggerVO_.duration, msg.getDuration());
                        }

                        if (msg.getName() != null || msg.getDescription() != null || msg.getExpression() != null || msg.getDuration() != null) {
                            sql.update();
                            self = dbf.reload(self);
                        }

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(getInventory());
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        SQL.New(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, self.getUuid())
                                .set(MonitorTriggerVO_.status, MonitorTriggerStatus.Error).update();
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void createTrigger(MonitorTriggerInventory inv, Completion completion) {
        TriggerExpression triggerExpression = TriggerExpression.expressionFromString(inv.getExpression());
        Item item = mmgr.getItem(inv.getTargetResourceUuid(), triggerExpression);
        MonitorProviderFactory f = mmgr.getMonitorProviderFactory(item.getProvider());
        MonitorProvider provider = f.createProvider();
        provider.createTrigger(inv, item, triggerExpression, completion);
    }

    private void deleteTrigger(MonitorTriggerInventory inv, Completion completion) {
        TriggerExpression triggerExpression = TriggerExpression.expressionFromString(inv.getExpression());
        Item item = mmgr.getItem(inv.getTargetResourceUuid(), triggerExpression);
        MonitorProviderFactory f = mmgr.getMonitorProviderFactory(item.getProvider());
        MonitorProvider provider = f.createProvider();
        provider.deleteTrigger(inv, item, triggerExpression, completion);
    }

    private void handle(APIDeleteMonitorTriggerMsg msg) {
        APIDeleteMonitorTriggerEvent evt = new APIDeleteMonitorTriggerEvent(msg.getId());
        deleteTrigger(getInventory(), new Completion(msg) {
            @Override
            public void success() {
                dbf.remove(self);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }
}
