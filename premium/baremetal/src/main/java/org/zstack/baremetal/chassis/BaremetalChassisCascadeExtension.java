package org.zstack.baremetal.chassis;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.baremetal.chassis.*;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerInventory;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by GuoYi on 7/8/18.
 */
public class BaremetalChassisCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(BaremetalChassisCascadeExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static final String NAME = BaremetalChassisVO.class.getSimpleName();

    private static final int OP_NOPE = 0;
    private static final int OP_DELETION = 1;
    private static final int OP_NOPXESERVER = 2;

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
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<BaremetalChassisInventory> chassisInvs = chassisFromAction(action);
        if (chassisInvs == null) {
            completion.success();
            return;
        }

        if (op == OP_DELETION) {
            new While<>(chassisInvs).all((chassis, noErrorCompletion) -> {
                DeleteBaremetalChassisMsg msg = new DeleteBaremetalChassisMsg();
                msg.setUuid(chassis.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, BaremetalChassisConstant.SERVICE_ID, chassis.getUuid());
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
        } else if (op == OP_NOPXESERVER) {
            // all handled in BaremetalPxeServerDetachExtensionPoint
            completion.success();
        }
    }

    private int toDeletionOpCode(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return OP_NOPE;
        }

        if (ClusterVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (BaremetalChassisVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (BaremetalPxeServerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_NOPXESERVER;
        }

        return OP_NOPE;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                BaremetalPxeServerVO.class.getSimpleName(),
                ClusterVO.class.getSimpleName()
        );
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<BaremetalChassisInventory> chassisFromAction(CascadeAction action) {
        List<BaremetalChassisInventory> ret = new ArrayList<>();

        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (ClusterVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ClusterInventory> clusters = action.getParentIssuerContext();
            List<String> clusterUuids = transformAndRemoveNull(clusters, ClusterInventory::getUuid);

            if (!clusterUuids.isEmpty()) {
                List<BaremetalChassisVO> lst = Q.New(BaremetalChassisVO.class)
                        .in(BaremetalChassisVO_.clusterUuid, clusterUuids)
                        .list();
                if (lst != null && lst.size() != 0) {
                    for (BaremetalChassisVO vo : lst) {
                        ret.add(BaremetalChassisInventory.valueOf(vo));
                    }
                }
            }
        } else if (BaremetalPxeServerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<BaremetalPxeServerInventory> pxes = action.getParentIssuerContext();
            List<String> pxeUuids = transformAndRemoveNull(pxes, BaremetalPxeServerInventory::getUuid);

            if (!pxeUuids.isEmpty()) {
                List<BaremetalChassisVO> lst = Q.New(BaremetalChassisVO.class)
                        .in(BaremetalChassisVO_.pxeServerUuid, pxeUuids)
                        .list();
                if (lst != null && lst.size() != 0) {
                    for (BaremetalChassisVO vo : lst) {
                        ret.add(BaremetalChassisInventory.valueOf(vo));
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<BaremetalChassisInventory> invs = chassisFromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }

        return null;
    }
}
