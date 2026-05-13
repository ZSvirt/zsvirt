package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class VipCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VipCascadeExtension.class);
    private static final String NAME = VipVO.class.getSimpleName();

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
        dbf.eoCleanup(VipVO.class);
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        final List<VipInventory> vipinvs = vipFromAction(action);
        if (vipinvs == null || vipinvs.isEmpty()) {
            completion.success();
            return;
        }

        List<VipDeletionMsg> msgs = transformAndRemoveNull(vipinvs, arg -> {
            VipDeletionMsg msg = new VipDeletionMsg();
            msg.setVipUuid(arg.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, msg.getVipUuid());
            return msg;
        });

        bus.send(msgs, 10, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    VipInventory vip = vipinvs.get(replies.indexOf(r));
                    if (!r.isSuccess()) {
                        logger.warn(String.format("failed to delete vip[uuid:%s, ip: %s, name:%s], %s",
                                vip.getUuid(), vip.getIp(), vip.getName(), r.getError()));
                        completion.fail(r.getError());
                        return;
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
        return Arrays.asList(L3NetworkVO.class.getSimpleName(), IpRangeVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VipInventory> vips = vipFromAction(action);
            if (vips != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(vips);
            }
        }
        return null;
    }

    private List<VipInventory> vipFromAction(CascadeAction action) {
        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> l3Uuids = CollectionUtils.transform(action.getParentIssuerContext(), L3NetworkInventory::getUuid);
            if (l3Uuids.isEmpty()) {
                return null;
            }

            List<VipVO> vipVOs = Q.New(VipVO.class)
                    .in(VipVO_.l3NetworkUuid, l3Uuids)
                    .list();
            return VipInventory.valueOf(vipVOs);
        } else if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> iprUuids = CollectionUtils.transform(action.getParentIssuerContext(), IpRangeInventory::getUuid);

            List<VipVO> vipVOs = Q.New(VipVO.class)
                    .in(VipVO_.ipRangeUuid, iprUuids)
                    .list();
            return VipInventory.valueOf(vipVOs);
        } else if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transform(action.getParentIssuerContext(), AccountInventory::getUuid);

            List<VipVO> vos = ResourceHelper.findOwnResources(VipVO.class, auuids);
            if (!vos.isEmpty()) {
                return VipInventory.valueOf(vos);
            }
        }
        return null;
    }
}
