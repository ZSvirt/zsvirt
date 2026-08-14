package org.zstack.baremetal.pxeserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class BaremetalPxeServerCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(BaremetalPxeServerCascadeExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static final String NAME = BaremetalPxeServerVO.class.getSimpleName();

    private static final int OP_NOPE = 0;
    private static final int OP_DELETION = 1;

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

    private void handleDeletion(CascadeAction action, Completion completion) {
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<BaremetalPxeServerInventory> pxeServerInvs = pxeServerFromAction(action);
        if (pxeServerInvs == null) {
            completion.success();
            return;
        }

        if (op == OP_DELETION) {
            new While<>(pxeServerInvs).all((pxe, noErrorCompletion) -> {
                DeleteBaremetalPxeServerMsg msg = new DeleteBaremetalPxeServerMsg();
                msg.setUuid(pxe.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, BaremetalPxeServerConstant.SERVICE_ID, pxe.getUuid());
                bus.send(msg, new CloudBusCallBack(noErrorCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            }
                        }
                        noErrorCompletion.done();
                    }
                });
            }).run(new WhileDoneCompletion(completion) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    completion.success();
                }
            });
        }
    }

    private int toDeletionOpCode(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return OP_NOPE;
        }

        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (BaremetalPxeServerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        return OP_NOPE;
    }

    private List<BaremetalPxeServerInventory> pxeServerFromAction(CascadeAction action) {
        List<BaremetalPxeServerInventory> ret = null;
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> zuuids = transformAndRemoveNull(action.getParentIssuerContext(), ZoneInventory::getUuid);
            if (zuuids != null && !zuuids.isEmpty()) {
                List<BaremetalPxeServerVO> pxes = Q.New(BaremetalPxeServerVO.class).in(BaremetalPxeServerVO_.zoneUuid, zuuids).list();
                ret = BaremetalPxeServerInventory.valueOf(pxes);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(ZoneVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<BaremetalPxeServerInventory> ctx = pxeServerFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
