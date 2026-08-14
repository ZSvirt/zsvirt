package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.configuration.InstanceOfferingDeletionMsg;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class InstanceOfferingCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(InstanceOfferingCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = InstanceOfferingVO.class.getSimpleName();


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
        deleteInstanceOfferingEONotReferredByVm();
        completion.success();
    }

    private void deleteInstanceOfferingEONotReferredByVm() {
        String sql = "select i.uuid from InstanceOfferingEO i where i.deleted is not null and i.uuid not in (select vm.instanceOfferingUuid from VmInstanceVO vm where vm.instanceOfferingUuid is not null)";
        dbf.hardDeleteCollectionSelectedBySQL(sql, InstanceOfferingVO.class);
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<InstanceOfferingInventory> ioinvs = instanceOfferingFromAction(action);
        if (ioinvs == null || ioinvs.isEmpty()) {
            completion.success();
            return;
        }

        List<InstanceOfferingDeletionMsg> msgs = new ArrayList<InstanceOfferingDeletionMsg>();
        for (InstanceOfferingInventory ioinv : ioinvs) {
            InstanceOfferingDeletionMsg msg = new InstanceOfferingDeletionMsg();
            msg.setInstanceOfferingUuid(ioinv.getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(msg, ConfigurationConstant.SERVICE_ID, ioinv.getUuid());
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.emptyList();
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<InstanceOfferingInventory> instanceOfferingFromAction(CascadeAction action) {
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return getForAccount((List<AccountInventory>) action.getParentIssuerContext());
        }

        throw new CloudRuntimeException("should not be here");
    }

    @Transactional(readOnly = true)
    private List<InstanceOfferingInventory> getForAccount(List<AccountInventory> invs) {
        List<String> accountUuids = CollectionUtils.transform(invs, AccountInventory::getUuid);
        List<InstanceOfferingVO> vos = ResourceHelper.findOwnResources(InstanceOfferingVO.class, accountUuids);
        return InstanceOfferingInventory.valueOf(vos);
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<InstanceOfferingInventory> ctx = instanceOfferingFromAction(action);
            if (ctx != null && !ctx.isEmpty()) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
