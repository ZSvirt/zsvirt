package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.SchedulerJobGroupJobRefVO;
import org.zstack.header.scheduler.SchedulerJobGroupJobRefVO_;
import org.zstack.header.scheduler.SchedulerJobSchedulerTriggerRefVO;
import org.zstack.header.scheduler.SchedulerJobSchedulerTriggerRefVO_;
import org.zstack.scheduler.DeleteSchedulerJobGroupMsg;
import org.zstack.scheduler.DeleteSchedulerJobMsg;
import org.zstack.scheduler.DeleteSchedulerTriggerMsg;
import org.zstack.scheduler.SchedulerConstant;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/18 2:31 PM
 */
public class AutoScalingRuleSchedulerJobTriggerFactory implements AutoScalingRuleTriggerFactory {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public AutoScalingRuleTriggerType getType() {
        return AutoScalingRuleTriggerType.TimedTask;
    }

    @Override
    public String getResourceUuid(String triggerUuid) {
        return dbf.findByUuid(triggerUuid, AutoScalingRuleSchedulerJobTriggerVO.class).getSchedulerJobUuid();
    }

    @Override
    public void cleanResource(String schedulerJobUuid, Completion completion) {

        DeleteSchedulerJobMsg jobMsg = new DeleteSchedulerJobMsg();
        jobMsg.setUuid(schedulerJobUuid);
        bus.makeTargetServiceIdByResourceUuid(jobMsg, SchedulerConstant.SERVICE_ID, schedulerJobUuid);

        List<String> tiggerUuids = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                .select(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, schedulerJobUuid)
                .listValues();

        List<DeleteSchedulerTriggerMsg> deleteSchedulerTriggerMsgs = tiggerUuids.stream().map(uuid -> {
            DeleteSchedulerTriggerMsg triggerMsg = new DeleteSchedulerTriggerMsg();
            triggerMsg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(triggerMsg, SchedulerConstant.SERVICE_ID, uuid);
            return triggerMsg;
        }).collect(Collectors.toList());

        List<String> schedulerJobGroupUuid = Q.New(SchedulerJobGroupJobRefVO.class)
                .select(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, schedulerJobUuid)
                .listValues();

        List<DeleteSchedulerJobGroupMsg>  deleteSchedulerJobGroupMsgs = schedulerJobGroupUuid.stream().map(uuid -> {
            DeleteSchedulerJobGroupMsg msg = new DeleteSchedulerJobGroupMsg();
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, uuid);
            return msg;
        }).collect(Collectors.toList());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-chedulerJob-resources");
        List<DeleteSchedulerTriggerMsg> finalDeleteSchedulerTriggerMsgs = deleteSchedulerTriggerMsgs;
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-schedulerJob-group";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<ErrorCode> errorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                        new While<>(deleteSchedulerJobGroupMsgs).step((deleteSchedulerJobGroupMsg, whileCompletion) -> {
                            bus.send(deleteSchedulerJobGroupMsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        errorCodes.add(reply.getError());
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodes.isEmpty()) {
                                    trigger.next();
                                    return;
                                }
                                trigger.fail(errorCodes.get(0));
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-schedulerJob";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bus.send(jobMsg, new CloudBusCallBack(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-scheduler-trigger";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<ErrorCode> errorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                        new While<>(deleteSchedulerTriggerMsgs).step((deleteSchedulerTriggerMsg, whileCompletion) -> {
                            bus.send(deleteSchedulerTriggerMsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        errorCodes.add(reply.getError());
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(completion) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodes.isEmpty()) {
                                    completion.success();
                                    return;
                                }
                                completion.fail(errorCodes.get(0));
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }
}
