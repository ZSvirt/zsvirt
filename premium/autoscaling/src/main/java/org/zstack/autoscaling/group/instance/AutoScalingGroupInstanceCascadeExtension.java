package org.zstack.autoscaling.group.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityInventory;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityVO;
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
public class AutoScalingGroupInstanceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AutoScalingGroupInstanceCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = AutoScalingGroupInstanceVO.class.getSimpleName();

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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<AutoScalingGroupInstanceInventory> instances = instanceFromAction(action);
        if (instances == null || instances.isEmpty()) {
            completion.success();
            return;
        }

        List<DeleteAutoScalingGroupInstanceMsg> msgs = new ArrayList<>();
        for (AutoScalingGroupInstanceInventory instance : instances) {
            DeleteAutoScalingGroupInstanceMsg msg = new DeleteAutoScalingGroupInstanceMsg();
            msg.setAutoScalingGroupUuid(instance.getScalingGroupUuid());
            msg.setInstanceUuid(instance.getInstanceUuid());
            msg.setForceDelete(true);
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
        return Arrays.asList(AutoScalingGroupVO.class.getSimpleName(), AutoScalingGroupActivityVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<AutoScalingGroupInstanceInventory> instanceFromAction(CascadeAction action) {
        List<AutoScalingGroupInstanceInventory> ret = null;
        if (AutoScalingGroupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AutoScalingGroupInventory> groups = action.getParentIssuerContext();
            List<String> groupUuids = groups.stream().map(AutoScalingGroupInventory::getUuid).collect(Collectors.toList());

            List<AutoScalingGroupInstanceVO> instanceVOS = Q.New(AutoScalingGroupInstanceVO.class)
                    .in(AutoScalingGroupInstanceVO_.scalingGroupUuid, groupUuids)
                    .list();
            if (!instanceVOS.isEmpty()) {
                ret = AutoScalingGroupInstanceInventory.valueOf1(instanceVOS);
            }
        } else if (AutoScalingGroupActivityVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AutoScalingGroupActivityInventory> groups = action.getParentIssuerContext();
            List<String> activityUuids = groups.stream().map(AutoScalingGroupActivityInventory::getUuid).collect(Collectors.toList());

            List<AutoScalingGroupInstanceVO> instanceVOS = Q.New(AutoScalingGroupInstanceVO.class)
                    .in(AutoScalingGroupInstanceVO_.scalingGroupActivityUuid, activityUuids)
                    .list();
            if (!instanceVOS.isEmpty()) {
                ret = AutoScalingGroupInstanceInventory.valueOf1(instanceVOS);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AutoScalingGroupInstanceInventory> ctx = instanceFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
