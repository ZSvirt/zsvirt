package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.*;
import org.zstack.header.host.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

class HostExtensionToCluster implements ClusterChangeStateExtensionPoint {
	private static final CLogger logger = Utils.getLogger(HostExtensionToCluster.class);
	
	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private CloudBus bus;
	@Autowired
	private HostExtensionPointEmitter extpEmitter;
	
	private List<HostVO> findHostUnderClusterByUuid(String clusterUuid) {
		SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
		query.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
		return query.list();
	}
	
	@Override
	public void preChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) throws ClusterException {
		if (!event.toString().equals(ClusterStateEvent.disable.toString()) && !event.toString().equals(ClusterStateEvent.enable.toString())) {
			logger.debug("Unsupported ClusterStateEvent: " + event + ", won't propgate to extensions of host");
            return;
		}
		
		HostStateEvent hostEvent = HostStateEvent.valueOf(event.toString());
		List<HostVO> vos = findHostUnderClusterByUuid(inventory.getUuid());
        try {
            extpEmitter.preChange(vos, hostEvent);
        } catch (HostException e) {
            throw new ClusterException(e.getMessage(), e);
        }
	}

	@Override
	public void beforeChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) {
		/*
		 * given changing cluster state is working simultaneously, we change host state one by one
		 * in order to avoid thread pool exhausted
		 */
		if (!event.toString().equals(ClusterStateEvent.disable.toString()) && !event.toString().equals(ClusterStateEvent.enable.toString())) {
			logger.debug("Unsupport ClusterStateEvent: " + event + ", won't propgate to extensions of host");
			return;
		}
		
		HostStateEvent hostEvent = HostStateEvent.valueOf(event.toString());
		List<HostVO> vos = findHostUnderClusterByUuid(inventory.getUuid());
		if (!vos.isEmpty()) {
			for (HostVO h : vos) {
				if (h.getState() == HostState.Maintenance || h.getState() == HostState.PreMaintenance) {
				    continue;
				}

				ChangeHostStateMsg msg = new ChangeHostStateMsg(h.getUuid(), hostEvent.toString());
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, h.getUuid());
				ChangeHostStateReply r = (ChangeHostStateReply) bus.call(msg);
				if (!r.isSuccess()) {
					logger.warn(String.format("Failed to change host[uuid: %s] state(%s) by event(%s), %s", h.getUuid(), h.getState(), hostEvent, r.getError()));
				} else {
					logger.debug(String.format("Successfully changed host[uuid: %s] state(%s) by event(%s), new state is %s", h.getUuid(), h.getState(), hostEvent, r.getInventory().getState()));
				}
			}
		}
	}

	@Override
	public void afterChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState previousState) {
		// TODO Auto-generated method stub

	}

}
