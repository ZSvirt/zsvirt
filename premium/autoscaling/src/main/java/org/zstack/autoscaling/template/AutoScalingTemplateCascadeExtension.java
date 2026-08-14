package org.zstack.autoscaling.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.rule.AutoScalingRuleInventory;
import org.zstack.autoscaling.group.rule.AutoScalingRuleVO;
import org.zstack.autoscaling.group.rule.AutoScalingRuleVO_;
import org.zstack.autoscaling.group.rule.DeleteAutoScalingRuleMsg;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
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
import java.util.stream.Collectors;

/**
 * Create by lining at 2018/10/10
 */
public class AutoScalingTemplateCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AutoScalingTemplateCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = AutoScalingTemplateVO.class.getSimpleName();

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(AutoScalingGroupVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

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

    private List<AutoScalingTemplateInventory> ruleFromAction(CascadeAction action) {
        List<AutoScalingTemplateInventory> ret = null;
        if (AutoScalingGroupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AutoScalingGroupInventory> groups = action.getParentIssuerContext();
            List<String> groupUuids = groups.stream().map(AutoScalingGroupInventory::getUuid).collect(Collectors.toList());

            List<AutoScalingTemplateVO> vos = SQL.New("select vo from AutoScalingTemplateVO vo, AutoScalingTemplateGroupRefVO refVO " +
                    "where vo.uuid = refVO.templateUuid and refVO.groupUuid in (:groupUuid)").param("groupUuid", groupUuids).list();

            if (!vos.isEmpty()) {
                ret = AutoScalingTemplateInventory.valueOf1(vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        List<AutoScalingGroupInventory> groups = action.getParentIssuerContext();
        if (groups.isEmpty()) {
            completion.success();
            return;
        }

        List<String> groupUuids = groups.stream().map(AutoScalingGroupInventory::getUuid).collect(Collectors.toList());

        List<AutoScalingTemplateGroupRefVO> refVOS = Q.New(AutoScalingTemplateGroupRefVO.class)
                .in(AutoScalingTemplateGroupRefVO_.groupUuid, groupUuids)
                .list();

        List<DeleteAutoScalingTemplateMsg> msgs = new ArrayList<>();

        for (AutoScalingTemplateGroupRefVO refVO : refVOS) {
            DeleteAutoScalingTemplateMsg deleteMsg = new DeleteAutoScalingTemplateMsg();
            deleteMsg.setTemplateUuid(refVO.getTemplateUuid());
            bus.makeLocalServiceId(deleteMsg, AutoScalingConstants.SERVICE_ID);
            msgs.add(deleteMsg);
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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AutoScalingTemplateInventory> ctx = ruleFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
