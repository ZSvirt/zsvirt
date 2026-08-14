package org.zstack.network.service.virtualrouter.lb;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;

import java.util.ArrayList;
import java.util.List;

public class LbConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {
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

    @Override
    protected void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        List<VirtualRouterLoadBalancerRefVO> refs = new ArrayList<>();
        for (String uuid : serviceUuids) {
            if (!Q.New(VirtualRouterLoadBalancerRefVO.class)
                    .eq(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid, uuid)
                    .eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, vrUuid).isExists()) {
                VirtualRouterLoadBalancerRefVO ref = new VirtualRouterLoadBalancerRefVO();
                ref.setLoadBalancerUuid(uuid);
                ref.setVirtualRouterVmUuid(vrUuid);
                refs.add(ref);
            }
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        SQL.New(VirtualRouterLoadBalancerRefVO.class)
                .in(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid, serviceUuids)
                .eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, vrUuid).delete();
    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        return Q.New(VirtualRouterLoadBalancerRefVO.class).eq(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid, serviceUuid)
                .select(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid).listValues();
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        return Q.New(VirtualRouterLoadBalancerRefVO.class).eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, vrUuid)
                .select(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid).listValues();
    }
}
