package org.zstack.baremetal.chassis;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.baremetal.chassis.BaremetalChassisStateEvent;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO_;
import org.zstack.header.cluster.ClusterChangeStateExtensionPoint;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterStateEvent;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class BaremetalChassisExtensionToCluster implements ClusterChangeStateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BaremetalChassisExtensionToCluster.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void preChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) {
    }

    @Override
    public void beforeChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) {
        if (event != ClusterStateEvent.disable && event != ClusterStateEvent.enable) {
            logger.debug("Unsupported ClusterStateEvent: " + event + ", won't propgate to extensions of baremetal chassis");
            return;
        }

        BaremetalChassisStateEvent chassisEvent = BaremetalChassisStateEvent.valueOf(event.toString());
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<BaremetalChassisVO> vos = q(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.clusterUuid, inventory.getUuid())
                        .list();
                if (vos == null) {
                    return;
                }

                for (BaremetalChassisVO vo : vos) {
                    vo.setState(vo.getState().nextState(chassisEvent));
                }
                databaseFacade.updateCollection(vos);
                logger.debug("Successfully changed baremetal chassis state after bm cluster state changed.");
            }
        }.execute();
    }

    @Override
    public void afterChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState previousState) {
    }
}
