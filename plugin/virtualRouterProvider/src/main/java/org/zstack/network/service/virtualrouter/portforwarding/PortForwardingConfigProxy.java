package org.zstack.network.service.virtualrouter.portforwarding;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;

import java.util.ArrayList;
import java.util.List;

public class PortForwardingConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {
    @Override
    protected void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        List<VirtualRouterPortForwardingRuleRefVO> refs = new ArrayList<VirtualRouterPortForwardingRuleRefVO>();
        for (String uuid : serviceUuids) {
            if (!Q.New(VirtualRouterPortForwardingRuleRefVO.class)
                    .eq(VirtualRouterPortForwardingRuleRefVO_.uuid, uuid)
                    .eq(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, vrUuid)
                    .isExists()) {
                PortForwardingRuleVO rule = dbf.findByUuid(uuid, PortForwardingRuleVO.class);
                VirtualRouterPortForwardingRuleRefVO ref = new VirtualRouterPortForwardingRuleRefVO();
                ref.setVirtualRouterVmUuid(vrUuid);
                ref.setVipUuid(rule.getVipUuid());
                ref.setUuid(rule.getUuid());
                refs.add(ref);
            }
        }
        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        SQL.New(VirtualRouterPortForwardingRuleRefVO.class).eq(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, vrUuid)
                .in(VirtualRouterPortForwardingRuleRefVO_.uuid, serviceUuids).delete();
    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        return Q.New(VirtualRouterPortForwardingRuleRefVO.class).select(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid)
                .eq(VirtualRouterPortForwardingRuleRefVO_.uuid, serviceUuid).listValues();
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        return Q.New(VirtualRouterPortForwardingRuleRefVO.class).eq(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, vrUuid)
                .select(VirtualRouterPortForwardingRuleRefVO_.uuid).listValues();
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
