package org.zstack.vpc;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vpc.VpcSnatStateVO;
import org.zstack.header.vpc.VpcSnatStateVO_;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;

import java.util.ArrayList;
import java.util.List;


public class SnatConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {

    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {
        /* for normal vpc,
         * if snat is enable on the public nic, there must have a record in VpcSnatStateVO and state is enable
         * if snat is disable on the public nic, there is no record
         * */
        List<String> snats = Q.New(VpcSnatStateVO.class).select(VpcSnatStateVO_.l3NetworkUuid)
                .eq(VpcSnatStateVO_.uuid, inv.getUuid()).eq(VpcSnatStateVO_.state, VpcStateEvent.enable.toString()).listValues();
        if (!snats.isEmpty()){
            attachNetworkService(inv.getUuid(), NetworkServiceType.SNAT.toString(), snats);
        }
    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {
        List<String> snats = Q.New(VpcSnatStateVO.class).select(VpcSnatStateVO_.l3NetworkUuid)
                .eq(VpcSnatStateVO_.uuid, inv.getUuid()).eq(VpcSnatStateVO_.state, VpcStateEvent.enable.toString()).listValues();
        if (!snats.isEmpty()){
            detachNetworkService(inv.getUuid(), NetworkServiceType.SNAT.toString(), snats);
        }
    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    protected void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        List<String> addedL3 = Q.New(VpcSnatStateVO.class).select(VpcSnatStateVO_.l3NetworkUuid)
                .eq(VpcSnatStateVO_.uuid, vrUuid).listValues();
        List<VpcSnatStateVO> refs = new ArrayList<>();
        for (String uuid : serviceUuids) {
            if (addedL3.contains(uuid)) {
                continue;
            }

            VpcSnatStateVO ref = new VpcSnatStateVO();
            ref.setUuid(Platform.getUuid());
            ref.setVpcUuid(vrUuid);
            ref.setL3NetworkUuid(uuid);
            ref.setState(VpcStateEvent.enable.toString());
            refs.add(ref);
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {
        SQL.New(VpcSnatStateVO.class).eq(VpcSnatStateVO_.vpcUuid, vrUuid)
                .in(VpcSnatStateVO_.l3NetworkUuid, serviceUuids).delete();
    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        return Q.New(VpcSnatStateVO.class).eq(VpcSnatStateVO_.l3NetworkUuid, serviceUuid)
                .select(VpcSnatStateVO_.vpcUuid).listValues();
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        return Q.New(VpcSnatStateVO.class).eq(VpcSnatStateVO_.vpcUuid, vrUuid)
                .select(VpcSnatStateVO_.l3NetworkUuid).listValues();
    }
}
