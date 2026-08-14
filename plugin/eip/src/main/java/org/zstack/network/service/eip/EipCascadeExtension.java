package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class EipCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(EipCascadeExtension.class);
    private static final String NAME = EipVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

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
        dbf.eoCleanup(EipVO.class);
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        final List<EipInventory> eipinvs = eipFromAction(action);
        if (eipinvs == null || eipinvs.isEmpty()) {
            completion.success();
            return;
        }

        List<EipDeletionMsg> msgs = transform(eipinvs, arg -> {
            EipDeletionMsg msg = new EipDeletionMsg();
            msg.setEipUuid(arg.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, EipConstant.SERVICE_ID, msg.getEipUuid());
            return msg;
        });

        bus.send(msgs, 10, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    EipInventory eip = eipinvs.get(replies.indexOf(r));
                    if (!r.isSuccess()) {
                        logger.warn(String.format("failed to delete eip[uuid:%s, ip: %s, name:%s], %s",
                                eip.getUuid(), eip.getVipIp(), eip.getName(), r.getError()));
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
        return Arrays.asList(VipVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<EipInventory> eips = eipFromAction(action);
            if (eips != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(eips);
            }
        }
        return null;
    }

    private List<EipInventory> eipFromAction(CascadeAction action) {
        if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> vipUuids = transformAndRemoveNull(action.getParentIssuerContext(), VipInventory::getUuid);
            if (vipUuids.isEmpty()) {
                return null;
            }

            List<EipVO> eipVOS = Q.New(EipVO.class).in(EipVO_.vipUuid, vipUuids).list();

            return EipInventory.valueOf(eipVOS);
        } else if (EipVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }
        return null;
    }
}