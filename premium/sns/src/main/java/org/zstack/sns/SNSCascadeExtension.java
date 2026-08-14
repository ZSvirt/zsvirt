package org.zstack.sns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SQLBatchWithReturn;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

public class SNSCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(SNSCascadeExtension.class);

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

    protected static final int OP_NOPE = 0;
    protected static final int OP_DELETION_BY_ACCOUNT = 1;
    protected static final int OP_DELETION_RESOURCE_UNDER_SNS_PLATFORM = 2;

    protected int toDeletionOpCode(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return OP_NOPE;
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION_BY_ACCOUNT;
        }

        if (SNSApplicationPlatformVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION_RESOURCE_UNDER_SNS_PLATFORM;
        }

        return OP_NOPE;
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        int op = toDeletionOpCode(action);

        if (op == OP_DELETION_BY_ACCOUNT) {
            deleteSNSResourceByAccount(action, completion);
        } else if (op == OP_DELETION_RESOURCE_UNDER_SNS_PLATFORM) {
            deleteSNSResourceUnderSNSPlatform(action, completion);
        }
    }

    private void deleteSNSResourceUnderSNSPlatform(CascadeAction action, Completion completion) {
        List<SNSApplicationPlatformInventory> platforms = action.getParentIssuerContext();

        if (platforms.isEmpty()) {
            completion.success();
            return;
        }

        List<SNSApplicationEndpointDeletionMsg> msgs = new SQLBatchWithReturn<List<SNSApplicationEndpointDeletionMsg>>() {
            @Override
            protected List<SNSApplicationEndpointDeletionMsg> scripts() {
                List<String> endpointUuids = q(SNSApplicationEndpointVO.class)
                        .select(SNSApplicationEndpointVO_.uuid)
                        .in(SNSApplicationEndpointVO_.platformUuid, platforms.stream().map(SNSApplicationPlatformInventory::getUuid).collect(Collectors.toList()))
                        .listValues();

                if (endpointUuids.isEmpty()) {
                    return null;
                }

                return endpointUuids.stream().map(euuid -> {
                    SNSApplicationEndpointDeletionMsg msg = new SNSApplicationEndpointDeletionMsg();
                    msg.setUuid(euuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, euuid);
                    return msg;
                }).collect(Collectors.toList());
            }
        }.execute();

        if (msgs == null) {
            completion.success();
            return;
        }

        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to delete sns application endpoint[uuid:%s], %s", msg.getUuid(), reply.getError()));
                }

                com.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private void deleteSNSResourceByAccount(CascadeAction action, Completion completion) {
        List<AccountInventory> accounts = action.getParentIssuerContext();
        List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-sns");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-topic";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> topicUuids = ResourceHelper.findOwnResourceUuidList(SNSTopicVO.class, accountUuids);
                        if (topicUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<SNSTopicDeletionMsg> msgs = topicUuids.stream().map(auuid -> {
                            SNSTopicDeletionMsg msg = new SNSTopicDeletionMsg();
                            msg.setUuid(auuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, auuid);
                            return msg;
                        }).collect(Collectors.toList());

                        if (msgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete sns topic[uuid:%s], %s", msg.getUuid(), reply.getError()));
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
                    String __name__ = "delete-application-endpoints";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> pUuids = ResourceHelper.findOwnResourceUuidList(
                                SNSApplicationEndpointVO.class, accountUuids);
                        if (pUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<SNSApplicationEndpointDeletionMsg> msgs = pUuids.stream().map(auuid -> {
                            SNSApplicationEndpointDeletionMsg msg = new SNSApplicationEndpointDeletionMsg();
                            msg.setUuid(auuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, auuid);
                            return msg;
                        }).collect(Collectors.toList());

                        if (msgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete sns application endpoint[uuid:%s], %s", msg.getUuid(), reply.getError()));
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
                    String __name__ = "delete-application-platform";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> pUuids = ResourceHelper.findOwnResourceUuidList(
                                SNSApplicationPlatformVO.class, accountUuids);
                        if (pUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<SNSApplicationPlatformDeletionMsg> msgs = pUuids.stream().map(auuid -> {
                            SNSApplicationPlatformDeletionMsg msg = new SNSApplicationPlatformDeletionMsg();
                            msg.setUuid(auuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, auuid);
                            return msg;
                        }).collect(Collectors.toList());

                        if (msgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete sns application platform[uuid:%s], %s", msg.getUuid(), reply.getError()));
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
        return SNSApplicationPlatformVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
