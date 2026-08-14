package org.zstack.zwatch.alarm;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.*;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AccountManager;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.message.AlarmDeletionMsg;
import org.zstack.zwatch.message.AlarmDeletionReply;
import org.zstack.zwatch.migratedb.MysqlAuditAction;
import org.zstack.zwatch.ruleengine.ComparisonOperator;
import org.zstack.zwatch.ruleengine.RuleManager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AlarmBase {
    private AlarmVO self;

    @Autowired
    private AlarmManager alarmMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RuleManager ruleMgr;
    @Autowired
    private AccountManager acntMgr;
    protected String syncThreadName;

    protected AlarmInventory getSelfInventory() {
        return AlarmInventory.valueOf(self);
    }

    public AlarmBase(AlarmVO self) {
        this.self = self;
        this.syncThreadName = "alarm-" + self.getUuid();
    }

    protected void refreshVO() {
        self = dbf.reload(self);
    }

    @MessageSafe
    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ChangeAlarmStatusMsg) {
            handle((ChangeAlarmStatusMsg) msg);
        } else if (msg instanceof ChangeAlarmStateMsg) {
            handle((ChangeAlarmStateMsg) msg);
        } else if (msg instanceof AlarmDeletionMsg) {
            handle((AlarmDeletionMsg) msg);
        } else if (msg instanceof UpdateAlarmLabelMsg) {
            handle((UpdateAlarmLabelMsg) msg);
        } else if (msg instanceof ExecuteAlarmActionsMsg) {
            handle((ExecuteAlarmActionsMsg) msg);
        } else if (msg instanceof UpdateAlarmMsg) {
            handle((UpdateAlarmMsg) msg);
        } else if (msg instanceof LoadAlarmRuleMsg) {
            handle((LoadAlarmRuleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(LoadAlarmRuleMsg msg) {
        LoadAlarmRuleReply reply = new LoadAlarmRuleReply();
        alarmMgr.loadAlarms(msg.getAlarmUuid());
        bus.reply(msg, reply);
    }

    private void handle(UpdateAlarmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                UpdateAlarmReply reply = new UpdateAlarmReply();
                doUpdateAlarm(msg, new ReturnValueCompletion<AlarmInventory>(chain) {
                    @Override
                    public void success(AlarmInventory returnValue) {
                        reply.setInventory(returnValue);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public String getName() {
                return String.format("update-alarm-%s", self.getUuid());
            }
        });
    }

    private void handle(UpdateAlarmLabelMsg msg) {
        UpdateAlarmLabelReply reply = new UpdateAlarmLabelReply();
        AlarmLabelVO labelVO = dbf.findByUuid(msg.getUuid(), AlarmLabelVO.class);

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

        unloadRule();
        alarmMgr.loadAlarms(labelVO.getAlarmUuid());

        bus.reply(msg, reply);
    }

    private void handle(AlarmDeletionMsg msg) {
        delete();
        bus.reply(msg, new AlarmDeletionReply());
    }

    private void handle(ExecuteAlarmActionsMsg msg) {
        long time = System.currentTimeMillis();
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                AlarmInventory inv = getSelfInventory();

                boolean notDoAction = self.getState() == AlarmState.Disabled || (msg.getPreviousStatus() == AlarmStatus.InsufficientData && msg.getCurrentStatus() == AlarmStatus.OK);

                if (!notDoAction) {
                    AlarmAction.TakeAlarmActionParam param = new AlarmAction.TakeAlarmActionParam();
                    param.alarm = inv;
                    param.currentStatus = msg.getCurrentStatus();
                    param.previousStatus = msg.getPreviousStatus();
                    param.currentValue = msg.getCurrentValue();
                    param.identifyLabel = msg.getIdentifyLabel();
                    param.alarmAccountUuid = acntMgr.getOwnerAccountUuidOfResource(inv.getUuid());
                    param.alarmDataUuid = Platform.getUuid();
                    param.timeMillis = time;

                    List<AlarmAction> actions = new ArrayList<>();
                    actions.addAll(self.getActions().stream().map(a -> {
                        AlarmActionFactory f = alarmMgr.getAlarmActionFactory(a.getActionType());
                        return f.createAlarmAction(a.getActionUuid());
                    }).collect(Collectors.toList()));

//                    actions.add(new AuditAction());
                    actions.add(new MysqlAuditAction());

                    actions.forEach(new Consumer<AlarmAction>() {
                        @Override
                        @AsyncThread
                        public void accept(AlarmAction alarmAction) {
                            alarmAction.takeAction(param);
                        }
                    });
                }

                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return String.format("execute-action-of-alarm-%s", self.getUuid());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    private void handle(ChangeAlarmStatusMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                if (msg.getCurrentStatus() != self.getStatus()) {
                    self.setStatus(msg.getCurrentStatus());
                    dbf.update(self);
                }

                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return String.format("change-status-of-alarm-%s", self.getUuid());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteAlarmMsg) {
            handle((APIDeleteAlarmMsg) msg);
        } else if (msg instanceof APIChangeAlarmStateMsg) {
            handle((APIChangeAlarmStateMsg) msg);
        } else if (msg instanceof APIUpdateAlarmMsg) {
            handle((APIUpdateAlarmMsg) msg);
        } else if (msg instanceof APIAddLabelToAlarmMsg) {
            handle((APIAddLabelToAlarmMsg) msg);
        } else if (msg instanceof APIRemoveLabelFromAlarmMsg) {
            handle((APIRemoveLabelFromAlarmMsg) msg);
        } else if (msg instanceof APIAddActionToAlarmMsg) {
            handle((APIAddActionToAlarmMsg) msg);
        } else if (msg instanceof APIUpdateAlarmLabelMsg) {
            handle((APIUpdateAlarmLabelMsg) msg);
        } else if (msg instanceof APIRemoveActionFromAlarmMsg) {
            handle((APIRemoveActionFromAlarmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateAlarmLabelMsg msg) {
        APIUpdateAlarmLabelEvent event = new APIUpdateAlarmLabelEvent(msg.getId());

        UpdateAlarmLabelMsg updateAlarmLabelMsg = new UpdateAlarmLabelMsg();
        updateAlarmLabelMsg.setAlarmUuid(msg.getAlarmUuid());
        updateAlarmLabelMsg.setValue(msg.getValue());
        updateAlarmLabelMsg.setKey(msg.getKey());
        updateAlarmLabelMsg.setUuid(msg.getUuid());
        updateAlarmLabelMsg.setOperator(msg.getOperator());
        bus.makeLocalServiceId(updateAlarmLabelMsg, AlarmConstants.SERVICE_ID);

        bus.send(updateAlarmLabelMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                AlarmLabelVO labelVO = dbf.findByUuid(msg.getUuid(), AlarmLabelVO.class);
                event.setInventory(AlarmLabelInventory.valueOf(labelVO));
                bus.publish(event);
            }
        });
    }

    private void handle(APIRemoveActionFromAlarmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        sql(AlarmActionVO.class).eq(AlarmActionVO_.alarmUuid, self.getUuid())
                                .eq(AlarmActionVO_.actionUuid, msg.getActionUuid()).hardDelete();
                        flush();
                        self = findByUuid(self.getUuid(), AlarmVO.class);
                    }
                }.execute();

                APIRemoveActionFromAlarmEvent evt = new APIRemoveActionFromAlarmEvent(msg.getId());
                evt.setInventory(getSelfInventory());
                bus.publish(evt);

                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIAddActionToAlarmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                AlarmActionVO vo = new AlarmActionVO();
                vo.setAlarmUuid(self.getUuid());
                vo.setActionUuid(msg.getActionUuid());
                vo.setActionType(msg.getActionType());
                persist(vo);
                flush();
                self = findByUuid(self.getUuid(), AlarmVO.class);
            }
        }.execute();

        APIAddActionToAlarmEvent evt = new APIAddActionToAlarmEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIRemoveLabelFromAlarmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AlarmLabelVO.class).eq(AlarmLabelVO_.uuid, msg.getUuid()).hardDelete();
                flush();
                self = findByUuid(self.getUuid(), AlarmVO.class);
            }
        }.execute();

        APIRemoveLabelFromAlarmEvent evt = new APIRemoveLabelFromAlarmEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIAddLabelToAlarmMsg msg) {
        AlarmLabelInventory inv = new SQLBatchWithReturn<AlarmLabelInventory>() {
            @Override
            protected AlarmLabelInventory scripts() {
                AlarmLabelVO vo = new AlarmLabelVO();
                vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                vo.setKey(msg.getKey());
                vo.setOperator(msg.getLabelOperator());
                vo.setValue(msg.getValue());
                vo.setAlarmUuid(self.getUuid());
                persist(vo);
                flush();

                self = findByUuid(self.getUuid(), AlarmVO.class);
                return AlarmLabelInventory.valueOf(vo);
            }
        }.execute();

        APIAddLabelToAlarmEvent evt = new APIAddLabelToAlarmEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void doUpdateAlarm(UpdateAlarmMsg msg, ReturnValueCompletion<AlarmInventory> completion) {
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
        }

        AlarmVO previousVO = new AlarmVO();
        BeanUtils.copyProperties(self, previousVO);
        if (msg.getComparisonOperator() != null) {
            self.setComparisonOperator(ComparisonOperator.valueOf(msg.getComparisonOperator()));
        }
        if (msg.getPeriod() != null) {
            self.setPeriod(msg.getPeriod());
        }
        if (msg.getRepeatInterval() != null) {
            self.setRepeatInterval(msg.getRepeatInterval());
        }
        if (msg.isEnableRecovery() != null) {
            self.setEnableRecovery(msg.isEnableRecovery());
        }
        if (msg.getThreshold() != null) {
            self.setThreshold(msg.getThreshold());
        }
        if (msg.getRepeatCount() != null) {
            self.setRepeatCount(msg.getRepeatCount());
        }
        if (msg.getEmergencyLevel() != null) {
            self.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
        }

        self = dbf.updateAndRefresh(self);

        if (alarmRuleChanged(previousVO, self)) {
            unloadRule();
            alarmMgr.loadAlarms(self.getUuid());
        }

        if (msg.getActions() != null && !msg.getActions().isEmpty()) {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    sql(AlarmActionVO.class)
                            .eq(AlarmActionVO_.alarmUuid, self.getUuid())
                            .hardDelete();

                    msg.getActions().forEach(a -> {
                        AlarmActionVO avo = new AlarmActionVO();
                        avo.setAlarmUuid(self.getUuid());
                        avo.setActionType(a.actionType);
                        avo.setActionUuid(a.actionUuid);
                        persist(avo);
                    });

                    self = findByUuid(self.getUuid(), AlarmVO.class);
                }
            }.execute();
        }

        completion.success(AlarmInventory.valueOf(self));
    }

    private void handle(APIUpdateAlarmMsg msg) {
        APIUpdateAlarmEvent evt = new APIUpdateAlarmEvent(msg.getId());

        UpdateAlarmMsg umsg = new UpdateAlarmMsg();
        umsg.setUuid(msg.getUuid());
        umsg.setComparisonOperator(msg.getComparisonOperator());
        umsg.setDescription(msg.getDescription());
        umsg.setEnableRecovery(msg.isEnableRecovery());
        umsg.setName(msg.getName());
        umsg.setPeriod(msg.getPeriod());
        umsg.setRepeatCount(msg.getRepeatCount());
        umsg.setRepeatInterval(msg.getRepeatInterval());
        umsg.setThreshold(msg.getThreshold());
        umsg.setEmergencyLevel(msg.getEmergencyLevel());
        umsg.setActions(msg.getActions());

        bus.makeTargetServiceIdByResourceUuid(umsg, AlarmConstants.SERVICE_ID, umsg.getUuid());
        bus.send(umsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }

                UpdateAlarmReply uReply = reply.castReply();
                evt.setInventory(uReply.getInventory());
                bus.publish(evt);
            }
        });
    }

    private boolean alarmRuleChanged(AlarmVO previous, AlarmVO current) {
        return previous.getRepeatCount() != current.getRepeatCount()
                || previous.getRepeatInterval() != current.getRepeatInterval()
                || previous.getThreshold() != current.getThreshold()
                || previous.getPeriod() != current.getPeriod()
                || previous.getComparisonOperator() != current.getComparisonOperator()
                || previous.isEnableRecovery() != current.isEnableRecovery();
    }

    private void handle(APIChangeAlarmStateMsg msg) {
        APIChangeAlarmStateEvent event = new APIChangeAlarmStateEvent(msg.getId());

        ChangeAlarmStateMsg changeAlarmStateMsg = new ChangeAlarmStateMsg();
        changeAlarmStateMsg.setUuid(msg.getUuid());
        changeAlarmStateMsg.setStateEvent(msg.getStateEvent());
        bus.makeTargetServiceIdByResourceUuid(changeAlarmStateMsg, AlarmConstants.SERVICE_ID, msg.getAlarmUuid());


        bus.send(changeAlarmStateMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                self = dbf.reload(self);
                event.setInventory(getSelfInventory());
                bus.publish(event);
            }
        });
    }

    private void handle(ChangeAlarmStateMsg msg) {
        ChangeAlarmStateReply reply = new ChangeAlarmStateReply();

        if (msg.getStateEvent() == AlarmStateEvent.enable) {
            self.setState(AlarmState.Enabled);
        } else {
            self.setState(AlarmState.Disabled);

            UpdateQuery.New(AlarmActionVO.class)
                    .eq(AlarmActionVO_.alarmUuid, self.getUuid())
                    .set(AlarmActionVO_.lastOpDate, new Timestamp(System.currentTimeMillis()))
                    .update();
        }

        self = dbf.updateAndRefresh(self);

        bus.reply(msg, reply);
    }

    private void unloadRule() {
        ruleMgr.deleteIf(r -> r instanceof AlarmManagerImpl.ARule && r.getUuid().equals(self.getUuid()));
    }

    protected void delete() {
        unloadRule();
        dbf.remove(self);
    }

    private void handle(APIDeleteAlarmMsg msg) {
        delete();
        APIDeleteAlarmEvent evt = new APIDeleteAlarmEvent(msg.getId());
        bus.publish(evt);
    }
}
