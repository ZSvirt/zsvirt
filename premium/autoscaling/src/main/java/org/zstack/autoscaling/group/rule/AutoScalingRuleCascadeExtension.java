package org.zstack.autoscaling.group.rule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleVmInstanceAlarmManager;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Create by lining at 2018/10/10
 */
public class AutoScalingRuleCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AutoScalingRuleCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AutoScalingRuleVmInstanceAlarmManager manager;

    private static final String NAME = AutoScalingRuleVO.class.getSimpleName();

    @Override
    public void syncCascade(CascadeAction action) {

    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<AutoScalingRuleInventory> ruleInventories = ruleFromAction(action);
        if (ruleInventories == null || ruleInventories.isEmpty()) {
            completion.success();
            return;
        }

        List<DeleteAutoScalingRuleMsg> msgs = new ArrayList<>();

        for (AutoScalingRuleInventory ruleInventory : ruleInventories) {
            DeleteAutoScalingRuleMsg msg = new DeleteAutoScalingRuleMsg();
            msg.setAutoScalingGroupUuid(ruleInventory.getScalingGroupUuid());
            msg.setRuleUuid(ruleInventory.getUuid());
            bus.makeLocalServiceId(msg, AutoScalingConstants.SERVICE_ID);
            msgs.add(msg);
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-AutoScalingRule-resources");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-autoscaling-rule";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<ErrorCode> errorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                        new While<>(msgs).step((deleteMsg, whileCompletion) -> {
                            bus.send(deleteMsg, new CloudBusCallBack(whileCompletion) {
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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(AutoScalingGroupVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<AutoScalingRuleInventory> ruleFromAction(CascadeAction action) {
        List<AutoScalingRuleInventory> ret = null;
        if (AutoScalingGroupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AutoScalingGroupInventory> groups = action.getParentIssuerContext();
            List<String> groupUuids = groups.stream().map(AutoScalingGroupInventory::getUuid).collect(Collectors.toList());

            List<AutoScalingRuleVO> ruleVOS = Q.New(AutoScalingRuleVO.class)
                    .in(AutoScalingRuleVO_.scalingGroupUuid, groupUuids)
                    .list();
            if (!ruleVOS.isEmpty()) {
                ret = AutoScalingRuleInventory.valueOf1(ruleVOS);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AutoScalingRuleInventory> ctx = ruleFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
