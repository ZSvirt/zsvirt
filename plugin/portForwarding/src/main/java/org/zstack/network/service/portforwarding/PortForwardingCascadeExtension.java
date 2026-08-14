package org.zstack.network.service.portforwarding;

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

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class PortForwardingCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(PortForwardingCascadeExtension.class);
    private static final String NAME = PortForwardingRuleVO.class.getSimpleName();

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
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        final List<PortForwardingRuleInventory> pfinvs = pfFromAction(action);
        if (pfinvs == null || pfinvs.isEmpty()) {
            completion.success();
            return;
        }

        List<PortForwardingRuleDeletionMsg> msgs = transformAndRemoveNull(pfinvs, arg -> {
            PortForwardingRuleDeletionMsg msg = new PortForwardingRuleDeletionMsg();
            msg.setRuleUuids(asList(arg.getUuid()));
            bus.makeTargetServiceIdByResourceUuid(msg, PortForwardingConstant.SERVICE_ID, arg.getUuid());
            return msg;
        });

        bus.send(msgs, 10, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    PortForwardingRuleInventory pf = pfinvs.get(replies.indexOf(r));
                    if (!r.isSuccess()) {
                        logger.warn(String.format("failed to delete portForwardingRule[uuid:%s, ip: %s, name:%s], %s",
                                pf.getUuid(), pf.getVipIp(), pf.getName(), r.getError()));
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
            List<PortForwardingRuleInventory> pfs = pfFromAction(action);
            if (pfs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(pfs);
            }
        }
        return null;
    }

    private List<PortForwardingRuleInventory> pfFromAction(CascadeAction action) {
        if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> vipUuids = transformAndRemoveNull(action.getParentIssuerContext(), VipInventory::getUuid);
            if (vipUuids.isEmpty()) {
                return null;
            }

            List<PortForwardingRuleVO> pfVos = Q.New(PortForwardingRuleVO.class).in(PortForwardingRuleVO_.vipUuid, vipUuids).list();
            return PortForwardingRuleInventory.valueOf(pfVos);
        } else if (PortForwardingRuleVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }
        return null;
    }
}