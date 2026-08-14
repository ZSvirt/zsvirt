package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.zone.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

class ClusterExtensionToZone implements ZoneChangeStateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ClusterExtensionToZone.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ClusterExtensionPointEmitter extpEmitter;

    private List<ClusterVO> findClusterUnderZone(String uuid) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.add(ClusterVO_.zoneUuid, Op.EQ, uuid);
        return q.list();
    }

    @Override
    public void preChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) throws ZoneException {
        if (!event.toString().equals(ClusterStateEvent.disable.toString()) && !event.toString().equals(ClusterStateEvent.enable.toString())) {
            logger.debug("Unsupported ZoneStateEvent: " + event + ", won't propgate to extensions of cluster");
            return;
        }

        ClusterStateEvent clusterEvent = ClusterStateEvent.valueOf(event.toString());
        List<ClusterVO> vos = findClusterUnderZone(inventory.getUuid());
        try {
            extpEmitter.preChange(vos, clusterEvent);
        } catch (ClusterException e) {
            throw new ZoneException(e.getMessage(), e);
        }
    }

    @Override
    public void beforeChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) {
        if (!event.toString().equals(ClusterStateEvent.disable.toString()) && !event.toString().equals(ClusterStateEvent.enable.toString())) {
            logger.debug("Unsupport ZoneStateEvent: " + event + ", won't propgate to extensions of cluster");
            return;
        }

        List<ClusterVO> vos = findClusterUnderZone(inventory.getUuid());
        if (!vos.isEmpty()) {
            ClusterStateEvent clusterEvent = ClusterStateEvent.valueOf(event.toString());
            List<ChangeClusterStateMsg> msgs = new ArrayList<ChangeClusterStateMsg>(vos.size());
            for (ClusterVO vo : vos) {
                ChangeClusterStateMsg msg = new ChangeClusterStateMsg();
                msg.setUuid(vo.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, ClusterConstant.SERVICE_ID, vo.getUuid());
                msg.setStateEvent(clusterEvent.toString());
                msgs.add(msg);
            }

            /*
             * TODO: Changing 10 clusters each time, thinking about 10000
             * clusters, changing them at once will exhaust thread pool and
             * mysql connections
             */
            logger.debug("Changing state of clusters in zone: " + inventory.getName() + " uuid: " + inventory.getUuid() + " by event: " + clusterEvent);
            List<MessageReply> replies = bus.call(msgs);
            for (MessageReply r : replies) {
                if (!r.isSuccess()) {
                    ChangeClusterStateReply cr = (ChangeClusterStateReply) r;
                    logger.warn("Changing state of cluster: " + cr.getInventory().getName() + " uuid: " + cr.getInventory().getUuid() + " failed, "
                            + r.getError());
                }
            }
        }
    }

    @Override
    public void afterChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState previousState) {
        // TODO Auto-generated method stub

    }
}
