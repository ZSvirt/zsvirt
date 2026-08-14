package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.inerr;
/**
 */
public class L3NetworkCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(L3NetworkCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private L3NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = L3NetworkVO.class.getSimpleName();

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
        try {
            final List<L3NetworkInventory> l3invs = l3NetworkFromAction(action);
            if (l3invs != null) {
                l3invs.forEach(l -> dbf.eoCleanup(L3NetworkVO.class, l.getUuid()));
            } else {
                dbf.eoCleanup(L3NetworkVO.class);
            }
        } catch (NullPointerException e) {
            logger.warn(e.getLocalizedMessage());
            dbf.eoCleanup(L3NetworkVO.class);
        } finally {
            completion.success();
        }
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<L3NetworkInventory> l3invs = l3NetworkFromAction(action);
        if (l3invs == null) {
            completion.success();
            return;
        }

        List<L3NetworkDeletionMsg> msgs = new ArrayList<L3NetworkDeletionMsg>();
        for (L3NetworkInventory l3inv : l3invs) {
            L3NetworkDeletionMsg msg = new L3NetworkDeletionMsg();
            msg.setL3NetworkUuid(l3inv.getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3inv.getUuid());
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                List<String> uuids = new ArrayList<String>();
                for (MessageReply r : replies) {
                    L3NetworkInventory inv = l3invs.get(replies.indexOf(r));
                    uuids.add(inv.getUuid());
                    logger.debug(String.format("delete l3Network[uuid:%s, name:%s]", inv.getUuid(), inv.getName()));
                }

                dbf.removeByPrimaryKeys(uuids, L3NetworkVO.class);
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        List<L3NetworkInventory> l3invs = l3NetworkFromAction(action);
        if (l3invs == null) {
            completion.success();
            return;
        }

        try {
            for (L3NetworkInventory prinv : l3invs) {
                extpEmitter.preDelete(prinv);
            }

            completion.success();
        } catch (L3NetworkException e) {
            completion.fail(inerr(e.getMessage()));
        }
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(L2NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<L3NetworkInventory> l3NetworkFromAction(CascadeAction action) {
        List<L3NetworkInventory> ret = null;
        if (L2NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> l2uuids = CollectionUtils.transform(action.getParentIssuerContext(), L2NetworkInventory::getUuid);

            SimpleQuery<L3NetworkVO> q = dbf.createQuery(L3NetworkVO.class);
            q.add(L3NetworkVO_.l2NetworkUuid, SimpleQuery.Op.IN, l2uuids);
            List<L3NetworkVO> l3vos = q.list();
            if (!l3vos.isEmpty()) {
                ret = L3NetworkInventory.valueOf(l3vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transform(action.getParentIssuerContext(), AccountInventory::getUuid);

            List<L3NetworkVO> l3vos = ResourceHelper.findOwnResources(L3NetworkVO.class, auuids);
            if (!l3vos.isEmpty()) {
                ret = L3NetworkInventory.valueOf(l3vos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<L3NetworkInventory> ctx = l3NetworkFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
