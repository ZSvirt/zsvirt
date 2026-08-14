package org.zstack.baremetal.pxeserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerState;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerStateEvent;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO_;
import org.zstack.header.zone.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by GuoYi on 2018-11-01.
 */
public class BaremetalPxeServerExtensionToZone implements ZoneChangeStateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BaremetalPxeServerExtensionToZone.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void preChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) throws ZoneException {

    }

    @Override
    public void beforeChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) {
        if (event != ZoneStateEvent.disable && event != ZoneStateEvent.enable) {
            logger.debug("Unsupported ZoneStateEvent: " + event + ", won't propgate to extensions of baremetal pxeserver");
            return;
        }

        BaremetalPxeServerStateEvent pxeEvent = BaremetalPxeServerStateEvent.valueOf(event.toString());
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<BaremetalPxeServerVO> vos = q(BaremetalPxeServerVO.class)
                        .notEq(BaremetalPxeServerVO_.state, BaremetalPxeServerState.Maintenance)
                        .eq(BaremetalPxeServerVO_.zoneUuid, inventory.getUuid())
                        .list();
                if (vos == null) {
                    return;
                }

                for (BaremetalPxeServerVO vo : vos) {
                    vo.setState(vo.getState().nextState(pxeEvent));
                }
                databaseFacade.updateCollection(vos);
                logger.debug("Successfully changed baremetal pxeserver state after Zone state changed.");
            }
        }.execute();
    }

    @Override
    public void afterChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState previousState) {

    }
}
