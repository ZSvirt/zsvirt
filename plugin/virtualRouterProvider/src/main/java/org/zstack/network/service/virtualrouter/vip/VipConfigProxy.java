package org.zstack.network.service.virtualrouter.vip;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class VipConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {
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
        List<VirtualRouterVipVO> refs = new ArrayList<>();
        for (String uuid : serviceUuids) {
            if (dbf.isExist(uuid, VirtualRouterVipVO.class)) {
                continue;
            }

            VirtualRouterVipVO ref = new VirtualRouterVipVO();
            ref.setUuid(uuid);
            ref.setVirtualRouterVmUuid(vrUuid);
            refs.add(ref);
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        SQL.New(VirtualRouterVipVO.class).in(VirtualRouterVipVO_.uuid, serviceUuids)
                .eq(VirtualRouterVipVO_.virtualRouterVmUuid, vrUuid).delete();
    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        VirtualRouterVipVO vipVo = dbf.findByUuid(serviceUuid, VirtualRouterVipVO.class);
        if (vipVo == null) {
            return null;
        }
        return asList(vipVo.getVirtualRouterVmUuid());
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        return Q.New(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.virtualRouterVmUuid, vrUuid).select(VirtualRouterVipVO_.uuid).listValues();
    }
}
