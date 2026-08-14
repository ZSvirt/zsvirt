package org.zstack.vpc.ha;

import org.zstack.appliancevm.ApplianceVmHaStatus;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.VpcRouterVmVO_;
import org.zstack.header.vpc.ha.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;

import java.util.ArrayList;
import java.util.List;


public class VpcHaGroupOperator {

    public static String getVpcHaRouterAffinityGroupName(String VpcHaName) {
        return "affinityGroup-for-vpcha-" + VpcHaName;
    }

    public static String getVpcHaGroupUuid(String vrUuid) {
        return Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid)
                .select(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid).findValue();
    }

    public static VpcHaGroupVO getVpcHaGroup(String vrUuid) {
        String vpcHaGroupUuid = getVpcHaGroupUuid(vrUuid);
        if (vpcHaGroupUuid == null) {
            return  null;
        }
        return Q.New(VpcHaGroupVO.class).eq(VpcHaGroupVO_.uuid, vpcHaGroupUuid).find();
    }

    public static String getVpcHaRouterPeerUuid(String vrUuid) {
        String vpcHaRouterUuid = Q.New(VpcHaGroupApplianceVmRefVO.class)
                .select(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid)
                .eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid)
                .findValue();
        if (vpcHaRouterUuid == null) {
            return null;
        }

        return Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, vpcHaRouterUuid)
                .notEq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid).select(VpcHaGroupApplianceVmRefVO_.uuid).findValue();
    }

    public static String getMasterUuidByVirtualRouterUuid(String vrUuid) {
        ApplianceVmVO appVo = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, vrUuid).find();
        if (ApplianceVmHaStatus.Master == appVo.getHaStatus()) {
            return vrUuid;
        }

        String peerUuid = getVpcHaRouterPeerUuid(vrUuid);
        if (peerUuid == null) {
            return null;
        }

        VpcRouterVmVO peerRouter = Q.New(VpcRouterVmVO.class).eq(VpcRouterVmVO_.uuid, peerUuid).find();
        if (peerRouter.getHaStatus() == ApplianceVmHaStatus.Master) {
            return peerRouter.getUuid();
        }

        return null;
    }

    public static List<String> getVpcUuidByVpcHaRouterUuid(String haUuid) {
        return Q.New(VpcHaGroupApplianceVmRefVO.class)
                .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid)
                .select(VpcHaGroupApplianceVmRefVO_.uuid).listValues();
    }

    public static String getMasterUuidByVpcHaRouterUuid(String haUuid) {
        List<String> vrUuids = Q.New(VpcHaGroupApplianceVmRefVO.class)
                .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid)
                .select(VpcHaGroupApplianceVmRefVO_.uuid).listValues();

        if (vrUuids == null || vrUuids.isEmpty()) {
            return null;
        }

        /* return master or first connected one or first one */
        String firstConnectedVpcUuid = null;
        List<VpcRouterVmVO> vrs = Q.New(VpcRouterVmVO.class).in(VpcRouterVmVO_.uuid, vrUuids).list();
        for (VpcRouterVmVO vr : vrs) {
            if (vr.getHaStatus() == ApplianceVmHaStatus.Master) {
                return vr.getUuid();
            }

            if (vr.getStatus() == ApplianceVmStatus.Connected) {
                firstConnectedVpcUuid = vr.getUuid();
            }
        }

        return firstConnectedVpcUuid != null ? firstConnectedVpcUuid : vrUuids.get(0);
    }

    public static boolean isLastHaGroupRouter(String routerUuid) {
        ApplianceVmVO appVo = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, routerUuid).find();
        if (appVo == null || !appVo.isHaEnabled()) {
            return true;
        }

        String vpcHaGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(appVo.getUuid());
        if (vpcHaGroupUuid == null) {
            return true;
        }

        return Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, vpcHaGroupUuid).count() <= 1;
    }

    public List<String> getVpcHaGroupSystemVipUuids(String haGroupUuid) {
        List<String> vipUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haGroupUuid)
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid).listValues();
        if (vipUuids.isEmpty()) {
            return vipUuids;
        }

        return Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).eq(VipVO_.system, true)
                .select(VipVO_.uuid).listValues();
    }

    /* this api for vpc network,  it return 1 or 0 value */
    public List<String> getVirtualRouterUuidsFromL3Uuid(String l3Uuid) {
        List<String> ret = new ArrayList<>();
        List<String> hagroupUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName())
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3Uuid)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).listValues();
        if (hagroupUuids != null && !hagroupUuids.isEmpty()) {
            ret.addAll(hagroupUuids);
            return ret;
        }

        hagroupUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3Uuid)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).listValues();
        if (hagroupUuids != null && !hagroupUuids.isEmpty()) {
            ret.addAll(hagroupUuids);
            return ret;
        }

        List<String> vrUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                .notNull(VmNicVO_.metaData).listValues();
        if (!vrUuids.isEmpty()) {
            ret.addAll(vrUuids);
        }
        return ret;
    }

    /* for public network, it may return multiple uuids */
    public List<String> getVirtualRouterUuidsFromPublicL3Uuid(String l3Uuid) {
        List<String> ret = new ArrayList<>();
        List<String> hagroupUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName())
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3Uuid)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).listValues();
        if (hagroupUuids != null && !hagroupUuids.isEmpty()) {
            ret.addAll(hagroupUuids);
        }

        hagroupUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3Uuid)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).listValues();
        if (hagroupUuids != null && !hagroupUuids.isEmpty()) {
            ret.addAll(hagroupUuids);
        }

        List<String> vrUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                .notNull(VmNicVO_.metaData).listValues();
        if (!vrUuids.isEmpty()) {
            ret.addAll(vrUuids);
        }
        return ret;
    }
}
