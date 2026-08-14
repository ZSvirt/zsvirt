package org.zstack.vrouterRoute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by weiwang on 21/06/2017.
 */
public class VRouterRouteEntryCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VRouterRouteEntryCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = VRouterRouteEntryVO.class.getSimpleName();

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                VRouterRouteTableVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VRouterRouteEntryInventory> ctx = VRouterRouteEntryFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(VRouterRouteEntryVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<VRouterRouteEntryInventory> invs = VRouterRouteEntryFromAction(action);
        if (invs == null) {
            completion.success();
            return;
        }

        new While<>(invs).all((inv, compl) -> {
            VRouterRouteEntryDeletionMsg dmsg = new VRouterRouteEntryDeletionMsg();
            dmsg.setUuid(inv.getUuid());

            dmsg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(dmsg, VRouterRouteConstants.SERVICE_ID, inv.getUuid());
            bus.send(dmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    } else {
                        logger.debug(String.format("delete VRouterRouteEntry[uuid:%s] success", dmsg.getUuid()));
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private List<VRouterRouteEntryInventory> VRouterRouteEntryFromAction(CascadeAction action) {
        if (VRouterRouteTableVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> tableUuids = transformAndRemoveNull(action.getParentIssuerContext(), VRouterRouteTableInventory::getUuid);

            List<VRouterRouteEntryVO> entryVOS = Q.New(VRouterRouteEntryVO.class)
                    .in(VRouterRouteEntryVO_.routeTableUuid, tableUuids)
                    .list();

            if (!entryVOS.isEmpty()) {
                return VRouterRouteEntryInventory.valueOf(entryVOS);
            }

        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }
}
