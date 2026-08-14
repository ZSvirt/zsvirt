package org.zstack.vpc.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.network.service.VirtualRouterHaAPIMessage;
import org.zstack.header.vm.*;
import org.zstack.header.vpc.VpcRouterVmInventory;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.ha.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.vpc.APICreateVpcVRouterMsg;
import org.zstack.vpc.APISetVpcVRouterDistributedRoutingEnabledMsg;
import org.zstack.vpc.APISetVpcVRouterNetworkServiceStateMsg;
import org.zstack.vpc.APIUpdateVirtualRouterSoftwareVersionMsg;
import org.zstack.vrouterRoute.APIAttachVRouterRouteTableToVRouterMsg;
import org.zstack.vrouterRoute.APIDetachVRouterRouteTableFromVRouterMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by shixin.ruan on 20/04/2019
 */
@InterceptorForService("vpcHa")
public class VpcHaGroupApiInterceptor implements ApiMessageInterceptor , GlobalApiMessageInterceptor {

    @Autowired
    protected DatabaseFacade dbf;

    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIAttachL3NetworkToVmMsg.class);
        ret.add(APIDetachL3NetworkFromVmMsg.class);
        ret.add(APICreateVpcVRouterMsg.class);
        ret.add(APIStartVmInstanceMsg.class);
        ret.add(APIRebootVmInstanceMsg.class);
        ret.add(APIChangeVpcHaGroupMonitorIpsMsg.class);
        ret.add(APIAttachVRouterRouteTableToVRouterMsg.class);
        ret.add(APIDetachVRouterRouteTableFromVRouterMsg.class);
        ret.add(APISetVpcVRouterDistributedRoutingEnabledMsg.class);
        ret.add(APISetVpcVRouterNetworkServiceStateMsg.class);
        ret.add(APIUpdateVirtualRouterSoftwareVersionMsg.class);

        ret.addAll(BeanUtils.reflections.getSubTypesOf(VirtualRouterHaAPIMessage.class));

        return ret;
    }

    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachL3NetworkToVmMsg) {
            handle((APIAttachL3NetworkToVmMsg)msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            handle((APIDetachL3NetworkFromVmMsg)msg);
        } else if (msg instanceof APICreateVpcVRouterMsg) {
            handle((APICreateVpcVRouterMsg)msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            handle((APIStartVmInstanceMsg)msg);
        } else if (msg instanceof APIRebootVmInstanceMsg) {
            handle((APIRebootVmInstanceMsg)msg);
        }  else if (msg instanceof APICreateVpcHaGroupMsg) {
            handle((APICreateVpcHaGroupMsg)msg);
        } else if (msg instanceof APIChangeVpcHaGroupMonitorIpsMsg) {
            handle((APIChangeVpcHaGroupMonitorIpsMsg)msg);
        } else if (msg instanceof APISetVpcVRouterNetworkServiceStateMsg) {
            handle((APISetVpcVRouterNetworkServiceStateMsg)msg);
        } else if (msg instanceof APIUpdateVirtualRouterSoftwareVersionMsg) {
            handle((APIUpdateVirtualRouterSoftwareVersionMsg)msg);
        }  else if (msg instanceof VirtualRouterHaAPIMessage) {
            handle((VirtualRouterHaAPIMessage)msg);
        }

        return msg;
    }

    private String getHaMasterRouter(String vrUuid) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpcVo == null || !vpcVo.isHaEnabled()) {
            return null;
        }

        /* both vpc are stopped, return a fake master to make api operation go on*/
        String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
        if (peerUuid != null) {
            VpcRouterVmVO peerVo = dbf.findByUuid(peerUuid, VpcRouterVmVO.class);
            if (vpcVo.getState() == VmInstanceState.Stopped && peerVo.getState() == VmInstanceState.Stopped) {
                return vrUuid;
            }
        } else {
            if (vpcVo.getState() == VmInstanceState.Stopped) {
                return vrUuid;
            }
        }

        String masterUuid = VpcHaGroupOperator.getMasterUuidByVirtualRouterUuid(vrUuid);
        if (masterUuid == null) {
            throw new ApiMessageInterceptionException(argerr("there is no master router of router [uuid:%s]", vrUuid));
        }

        return masterUuid;
    }

    private void handle(APIUpdateVirtualRouterSoftwareVersionMsg msg) {
        String currentVersion = Q.New(VirtualRouterSoftwareVersionVO.class).eq(VirtualRouterSoftwareVersionVO_.uuid, msg.getVirtualRouterUuid())
                .eq(VirtualRouterSoftwareVersionVO_.softwareName, msg.getSoftwareName())
                .select(VirtualRouterSoftwareVersionVO_.currentVersion).findValue();
        String latestVersion = Q.New(VirtualRouterSoftwareVersionVO.class).eq(VirtualRouterSoftwareVersionVO_.uuid, msg.getVirtualRouterUuid())
                .eq(VirtualRouterSoftwareVersionVO_.softwareName, msg.getSoftwareName())
                .select(VirtualRouterSoftwareVersionVO_.latestVersion).findValue();
        if(latestVersion == null || currentVersion == null || currentVersion.equals("") || latestVersion.equals("")) {
            throw new ApiMessageInterceptionException(argerr("Could not update this network service, due to vpc [uuid:%s] is not support update network service version", msg.getVirtualRouterUuid()));
        }
        if(currentVersion.equals(msg.getTargetVersion())) {
            return;
        }
        VirtualRouterMetadataVO vo = dbf.findByUuid(msg.getVirtualRouterUuid(), VirtualRouterMetadataVO.class);
        if (vo.getKernelVersion().equals(VirtualRouterConstant.VR_OLD_KERNEL_VERSION)) {
            throw new ApiMessageInterceptionException(argerr("Could not update this network service, due to vpc [uuid:%s] used old kernel version:[%s]", msg.getVirtualRouterUuid(), vo.getKernelVersion()));
        }
    }

    private void handle(APISetVpcVRouterNetworkServiceStateMsg msg) {
        if(msg.getL3NetworkUuid() != null && msg.getNetworkService().equals(NetworkServiceType.SNAT.toString())) {
            if (!VirtualRouterGlobalProperty.ENABLE_MULTI_SNAT) {
                throw new ApiMessageInterceptionException(argerr("Could not apply snat with non-default public network, due to multi snat feature is disabled"));
            }
            if(!Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).eq(L3NetworkVO_.category, L3NetworkCategory.Public).isExists()){
                throw new ApiMessageInterceptionException(argerr("Could not apply snat with this L3Network, due to l3 network [uuid:%s] is not public network", msg.getL3NetworkUuid()));
            }
            if(!Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, msg.getL3NetworkUuid()).eq(VmNicVO_.vmInstanceUuid, msg.getUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("Could not apply snat with this L3Network, due to l3 network [uuid:%s] is not attached to vpc router", msg.getL3NetworkUuid()));
            }
        }
    }

    private void handle(VirtualRouterHaAPIMessage msg) {
        String masterUuid = getHaMasterRouter(msg.getVirtualRouterUuid());
        if (masterUuid == null) {
            return;
        }
        if (!masterUuid.equals(msg.getVirtualRouterUuid())) {
            msg.setVirtualRouterUuid(masterUuid);
        }
    }

    private void valiadateMonitorIps(List<String> monitorIps) {
        for (String ip : monitorIps) {
            if (!NetworkUtils.isValidIPAddress(ip)) {
                throw new ApiMessageInterceptionException(argerr("invalid monitor ip address [%s]", ip));
            }
        }
    }

    private void handle(APIChangeVpcHaGroupMonitorIpsMsg msg) {
        if (msg.getMonitorIps() == null) {
            msg.setMonitorIps(new ArrayList<>());
        } else {
            valiadateMonitorIps(msg.getMonitorIps());
        }
    }

    private void handle(APICreateVpcHaGroupMsg msg) {
        if (msg.getMonitorIps() == null) {
            msg.setMonitorIps(new ArrayList<>());
        } else {
            valiadateMonitorIps(msg.getMonitorIps());
        }
    }

    private void handle(APIAttachL3NetworkToVmMsg msg) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(msg.getVmInstanceUuid(), VpcRouterVmVO.class);
        if (vpcVo == null || !vpcVo.isHaEnabled()) {
            return;
        }

        String masterUuid = getHaMasterRouter(vpcVo.getUuid());
        if (masterUuid == null) {
            throw new ApiMessageInterceptionException(argerr("there is no master router of router [uuid:%s]", msg.getVmInstanceUuid()));
        }
        if (!masterUuid.equals(msg.getVmInstanceUuid())) {
            msg.setVmInstanceUuid(masterUuid);
        }

        String vpcHaUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vpcVo.getUuid());
        if (vpcHaUuid == null) {
            throw new ApiMessageInterceptionException(argerr("vpcHaRouter [uuid:%s] is deleted", vpcVo.getUuid()));
        }
    }

    private void handle(APIDetachL3NetworkFromVmMsg msg) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(msg.getVmInstanceUuid(), VpcRouterVmVO.class);
        if (vpcVo == null || !vpcVo.isHaEnabled()) {
            return;
        }

        String masterUuid = getHaMasterRouter(vpcVo.getUuid());
        if (masterUuid == null) {
            throw new ApiMessageInterceptionException(argerr("there is no master router of router [uuid:%s]", msg.getVmInstanceUuid()));
        }
    }

    private void handle(APICreateVpcVRouterMsg msg) {
        if (msg.getSystemTags() == null) {
            return;
        }

        for (String sysTag : msg.getSystemTags()) {
            if(!ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.getTagFormat(), sysTag);
            String haUuid = token.get(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN);

            if (Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid).count() >= 2) {
                throw new ApiMessageInterceptionException(argerr("there are more than 2 vpc routers attached to haGroup [uuid:%s]", haUuid));
            }

            /* check new created vpc router has same l3 network with ha group */
            List<String> vrUuids = Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid)
                    .select(VpcHaGroupApplianceVmRefVO_.uuid).limit(1).listValues();
            if (vrUuids == null || vrUuids.isEmpty()) {
                return;
            }

            VpcRouterVmVO vpc = dbf.findByUuid(vrUuids.get(0), VpcRouterVmVO.class);
            if (vpc == null) {
                return;
            }

            /* ManagementNetworkUuid and PublicNetworkUuid is same to uuid in the VirtualRouterOfferingVO */
            List<String> l3Uuids = new ArrayList<>();
            VpcRouterVmInventory inv = VpcRouterVmInventory.valueOf(vpc);
            l3Uuids.add(inv.getManagementNetworkUuid());
            if (!inv.getPublicNetworkUuid().equals(inv.getManagementNetworkUuid())) {
                l3Uuids.add(inv.getPublicNetworkUuid());
            }

            List<String> offeringL3Uuids = new ArrayList<>();
            VirtualRouterOfferingVO offeringVO = Q.New(VirtualRouterOfferingVO.class).eq(VirtualRouterOfferingVO_.uuid, msg.getVirtualRouterOfferingUuid()).find();
            offeringL3Uuids.add(offeringVO.getManagementNetworkUuid());
            if (!offeringVO.getManagementNetworkUuid().equals(offeringVO.getPublicNetworkUuid())) {
                offeringL3Uuids.add(offeringVO.getPublicNetworkUuid());
            }
            
            if (!l3Uuids.containsAll(offeringL3Uuids) || !offeringL3Uuids.containsAll(l3Uuids)) {
                throw new ApiMessageInterceptionException(argerr("ha group management l3 and public l3 networks[uuid:%s] are different from offering l3 networks [uuid:%s]",
                        l3Uuids, offeringL3Uuids));
            }
        }
    }

    private List<String> getVpcUserL3Uuids(String vrUuid)  {
        VpcRouterVmVO vpcVO = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        List<String> uuids = new ArrayList<>();
        String pubL3 = vpcVO.getPublicNetworkUuid();
        String mgtL3 = vpcVO.getManagementNetworkUuid();
        for (VmNicVO nic : vpcVO.getVmNics()) {
            if (mgtL3.equals(nic.getL3NetworkUuid())) {
                continue;
            }

            if (pubL3.equals(nic.getL3NetworkUuid())) {
                continue;
            }

            uuids.add(nic.getL3NetworkUuid());
        }

        return uuids;
    }

    private List<String> getVpcHaGroupL3Uuids(String haUuid)  {
        return Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName())
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid).listValues();
    }

    private String getHaGroupUuidFromSystemTags(List<String> systemTags) {
        if (systemTags == null || systemTags.isEmpty()) {
            return null;
        }

        for (String sysTag : systemTags) {
            if (!ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.getTagFormat(), sysTag);
            return token.get(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN);
        }

        return null;
    }

    private void validateVpcRouterStartInHaGroup(String vrUuid, String haUuid) {
        List<String> vpcL3Uuids = getVpcUserL3Uuids(vrUuid);
        List<String> vpcHaGroupL3Uuids = getVpcHaGroupL3Uuids(haUuid);
        if (vpcL3Uuids.containsAll(vpcHaGroupL3Uuids) && vpcHaGroupL3Uuids.containsAll(vpcL3Uuids)) {
            return;
        }

        throw new ApiMessageInterceptionException(argerr("vpc router l3 networks [uuid:%s] are different from ha group l3 networks [uuid:%s], " +
                        "!!! please delete this router and recreate it",
                vpcL3Uuids, vpcHaGroupL3Uuids));
    }

    private void validateAddVpcRouterToHaGroup(String vrUuid, String haUuid) {
        String oldHaUuid = ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.getTokenByResourceUuid(vrUuid, ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN);
        if (oldHaUuid != null) {
            throw new ApiMessageInterceptionException(argerr("vpc router has been attached to ha group [uuid:%s]", oldHaUuid));
        }

        if (!Q.New(VpcHaGroupVO.class).eq(VpcHaGroupVO_.uuid, haUuid).isExists()) {
            throw new ApiMessageInterceptionException(argerr("vpc ha group [uuid:%s] is not existed", haUuid));
        }

        /* this check is called when upgrade a old vpc to ha group, so ha group should not include other vpc router */
        if (Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid).count() >= 1) {
            throw new ApiMessageInterceptionException(argerr("there are more than 1 vpc routers attached to haGroup [uuid:%s]", haUuid));
        }

        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpcVo.getPublicNetworkUuid().equals(vpcVo.getManagementNetworkUuid())) {
            throw new ApiMessageInterceptionException(argerr("vpc router [uuid:%s] can not be upgraded to ha router because "+
                    "it public network is same to management network", haUuid));
        }
    }

    private void validateStartVpcRouterInHagroup(String vrUuid, List<String> systemTags) {
        VpcRouterVmVO vpcRouterVmVO = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        /* only check vpc router */
        if (vpcRouterVmVO == null) {
            return;
        }

        if (!vpcRouterVmVO.isHaEnabled()) {
            String haGroupUuid = getHaGroupUuidFromSystemTags(systemTags);
            if (haGroupUuid == null) {
                return;
            }

            validateAddVpcRouterToHaGroup(vrUuid, haGroupUuid);
        } else {
            String oldHaUuid = ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.getTokenByResourceUuid(vrUuid, ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN);
            validateVpcRouterStartInHaGroup(vrUuid, oldHaUuid);
        }
    }

    private void handle(APIStartVmInstanceMsg msg) {
        validateStartVpcRouterInHagroup(msg.getUuid(), msg.getSystemTags());
    }

    private void handle(APIRebootVmInstanceMsg msg) {
        validateStartVpcRouterInHagroup(msg.getUuid(), msg.getSystemTags());
    }
}
