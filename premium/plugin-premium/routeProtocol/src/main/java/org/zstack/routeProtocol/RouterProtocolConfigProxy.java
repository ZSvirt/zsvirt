package org.zstack.routeProtocol;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.header.protocol.NetworkRouterAreaRefVO;
import org.zstack.header.protocol.NetworkRouterAreaRefVO_;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.ha.VpcHaGroupConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterConfigProxy;
import org.zstack.tag.SystemTagCreator;
import org.zstack.vpc.VpcSystemTags;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


public class RouterProtocolConfigProxy extends VirtualRouterConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {

    @Override
    protected void attachNetworkServiceToNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {

    }

    @Override
    protected void detachNetworkServiceFromNoHaVirtualRouter(String vrUuid, String type, List<String> serviceUuids) {

    }

    @Override
    protected List<String> getNoHaVirtualRouterUuidsByNetworkService(String serviceUuid) {
        return new ArrayList<>();
    }

    @Override
    protected List<String> getServiceUuidsByNoHaVirtualRouter(String vrUuid) {
        return new ArrayList<>();
    }


    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {
        SQL.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.vRouterUuid, inv.getUuid()).set(NetworkRouterAreaRefVO_.vRouterUuid, haUuid)
                .set(NetworkRouterAreaRefVO_.applianceVmType, VpcHaGroupConstants.VPCHA_GROUP_VROUTER_VM_TYPE).update();
    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {
        SQL.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.vRouterUuid, haUuid).set(NetworkRouterAreaRefVO_.vRouterUuid, inv.getUuid())
                .set(NetworkRouterAreaRefVO_.applianceVmType, VpcConstants.VPC_VROUTER_VM_TYPE).update();
    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        completion.done();
    }

    protected String getHaUuidOfVpcRouter(String vrUuid) {
        return VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
    }

    protected void setOspfRouterId(String vrUuid, String routerId) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpcVo.isHaEnabled()) {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter("OpsfRouterId", asList(routerId), vrUuid);
            }
        } else {
            if (VpcSystemTags.VROUTER_ROUTER_ID.hasTag(vrUuid)) {
                if (!routerId.equals(
                        VpcSystemTags.VROUTER_ROUTER_ID.getTokenByResourceUuid(vrUuid, VpcSystemTags.VROUTER_ROUTER_ID_TOKEN)) ) {
                    VpcSystemTags.VROUTER_ROUTER_ID.update(vrUuid, VpcSystemTags.VROUTER_ROUTER_ID.instantiateTag(
                            map(e(VpcSystemTags.VROUTER_ROUTER_ID_TOKEN,routerId))
                    ));
                }
            } else {
                SystemTagCreator creator = VpcSystemTags.VROUTER_ROUTER_ID.newSystemTagCreator(vrUuid);
                creator.setTagByTokens(map(
                        e(VpcSystemTags.VROUTER_ROUTER_ID_TOKEN, routerId)
                ));
                creator.create();
            }
        }
    }

    protected String getOspfRouterId(String vrUuid) {
        String routerId = null;
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpcVo.isHaEnabled()) {
            final VirtualRouterVmInventory vrinv = VirtualRouterVmInventory.valueOf(vpcVo);
            for (VmNicInventory nic : vrinv.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(vrinv.getManagementNetworkUuid())) {
                    routerId = nic.getIp();
                }
            }
            /*List<VirtualRouterHaGroupExtensionPoint> exts = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            List<String> routerIds = exts.get(0).getNetworkServicesFromHaVrUuid("OpsfRouterId", vrUuid);
            if (routerIds != null && !routerIds.isEmpty()) {
                routerId = routerIds.get(0);
            }*/
        } else {
            if (VpcSystemTags.VROUTER_ROUTER_ID.hasTag(vrUuid)) {
                routerId =VpcSystemTags.VROUTER_ROUTER_ID.getTokenByResourceUuid(vrUuid, VpcSystemTags.VROUTER_ROUTER_ID_TOKEN);
            } else {
                final VirtualRouterVmInventory vrinv = VirtualRouterVmInventory.valueOf(vpcVo);
                for (VmNicInventory nic : vrinv.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(vrinv.getManagementNetworkUuid())) {
                        routerId = nic.getIp();
                    }
                }
            }
        }

        return routerId;
    }

    protected List<NetworkRouterAreaRefVO> getNetworkRouterAreaRef(List<String> l3Uuids, String vrUuid) {
        String vrouterUuid = vrUuid;
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpcVo.isHaEnabled()) {
            vrouterUuid = getHaUuidOfVpcRouter(vrUuid);
        }
        List<NetworkRouterAreaRefVO> vos = Q.New(NetworkRouterAreaRefVO.class)
                .in(NetworkRouterAreaRefVO_.l3NetworkUuid, l3Uuids)
                .eq(NetworkRouterAreaRefVO_.vRouterUuid, vrouterUuid).list();

        return vos;
    }

    protected String getMasterVrUuid(String haUuid) {
        return VpcHaGroupOperator.getMasterUuidByVpcHaRouterUuid(haUuid);
    }
}
