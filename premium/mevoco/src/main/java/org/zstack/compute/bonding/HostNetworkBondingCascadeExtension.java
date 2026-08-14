package org.zstack.compute.bonding;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.bonding.BondingDeletionMsg;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionDSL;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HostNetworkBondingCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(HostNetworkBondingCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = HostNetworkBondingVO.class.getSimpleName();

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
        final List<HostNetworkBondingInventory> bondings = hostNetworkBondingFromAction(action);
        ErrorCodeList elist = new ErrorCodeList();
        if (bondings == null) {
            completion.success();
            return;
        }

        List<BondingDeletionMsg> msgs = new ArrayList<BondingDeletionMsg>();
        for (HostNetworkBondingInventory bonding : bondings) {
            BondingDeletionMsg msg = new BondingDeletionMsg();
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            msg.setBondingUuid(bonding.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, HostNetworkBondingConstant.SERVICE_ID, bonding.getUuid());
            msgs.add(msg);
        }

        new While<>(msgs).all((msg, wcomp) -> {
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        elist.getCauses().add(reply.getError());
                    }
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (elist.getCauses().size() > 0) {
                    completion.fail(elist.getCauses().get(0));
                } else {
                    dbf.removeByPrimaryKeys(bondings.stream().map(HostNetworkBondingInventory::getUuid).collect(Collectors.toList()), HostNetworkBondingVO.class);
                    completion.success();
                }
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return CollectionDSL.list();
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<HostNetworkBondingInventory> hostNetworkBondingFromAction(CascadeAction action) {
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else {
            return null;
        }
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<HostNetworkBondingInventory> ctx = hostNetworkBondingFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
