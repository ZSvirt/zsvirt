package org.zstack.autoscaling.group.activity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Create by lining at 2018/10/10
 */
public class AutoScalingGroupActivityCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AutoScalingGroupActivityCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = AutoScalingGroupActivityVO.class.getSimpleName();

    @Override
    public void syncCascade(CascadeAction action) throws CascadeException {

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

    private List<AutoScalingGroupActivityInventory> activityFromAction(CascadeAction action) {
        List<AutoScalingGroupActivityInventory> ret = null;
        if (AutoScalingGroupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AutoScalingGroupInventory> groups = action.getParentIssuerContext();
            List<String> groupUuids = groups.stream().map(AutoScalingGroupInventory::getUuid).collect(Collectors.toList());

            List<AutoScalingGroupActivityVO> activityVOS = Q.New(AutoScalingGroupActivityVO.class)
                    .in(AutoScalingGroupActivityVO_.scalingGroupUuid, groupUuids)
                    .list();
            if (!activityVOS.isEmpty()) {
                ret = AutoScalingGroupActivityInventory.valueOf1(activityVOS);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        List<AutoScalingGroupInventory> groups = action.getParentIssuerContext();
        if (groups.isEmpty()){
            completion.success();
            return;
        }
        List<String> groupUuids = groups.stream().map(AutoScalingGroupInventory::getUuid).collect(Collectors.toList());

        List<DeleteAutoScalingGroupActivityMsg> msgs = new ArrayList<>();
        for (String groupUuid : groupUuids) {
            DeleteAutoScalingGroupActivityMsg msg = new DeleteAutoScalingGroupActivityMsg();
            msg.setAutoScalingGroupUuid(groupUuid);
            bus.makeLocalServiceId(msg, AutoScalingConstants.SERVICE_ID);
            msgs.add(msg);
        }

        List<ErrorCode> errorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
        new While<>(msgs).step((deleteMsg, whileCompletion) -> {
            bus.send(deleteMsg, new CloudBusCallBack(completion) {
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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
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

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AutoScalingGroupActivityInventory> ctx = activityFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
