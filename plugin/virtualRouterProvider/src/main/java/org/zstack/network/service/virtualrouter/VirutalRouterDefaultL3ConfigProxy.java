package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;
import org.zstack.utils.DebugUtils;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class VirutalRouterDefaultL3ConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {
    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {
        attachNetworkService(inv.getUuid(), VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK, asList(inv.getDefaultRouteL3NetworkUuid()));
    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {

    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    protected void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        SQL.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid)
                .set(VirtualRouterVmVO_.defaultRouteL3NetworkUuid, serviceUuids.get(0)).update();
    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        /**/
    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        DebugUtils.Assert(false, String.format("this API should not be called"));
        return null;
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        String defRouteL3Uuid = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid)
                .select(VirtualRouterVmVO_.defaultRouteL3NetworkUuid).findValue();
        return Collections.singletonList(defRouteL3Uuid);
    }

    @Override
    public void attachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            SQL.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid)
                    .set(VirtualRouterVmVO_.defaultRouteL3NetworkUuid, serviceUuids.get(0)).update();
        } else {
            List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            if (exps.isEmpty()) {
                return;
            }

            for (VirtualRouterHaGroupExtensionPoint ext : exps) {
                ext.attachNetworkServiceToHaRouter(type, serviceUuids, vrUuid);
            }

            /* TODO: this is a special case for ha router configuration */
            SQL.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid)
                    .set(VirtualRouterVmVO_.defaultRouteL3NetworkUuid, serviceUuids.get(0)).update();

            String peerUuid = exps.get(0).getPeerUuid(vrUuid);
            if (peerUuid == null) {
                return;
            }

            SQL.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, peerUuid)
                    .set(VirtualRouterVmVO_.defaultRouteL3NetworkUuid, serviceUuids.get(0)).update();
        }
    }
}
