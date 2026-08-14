package org.zstack.network.service.virtualrouter.eip;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;

import java.util.ArrayList;
import java.util.List;

public class EipConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {
    @Override
    protected void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        List<VirtualRouterEipRefVO> refs = new ArrayList<>();
        for (String uuid : serviceUuids) {
            if (Q.New(VirtualRouterEipRefVO.class).eq(VirtualRouterEipRefVO_.eipUuid, uuid).isExists()) {
                continue;
            }

            VirtualRouterEipRefVO ref = new VirtualRouterEipRefVO();
            ref.setEipUuid(uuid);
            ref.setVirtualRouterVmUuid(vrUuid);
            refs.add(ref);
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        SQL.New(VirtualRouterEipRefVO.class).eq(VirtualRouterEipRefVO_.virtualRouterVmUuid, vrUuid)
                .in(VirtualRouterEipRefVO_.eipUuid, serviceUuids).delete();
    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        return Q.New(VirtualRouterEipRefVO.class).eq(VirtualRouterEipRefVO_.eipUuid, serviceUuid)
                .select(VirtualRouterEipRefVO_.virtualRouterVmUuid).listValues();
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        return Q.New(VirtualRouterEipRefVO.class).eq(VirtualRouterEipRefVO_.virtualRouterVmUuid, vrUuid)
                .select(VirtualRouterEipRefVO_.eipUuid).listValues();
    }

    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {

    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {

    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        completion.done();
    }
}
