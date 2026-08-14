package org.zstack.vrouterRoute.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.vrouterRoute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 22/06/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosResyncRoutesFlow extends NoRollbackFlow {
    @Autowired
    private VyosRouteBackend bkd;
    @Autowired
    private VRouterRouteManager routeManager;

    String __name__ = "sync-vrouter-routes";

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<VRouterRouteEntryTo> routes = fetchRoutes(vr.getUuid());
        if (routes.isEmpty()) {
            trigger.next();
            return;
        } else {
            bkd.syncRouteEntries(routes, asList(vr.getUuid()), false);
            trigger.next();
        }
    }

    public List<VRouterRouteEntryTo> fetchRoutes(String vrouterVmUuid) {
        List<String> tableUuids = routeManager.getTableUuidsFromVrUuid(vrouterVmUuid);
        if (tableUuids == null || tableUuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<VRouterRouteEntryVO> entryVOS = Q.New(VRouterRouteEntryVO.class)
                .in(VRouterRouteEntryVO_.routeTableUuid, tableUuids)
                .list();
        return entryVOS.stream().map(entryVO -> VRouterRouteEntryTo.valueOf(entryVO))
                .collect(Collectors.toList());
    }
}
