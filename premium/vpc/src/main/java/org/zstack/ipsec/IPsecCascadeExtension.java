package org.zstack.ipsec;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by MaJin on 2017-04-20.
 */
public class IPsecCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(IPsecCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = IPsecConnectionVO.class.getSimpleName();

    @Override
    public List<String> getEdgeNames() {
        List<String> ret = new ArrayList<>();
        ret.add(L3NetworkVO.class.getSimpleName());
        ret.add(VipVO.class.getSimpleName());
        return ret;
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<IPsecConnectionInventory> ctx = IPsecConnectionFromAction(action);
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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(IPsecConnectionVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<IPsecConnectionInventory> IPsecInvs = IPsecConnectionFromAction(action);
        if (IPsecInvs == null || IPsecInvs.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(IPsecInvs).step((IPsecInv, compl) -> {
            IPsecConnectionDeletionMsg dmsg = new IPsecConnectionDeletionMsg();
            dmsg.setUuid(IPsecInv.getUuid());

            dmsg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(dmsg, IPsecConstants.SERVICE_ID, IPsecInv.getUuid());
            bus.send(dmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    } else {
                        logger.debug(String.format("delete IPsecConncection[uuid:%s] success", dmsg.getUuid()));
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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }


    private List<IPsecConnectionInventory> IPsecConnectionFromAction(CascadeAction action) {
        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            /* delete vpc network will not delete ipsec, so use extension to delete it */
            List<L3NetworkInventory> basicL3Networks = ((List<L3NetworkInventory>) action.getParentIssuerContext()).stream()
                    .filter(inv -> !inv.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)).collect(Collectors.toList());

            if (basicL3Networks.isEmpty()) {
                return null;
            }
            List<String> l3Uuids = transformAndRemoveNull(basicL3Networks, L3NetworkInventory::getUuid);

            List<String> IPsecUuids = Q.New(IPsecL3NetworkRefVO.class).select(IPsecL3NetworkRefVO_.connectionUuid)
                    .in(IPsecL3NetworkRefVO_.l3NetworkUuid, l3Uuids).listValues();
            if (IPsecUuids == null || IPsecUuids.isEmpty()) {
                return null;
            }

            List<IPsecConnectionVO> ipvos = Q.New(IPsecConnectionVO.class).in(IPsecConnectionVO_.uuid, IPsecUuids).list();
            if (ipvos == null || ipvos.isEmpty()) {
                return null;
            } else {
                return IPsecConnectionInventory.valueOf(ipvos);
            }
        } if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> vipUuids = transformAndRemoveNull(action.getParentIssuerContext(), VipInventory::getUuid);
            if (vipUuids.isEmpty()) {
                return null;
            }

            List<IPsecConnectionVO> ipsecVos = Q.New(IPsecConnectionVO.class).in(IPsecConnectionVO_.vipUuid, vipUuids).list();
            return IPsecConnectionInventory.valueOf(ipsecVos);
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }
}
