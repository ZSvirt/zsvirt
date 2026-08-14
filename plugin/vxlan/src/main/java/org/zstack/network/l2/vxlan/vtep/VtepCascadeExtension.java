package org.zstack.network.l2.vxlan.vtep;

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
import org.zstack.header.network.l2.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by weiwang on 12/05/2017.
 */
public class VtepCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VtepCascadeExtension.class);

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            handleDetach(action, completion);
        } else {
            completion.success();
        }
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = VtepVO.class.getSimpleName();

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(L2NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private void handleDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        List<VtepVO> vteps = new ArrayList<>();
        for (L2NetworkDetachStruct s : structs) {
            vteps.addAll(Q.New(VtepVO.class).eq(VtepVO_.poolUuid, s.getL2NetworkUuid()).eq(VtepVO_.clusterUuid, s.getClusterUuid()).list());
        }

        if (vteps.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(vteps).all((vtep, completion1) -> {
            DeleteVtepMsg msg = new DeleteVtepMsg();
            msg.setL2NetworkUuid(vtep.getPoolUuid());
            msg.setVtepUuid(vtep.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }
                    completion1.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(VtepVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        List<VtepInventory> vteps = vtepFromAction(action);

        if (vteps == null) {
            completion.success();
            return;
        }

        new While<>(vteps).all((vtep, completion1) -> {
            DeleteVtepMsg msg = new DeleteVtepMsg();
            msg.setL2NetworkUuid(vtep.getPoolUuid());
            msg.setVtepUuid(vtep.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }
                    completion1.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private List<VtepInventory> vtepFromAction(CascadeAction action) {
        List<VtepInventory> ret = null;
        if (L2NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> l2uuids = transformAndRemoveNull(action.getParentIssuerContext(), L2NetworkInventory::getUuid);

            List<VtepVO> vos = Q.New(VtepVO.class).in(VtepVO_.poolUuid, l2uuids).list();
            if (!vos.isEmpty()) {
                ret = VtepInventory.valueOf(vos);
            }
        }  else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VtepInventory> ctx = vtepFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
