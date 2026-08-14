package org.zstack.snmp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.*;
import org.zstack.header.AbstractService;
import org.zstack.header.core.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.snmp.agent.*;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @Author : jingwang
 * @create 2023/7/14 4:36 PM
 */
public class SnmpManagerImpl extends AbstractService {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private static final CLogger logger = Utils.getLogger(SnmpManagerImpl.class);

    @Override
    public boolean start() {
        startSnmpAgentIfEnable();
        return true;
    }

    private void startSnmpAgentIfEnable() {
        List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.isEmpty()) {
            return;
        }
        SnmpAgentVO vo = lst.get(0);
        if (vo != null && vo.getStatus().equals(SnmpAgentStatus.Enable)) {
            StartSnmpAgentMsg msg = new StartSnmpAgentMsg();
            bus.makeLocalServiceId(msg, SnmpConstants.SERVICE_ID);
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    StartSnmpAgentReply r = reply.castReply();
                    if (reply.isSuccess()) {
                        logger.info(String.format("success start snmp agent[%s] with management node.", r.getInventory().getUuid()));
                    } else {
                        logger.error(String.format("failed to start snmp agent with management node, because %s", r.getError().getDetails()));
                    }
                }
            });
        }
    }

    @Override
    public boolean stop() {
        return true;
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
        if (msg instanceof StartSnmpAgentMsg) {
            handle((StartSnmpAgentMsg) msg);
        } else if (msg instanceof StopSnmpAgentMsg) {
            handle((StopSnmpAgentMsg) msg);
        } else if (msg instanceof UpdateSnmpAgentMsg) {
            handle((UpdateSnmpAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(UpdateSnmpAgentMsg msg) {
        inQueue().name(String.format("update-snmp-agent-%s", msg.getAgentVO().getUuid()))
                .asyncBackup(msg)
                .run(chain -> updateSnmpAgent(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void updateSnmpAgent(UpdateSnmpAgentMsg msg, NoErrorCompletion noErrorCompletion) {
        UpdateSnmpAgentReply reply = new UpdateSnmpAgentReply();
        doUpdateSnmpAgent(new ReturnValueCompletion<SnmpAgentInventory>(msg, noErrorCompletion) {
            @Override
            public void success(SnmpAgentInventory inventory) {
                reply.setInventory(inventory);
                bus.reply(msg,reply);
                noErrorCompletion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void doUpdateSnmpAgent(ReturnValueCompletion<SnmpAgentInventory> completion) {
        SnmpAgentInstance agent = SnmpAgentInstance.getInstance();
        agent.update(completion);
    }

    private void handle(StopSnmpAgentMsg msg) {
        inQueue().name(String.format("stop-snmp-agent-%s", msg.getUuid()))
                .asyncBackup(msg)
                .run(chain -> stopSnmpAgent(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void stopSnmpAgent(StopSnmpAgentMsg msg, NoErrorCompletion noErrorCompletion) {
        StopSnmpAgentReply reply = new StopSnmpAgentReply();
        SnmpAgentInstance agent = SnmpAgentInstance.getInstance();
        agent.stop(new ReturnValueCompletion<SnmpAgentInventory>(msg) {
            @Override
            public void success(SnmpAgentInventory inventory) {
                reply.setInventory(inventory);
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void handle(StartSnmpAgentMsg msg) {
        inQueue().name(String.format("start-snmp-agent-%s", msg.getUuid()))
                .asyncBackup(msg)
                .run(chain -> startSnmpAgent(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void startSnmpAgent(StartSnmpAgentMsg msg, NoErrorCompletion noErrorCompletion) {
        StartSnmpAgentReply reply = new StartSnmpAgentReply();
        SnmpAgentInstance agent = SnmpAgentInstance.getInstance();
        agent.start(new ReturnValueCompletion<SnmpAgentInventory>(msg) {
            @Override
            public void success(SnmpAgentInventory inv) {
                reply.setInventory(inv);
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateSnmpAgentMsg) {
            handle((APICreateSnmpAgentMsg) msg);
        } else if (msg instanceof APIStartSnmpAgentMsg) {
            handle((APIStartSnmpAgentMsg) msg);
        } else if (msg instanceof APIStopSnmpAgentMsg) {
            handle((APIStopSnmpAgentMsg) msg);
        } else if (msg instanceof APIUpdateSnmpAgentMsg) {
            handle((APIUpdateSnmpAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateSnmpAgentMsg msg) {
        final APICreateSnmpAgentEvent event = new APICreateSnmpAgentEvent(msg.getId());
        SnmpAgentVO vo = SnmpAgentUtils.buildSnmpAgentVOFromNewSnmpAgentMessage(msg, Platform.getUuid());
        dbf.persist(vo);

        // send StartSnmpAgentMsg to all mn nodes.
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                List<ManagementNodeVO> mns = Q.New(ManagementNodeVO.class).list();
                List<StartSnmpAgentMsg> startSnmpAgentInnerMessages = mns.stream().map( mn -> {
                    StartSnmpAgentMsg smsg = new StartSnmpAgentMsg();
                    smsg.setUuid(vo.getUuid());
                    bus.makeServiceIdByManagementNodeId(smsg, SnmpConstants.SERVICE_ID, mn.getUuid());
                    return smsg;
                }).collect(Collectors.toList());
                new While<>(startSnmpAgentInnerMessages).each((m, completion) -> {
                    bus.send(m, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                completion.addError(reply.getError());
                                completion.allDone();
                            } else {
                                completion.done();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errs) {
                        if (!errs.getCauses().isEmpty()) {
                            event.setError(errs.getCauses().get(0));
                        }
                        SnmpAgentVO lastVO = dbf.findByUuid(vo.getUuid(), SnmpAgentVO.class);
                        event.setInventory(SnmpAgentInventory.valueOf(lastVO));
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return SnmpConstants.SNMP_AGENT_SIGNATURE;
            }

            @Override
            public String getName() {
                return "chain-task-create-snmp-agent";
            }
        });
    }

    private void handle(APIUpdateSnmpAgentMsg msg) {
        APIUpdateSnmpAgentEvent event = new APIUpdateSnmpAgentEvent(msg.getId());
        SnmpAgentVO originalVO = Q.New(SnmpAgentVO.class)
                .eq(SnmpAgentVO_.uuid, msg.getUuid())
                .find();

        SnmpAgentVO vo = SnmpAgentUtils.buildSnmpAgentVOFromNewSnmpAgentMessage(msg, msg.getUuid());
        vo.setStatus(originalVO.getStatus());
        if (!SnmpAgentInventory.valueOf(originalVO)
                .equals(SnmpAgentInventory.valueOf(vo))) {
            dbf.updateAndRefresh(vo);
        }
        if (originalVO.getStatus() == SnmpAgentStatus.Disable) {
            event.setInventory(SnmpAgentInventory.valueOf(vo));
            bus.publish(event);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                List<SnmpAgentInnerMessage> innerMessages = buildSnmpAgentInnerMessageToAllMgmtNodes(msg);
                new While<>(innerMessages).each((innerMessage, completion) -> {
                    bus.send(innerMessage, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                completion.done();
                            } else {
                                UpdateSnmpAgentReply r = reply.castReply();
                                completion.addError(r.getError());
                                completion.allDone();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        SnmpAgentVO lastVO = dbf.findByUuid(msg.getUuid(), SnmpAgentVO.class);
                        if (errorCodeList.getCauses().isEmpty()) {
                            event.setInventory(SnmpAgentInventory.valueOf(lastVO));
                        } else {
                            event.setInventory(SnmpAgentInventory.valueOf(lastVO));
                            event.setError(errorCodeList.getCauses().get(0));
                        }
                        bus.publish(event);
                    }
                });
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return SnmpConstants.SNMP_AGENT_SIGNATURE;
            }

            @Override
            public String getName() {
                return String.format("chain-task-update-snmp-agent-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIStopSnmpAgentMsg msg) {
        APIStopSnmpAgentEvent event = new APIStopSnmpAgentEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                List<SnmpAgentInnerMessage> innerMessages = buildSnmpAgentInnerMessageToAllMgmtNodes(msg);
                new While<>(innerMessages).each((innerMessage, completion) -> {
                    bus.send(innerMessage, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                completion.addError(reply.getError());
                                completion.allDone();
                            } else {
                                completion.done();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errs) {
                        if (!errs.getCauses().isEmpty()) {
                            event.setError(errs.getCauses().get(0));
                        } else {
                            SnmpAgentVO lastVO = dbf.findByUuid(msg.getUuid(), SnmpAgentVO.class);
                            event.setInventory(SnmpAgentInventory.valueOf(lastVO));
                        }
                        bus.publish(event);
                    }
                });
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return SnmpConstants.SNMP_AGENT_SIGNATURE;
            }

            @Override
            public String getName() {
                return String.format("chain-task-stop-snmp-agent-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIStartSnmpAgentMsg msg) {
        APIStartSnmpAgentEvent event = new APIStartSnmpAgentEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                List<SnmpAgentInnerMessage> innerMessages = buildSnmpAgentInnerMessageToAllMgmtNodes(msg);
                new While<>(innerMessages).each((innerMessage, completion) -> {
                    bus.send(innerMessage, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                completion.addError(reply.getError());
                                completion.allDone();
                            } else {
                                completion.done();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errs) {
                        if (!errs.getCauses().isEmpty()) {
                            event.setError(errs.getCauses().get(0));
                        } else {
                            SnmpAgentVO lastVO = dbf.findByUuid(msg.getUuid(), SnmpAgentVO.class);
                            event.setInventory(SnmpAgentInventory.valueOf(lastVO));
                        }
                        bus.publish(event);
                    }
                });
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return SnmpConstants.SNMP_AGENT_SIGNATURE;
            }

            @Override
            public String getName() {
                return String.format("chain-task-start-snmp-agent-%s", msg.getUuid());
            }
        });
    }

    private List<SnmpAgentInnerMessage> buildSnmpAgentInnerMessageToAllMgmtNodes(SnmpAgentLocalMessageBuilder snmpAgentLocalMessageBuilder) {
        List<ManagementNodeVO> mns = Q.New(ManagementNodeVO.class).list();
        return mns.stream().map(mn -> {
            SnmpAgentInnerMessage message = snmpAgentLocalMessageBuilder.buildLocalMessage();
            bus.makeServiceIdByManagementNodeId(message, SnmpConstants.SERVICE_ID, mn.getUuid());
            return message;
        }).collect(Collectors.toList());
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SnmpConstants.SERVICE_ID);
    }

    private RunInQueue inQueue() {
        return new RunInQueue("SNMP", thdf, getSnmpSyncLevel());
    }

    private int getSnmpSyncLevel() {
        return SnmpGlobalConfig.SNMP_SYNC_LEVEL.value(Integer.class);
    }
}
