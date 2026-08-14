package org.zstack.vpc.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vpc.ha.*;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VpcHaGroupCascadeExtesion extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VpcHaGroupCascadeExtesion.class);

    @Autowired
    private CloudBus bus;

    private static final String NAME = VpcHaGroupVO.class.getSimpleName();

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

    @Override
    public List<String> getEdgeNames() {
        List<String> ret = new ArrayList<>();
        ret.add(AccountVO.class.getSimpleName());
        return ret;
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VpcHaGroupInventory> ctx = VpcHaRouterFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }

    private List<VpcHaGroupInventory> VpcHaRouterFromAction(CascadeAction action) {
        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccountInventory> accounts = action.getParentIssuerContext();
            List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

            List<String> uuids = ResourceHelper.findOwnResourceUuidList(VpcHaGroupVO.class, accountUuids);
            if (uuids == null || uuids.isEmpty()) {
                return null;
            }

            List<VpcHaGroupVO> vpcHas = Q.New(VpcHaGroupVO.class).in(VpcHaGroupVO_.uuid, uuids).list();
            if (vpcHas == null || vpcHas.isEmpty()) {
                return null;
            } else {
                return VpcHaGroupInventory.valueOf(vpcHas);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<VpcHaGroupInventory> vpcHas = VpcHaRouterFromAction(action);
        if (vpcHas == null || vpcHas.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(vpcHas).step((inv, compl) -> {
            VpcHaGroupDeletionMsg dmsg = new VpcHaGroupDeletionMsg();
            dmsg.setUuid(inv.getUuid());

            dmsg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(dmsg, VpcHaGroupConstants.SERVICE_ID, inv.getUuid());
            bus.send(dmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    } else {
                        logger.debug(String.format("delete vpc ha group [uuid:%s] success", dmsg.getUuid()));
                    }
                    compl.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }
}
