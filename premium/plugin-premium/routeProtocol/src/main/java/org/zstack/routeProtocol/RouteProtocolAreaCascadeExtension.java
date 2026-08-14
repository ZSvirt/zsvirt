package org.zstack.routeProtocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.protocol.RouterAreaInventory;
import org.zstack.header.protocol.RouterAreaVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 */
public class RouteProtocolAreaCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(RouteProtocolAreaCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    private static final String NAME = RouterAreaVO.class.getSimpleName();

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
        final List<RouterAreaInventory> areas = action.getParentIssuerContext();
        List<String> uuids = new ArrayList<String>();
        if (areas != null && !areas.isEmpty()) {
            for (RouterAreaInventory inv : areas) {
                logger.debug(String.format("delete router area[uuid:%s, area id:%s]", inv.getUuid(), inv.getAreaId()));
                uuids.add(inv.getUuid());
            }
        }

        if (!uuids.isEmpty()) {
            dbf.removeByPrimaryKeys(uuids, RouterAreaVO.class);
        }
        completion.success();
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList();
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return action;
        }

        return null;
    }
}
