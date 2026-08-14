package org.zstack.zwatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.alarm.AlarmConstants;
import org.zstack.zwatch.alarm.AlarmVO;
import org.zstack.zwatch.alarm.EventSubscriptionVO;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateVO;
import org.zstack.zwatch.message.AlarmDeletionMsg;
import org.zstack.zwatch.message.EventSubscriptionDeletionMsg;
import org.zstack.zwatch.message.SNSTextTemplateDeletionMsg;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZWatchCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ZWatchCascadeExtension.class);

    @Autowired
    private CloudBus bus;

    @Override
    public List<String> getEdgeNames() {
        return Collections.emptyList();
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<AccountInventory> accounts = action.getParentIssuerContext();
        List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-zwatch-resources");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-alarm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> alarmUuids = ResourceHelper.findOwnResourceUuidList(AlarmVO.class, accountUuids);

                        List<AlarmDeletionMsg> msgs;
                        if (alarmUuids.isEmpty()) {
                            msgs = Collections.emptyList();
                        } else {
                            msgs = alarmUuids.stream().map(auuid -> {
                                AlarmDeletionMsg msg = new AlarmDeletionMsg();
                                msg.setUuid(auuid);
                                bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, auuid);
                                return msg;
                            }).collect(Collectors.toList());
                        }

                        if (msgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete alarm[uuid:%s], %s", msg.getUuid(), reply.getError()));
                                }

                                com.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-sns-text-template";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> templateUuids = ResourceHelper.findOwnResourceUuidList(SNSTextTemplateVO.class, accountUuids);
                        List<SNSTextTemplateDeletionMsg> msgs;
                        if (templateUuids.isEmpty()) {
                            msgs = Collections.emptyList();
                        } else {
                            msgs = templateUuids.stream().map(auuid -> {
                                SNSTextTemplateDeletionMsg msg = new SNSTextTemplateDeletionMsg();
                                msg.setUuid(auuid);
                                bus.makeTargetServiceIdByResourceUuid(msg, SNSActionFactory.SERVICE_ID, auuid);
                                return msg;
                            }).collect(Collectors.toList());
                        }

                        if (msgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete sns text template[uuid:%s], %s", msg.getUuid(), reply.getError()));
                                }

                                com.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-event-subscription";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> subscriptionUuids = ResourceHelper.findOwnResourceUuidList(EventSubscriptionVO.class, accountUuids);
                        List<EventSubscriptionDeletionMsg> msgs;
                        if (subscriptionUuids.isEmpty()) {
                            msgs = Collections.emptyList();
                        } else {
                            msgs = subscriptionUuids.stream().map(auuid -> {
                                EventSubscriptionDeletionMsg msg = new EventSubscriptionDeletionMsg();
                                msg.setUuid(auuid);
                                bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, auuid);
                                return msg;
                            }).collect(Collectors.toList());
                        }

                        if (msgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete event subscription[uuid:%s], %s", msg.getUuid(), reply.getError()));
                                }

                                com.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
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

    @Override
    public String getCascadeResourceName() {
        return "ZWatch";
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
