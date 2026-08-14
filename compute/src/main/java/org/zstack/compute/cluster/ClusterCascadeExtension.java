package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.cluster.*;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.inerr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class ClusterCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ClusterCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ClusterExtensionPointEmitter extpEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = ClusterVO.class.getSimpleName();

    @Override
    public void syncCascade(CascadeAction action) throws CascadeException {

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
        dbf.eoCleanup(ClusterVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<ClusterInventory> cinvs = clusterFromAction(action);
        if (cinvs == null) {
            completion.success();
            return;
        }

        List<ClusterDeletionMsg> msgs = new ArrayList<ClusterDeletionMsg>();
        for (ClusterInventory cinv : cinvs) {
            ClusterDeletionMsg msg = new ClusterDeletionMsg();
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            msg.setClusterUuid(cinv.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, ClusterConstant.SERVICE_ID, cinv.getUuid());
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
                    ClusterInventory inv = cinvs.get(replies.indexOf(r));
                    uuids.add(inv.getUuid());
                    logger.debug(String.format("deleted cluster[uuid:%s, name:%s]", inv.getUuid(), inv.getName()));
                }

                dbf.removeByPrimaryKeys(uuids, ClusterVO.class);
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        List<ClusterInventory> cinvs = clusterFromAction(action);
        if (cinvs == null) {
            completion.success();
            return;
        }

        try {
            for (ClusterInventory cinv : cinvs) {
                extpEmitter.preDelete(cinv);
            }

            completion.success();
        } catch (ClusterException e) {
            completion.fail(inerr(e.getMessage()));
        }
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ZoneVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<ClusterInventory> clusterFromAction(CascadeAction action) {
        List<ClusterInventory> ret = null;
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ZoneInventory> zones = action.getParentIssuerContext();
            List<String> zuuids = transformAndRemoveNull(zones, ZoneInventory::getUuid);

            SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
            q.add(ClusterVO_.zoneUuid, SimpleQuery.Op.IN, zuuids);
            List<ClusterVO> cvos = q.list();
            if (!cvos.isEmpty()) {
                ret = ClusterInventory.valueOf(cvos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<ClusterInventory> ctx = clusterFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
