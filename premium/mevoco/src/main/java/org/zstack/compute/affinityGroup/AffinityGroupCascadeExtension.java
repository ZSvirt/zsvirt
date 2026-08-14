package org.zstack.compute.affinityGroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.affinitygroup.AffinityGroupConstants;
import org.zstack.header.affinitygroup.AffinityGroupDeletionMsg;
import org.zstack.header.affinitygroup.AffinityGroupInventory;
import org.zstack.header.affinitygroup.AffinityGroupVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.multiErr;

/**
 * Created by shixin on 2017-11-16.
 */
public class AffinityGroupCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AffinityGroupCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = AffinityGroupVO.class.getSimpleName();

    @Override
    public List<String> getEdgeNames() {
        return Collections.emptyList();
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AffinityGroupInventory> ctx = AffinityGroupFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
            return action;
        }

        return null;
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
        dbf.eoCleanup(AffinityGroupVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<AffinityGroupInventory> agInvs = AffinityGroupFromAction(action);
        if (agInvs == null || agInvs.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(agInvs).all((agInv, compl) -> {
            AffinityGroupDeletionMsg msg = new AffinityGroupDeletionMsg();
            msg.setUuid(agInv.getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(msg, AffinityGroupConstants.SERVICE_ID, agInv.getUuid());
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        compl.addError(reply.getError());
                    } else {
                        logger.debug(String.format("delete AffinityGroup[uuid:%s] success", agInv.getUuid()));
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.hasError()) {
                    completion.fail(multiErr(errorCodeList));
                    return;
                }
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private List<AffinityGroupInventory> AffinityGroupFromAction(CascadeAction action) {
        List<AffinityGroupInventory> ret = null;
        if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transform(action.getParentIssuerContext(), AccountInventory::getUuid);
            List<AffinityGroupVO> vos = ResourceHelper.findOwnResources(AffinityGroupVO.class, auuids);
            if (!vos.isEmpty()) {
                ret = AffinityGroupInventory.valueOf(vos);
            }
        }

        return ret;
    }
}
