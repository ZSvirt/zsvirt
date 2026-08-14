package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.configuration.InstanceOfferingState;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcMessage;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.ipsec.APICreateIPsecConnectionMsg;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO;
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 * Created by weiwang on 18/09/2017
 */
@InterceptorForService("vpc")
public class VpcApiInterceptor implements ApiMessageInterceptor , GlobalApiMessageInterceptor {
    
    private final static CLogger logger = Utils.getLogger(VpcApiInterceptor.class);

    @Autowired
    protected VpcManager vpcManager;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected VirtualRouterHaBackend haBackend;

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIAttachL3NetworkToVmMsg.class);
        ret.add(APIDetachL3NetworkFromVmMsg.class);
        ret.add(APICreateIPsecConnectionMsg.class);
        ret.add(APIAddIpRangeByNetworkCidrMsg.class);
        ret.add(APIAddIpRangeMsg.class);
        ret.add(APIAddIpv6RangeMsg.class);
        ret.add(APIAddIpv6RangeByNetworkCidrMsg.class);
        ret.add(APIDeleteVmNicMsg.class);
        ret.add(APIChangeVmNicStateMsg.class);
        return ret;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VpcMessage) {
            VpcMessage vmsg = (VpcMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VpcConstants.SERVICE_ID, vmsg.getVpcRouterUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVpcVRouterMsg) {
            validate((APICreateVpcVRouterMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            validate((APIDetachL3NetworkFromVmMsg) msg);
        } else if (msg instanceof APIGetVpcVRouterDistributedRoutingConnectionsMsg) {
            validate((APIGetVpcVRouterDistributedRoutingConnectionsMsg) msg);
        } else if (msg instanceof APIGetVpcVRouterDistributedRoutingEnabledMsg) {
            validate((APIGetVpcVRouterDistributedRoutingEnabledMsg) msg);
        } else if (msg instanceof APISetVpcVRouterDistributedRoutingEnabledMsg) {
            validate((APISetVpcVRouterDistributedRoutingEnabledMsg) msg);
        } else if (msg instanceof APIAddDnsToVpcRouterMsg) {
            validate((APIAddDnsToVpcRouterMsg) msg);
        } else if (msg instanceof APIRemoveDnsFromVpcRouterMsg) {
            validate((APIRemoveDnsFromVpcRouterMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            validate((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpRangeMsg) {
            validate((APIAddIpRangeMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            validate((APIAddIpv6RangeMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            validate((APIAddIpv6RangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIDeleteVmNicMsg) {
            validate((APIDeleteVmNicMsg) msg);
            return msg;
        } else if (msg instanceof APIChangeVmNicStateMsg) {
            validate((APIChangeVmNicStateMsg)msg);
            return msg;
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIDeleteVmNicMsg msg) {
        String vmInstanceUuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getUuid()).select(VmNicVO_.vmInstanceUuid).findValue();
        if (vmInstanceUuid == null) {
            return;
        }

        boolean isApplianceVm = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, vmInstanceUuid).isExists();
        if (isApplianceVm) {
            throw new ApiMessageInterceptionException(argerr("could not delete vm nic [uuid:%s], because vm[uuid:%s] type is appliance vm, please use API APIDetachL3NetworkFromVm", msg.getUuid(), vmInstanceUuid));
        }
    }

    private void validate(APIChangeVmNicStateMsg msg) {
        VmNicVO vmNicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        if (VirtualRouterNicMetaData.isManagementNic(vmNicVO)) {
            throw new ApiMessageInterceptionException(operr("could not update nic[uuid: %s] state, due to management nic not support",
                    msg.getVmNicUuid()));
        }
    }

    private Boolean checkVpcVRouterRunningAndConnected(String uuid) {
        VirtualRouterVmVO virtualRouterVmVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, uuid).find();
        boolean isVpcVrouterVm = VpcConstants.VPC_VROUTER_VM_TYPE.equals(virtualRouterVmVO.getApplianceVmType());

        if (!isVpcVrouterVm) {
            throw new ApiMessageInterceptionException(operr(String.format(
                    "the virtual router [uuid:%s] is not a vpc vrouter", uuid)));
        }

        return virtualRouterVmVO.getState().equals(VmInstanceState.Running) &&
                virtualRouterVmVO.getStatus().equals(ApplianceVmStatus.Connected);
    }

    private void validate(APIGetVpcVRouterDistributedRoutingConnectionsMsg msg) {
        if (checkVpcVRouterRunningAndConnected(msg.getUuid()).equals(false)) {
            throw new ApiMessageInterceptionException(operr(String.format(
                    "the virtual router [uuid:%s] is not in state [%s] or status [%s]",
                    msg.getUuid(), VmInstanceState.Running, ApplianceVmStatus.Connected)));
        }
    }

    private void validate(APIGetVpcVRouterDistributedRoutingEnabledMsg msg) {
        checkVpcVRouterRunningAndConnected(msg.getUuid());
    }

    private void validate(APISetVpcVRouterDistributedRoutingEnabledMsg msg) {
        if (checkVpcVRouterRunningAndConnected(msg.getUuid()).equals(false)) {
            throw new ApiMessageInterceptionException(operr(String.format(
                    "the virtual router [uuid:%s] is not in state [%s] or status [%s]",
                    msg.getUuid(), VmInstanceState.Running, ApplianceVmStatus.Connected)));
        }
    }

    private void validate(APIDetachL3NetworkFromVmMsg msg) {
        VmNicVO vmNicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vmNicVO.getL3NetworkUuid()).find();
        if (l3NetworkVO == null) { // ZSTAC-29441
            logger.warn(String.format("L3 network not found. Vmnic [uuid:%s]", msg.getVmNicUuid()));
            return;
        }

        String applianceVmType = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, vmNicVO.getVmInstanceUuid())
                .select(ApplianceVmVO_.applianceVmType).findValue();
        if (applianceVmType != null && !applianceVmType.equals(VpcConstants.VPC_VROUTER_VM_TYPE)) {
            return;
        }

        VpcRouterVmVO vpc = dbf.findByUuid(vmNicVO.getVmInstanceUuid(), VpcRouterVmVO.class);
        if (vpc != null) {
            if (vmNicVO.getL3NetworkUuid().equals(vpc.getManagementNetworkUuid())) {
                throw new ApiMessageInterceptionException(operr("management network can not be detached"));
            }

            if (vmNicVO.getL3NetworkUuid().equals(vpc.getDefaultRouteL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(operr("default route network can not be detached"));
            }

            if (vmNicVO.getL3NetworkUuid().equals(vpc.getPublicNetworkUuid())) {
                throw new ApiMessageInterceptionException(operr("original public network can not be detached"));
            }

            if (vpc.getState() != VmInstanceState.Running && vpc.getState() != VmInstanceState.Stopped) {
                throw new ApiMessageInterceptionException(operr("could not detach l3 network to vpc router[uuid:%s] because its state is not running or stopped",
                        vpc.getUuid()));
            }

            String peerUuid = haBackend.getVirtualRouterPeerUuid(vpc.getUuid());
            if (peerUuid != null) {
                VirtualRouterVmVO peerVo = dbf.findByUuid(peerUuid, VirtualRouterVmVO.class);
                if (peerVo.getState() != vpc.getState()) {
                    throw new ApiMessageInterceptionException(operr("could not detach l3 network to vpc router[uuid:%s] becaus the states of the master and slave are inconsistent",
                            vpc.getUuid()));
                }
            }
        }

        Boolean isVpcNetwork = VpcConstants.VPC_L3_NETWORK_TYPE.equals(l3NetworkVO.getType());
        if (isVpcNetwork.equals(false) && !l3NetworkVO.getCategory().equals(L3NetworkCategory.Public)) {
            return;
        }

        if (l3NetworkVO.isSystem()) {
            return;
        }

        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmNicVO.getVmInstanceUuid()).find();

        if (!vmInstanceVO.getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)){
            return;
        }

        List<String> peerNetworks = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid)
                                     .eq(VmNicVO_.vmInstanceUuid, vmInstanceVO.getUuid())
                                     .listValues();
        DebugUtils.Assert(!peerNetworks.isEmpty(), "there is no any nics in the vpc vrouter:${vmInstanceVO.getUuid()}");

        List<VipVO> vipVOs = Q.New(VipVO.class).eq(VipVO_.l3NetworkUuid, l3NetworkVO.getUuid()).eq(VipVO_.system, false).list();
        if (!vipVOs.isEmpty()) {
            List<String> vipUuids = vipVOs.stream().map(VipVO::getUuid).collect(Collectors.toList());

            List<VipPeerL3NetworkRefVO> vipPeerVOs = Q.New(VipPeerL3NetworkRefVO.class).in(VipPeerL3NetworkRefVO_.vipUuid, vipUuids)
                                                      .in(VipPeerL3NetworkRefVO_.l3NetworkUuid, peerNetworks).list();
            if (!vipPeerVOs.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("l3 network[uuid:%s] can not detach from vpc vrouter[uuid:%s] " +
                                "since network services attached vips[%s] still used in l3", l3NetworkVO.getUuid(), vmInstanceVO.getUuid(),
                        vipPeerVOs.stream().map(VipPeerL3NetworkRefVO::getVipUuid).collect(Collectors.toList())));
            }
        }

        if (!l3NetworkVO.getCategory().equals(L3NetworkCategory.Private)) {
            return;
        }

        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, l3NetworkVO.getUuid()).isNull(VmNicVO_.metaData)
                .notEq(VmNicVO_.uuid, msg.getVmNicUuid())
                .list();

        if (vmNicVOS != null && !vmNicVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("vpc l3 network[uuid:%s] can not detach from vpc vrouter[uuid:%s] " +
                    "since vm nics[%s] still used in l3", l3NetworkVO.getUuid(), vmInstanceVO.getUuid(),
                    vmNicVOS.stream().map(ResourceVO::getUuid).collect(Collectors.toList())));
        }
    }

    private void validate(APICreateVpcVRouterMsg msg) {
        VirtualRouterOfferingVO offeringVO = Q.New(VirtualRouterOfferingVO.class)
                .eq(VirtualRouterOfferingVO_.uuid, msg.getVirtualRouterOfferingUuid()).find();
        if (offeringVO.getState() != InstanceOfferingState.Enabled) {
            throw new ApiMessageInterceptionException(operr("virtual router offering[uuid: %s] is not enabled",
                    msg.getVirtualRouterOfferingUuid()));
        }
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();

        if (l3NetworkVO.isSystem() || !l3NetworkVO.getCategory().equals(L3NetworkCategory.Private)) {
            return;
        }

        boolean isVpcNetwork = VpcConstants.VPC_L3_NETWORK_TYPE.equals(l3NetworkVO.getType());
        boolean isVpcVrouterVm = false;
        if (!vmInstanceVO.getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)){
            return;
        } else {
            VirtualRouterVmVO virtualRouterVmVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, msg.getVmInstanceUuid()).find();
            isVpcVrouterVm = VpcConstants.VPC_VROUTER_VM_TYPE.equals(virtualRouterVmVO.getApplianceVmType());
            if (!isVpcNetwork && !isVpcVrouterVm) {
                return;
            }
            if (!(isVpcNetwork && isVpcVrouterVm)) {
                //flat net work cannot attach to vpc
                if (isVpcVrouterVm && l3NetworkVO.getCategory().toString().equals(L3NetworkCategory.Private.toString())) {
                    throw new ApiMessageInterceptionException(operr("only vpc l3 network can attach to vpc vrouter"));
                }
            }

            if (isVpcNetwork && isVpcVrouterVm) {
                List<VmNicVO> vmNics = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).list();
                if (!vmNics.isEmpty()) {
                    throw new ApiMessageInterceptionException(operr("Vpc network [uuid:%s] already attached to vpc router [uuid:%s]",
                            msg.getL3NetworkUuid(), vmNics.get(0).getVmInstanceUuid()));
                }
            }

            /* vpc l3 network can be attached only when vpc vrouter(or both master vpc and backup vpc) is running or stopped */
            if (!virtualRouterVmVO.isHaEnabled()) {
                if (virtualRouterVmVO.getState() == VmInstanceState.Stopped) {
                    msg.setApplyToBackend(false);
                } else if (virtualRouterVmVO.getState() != VmInstanceState.Running) {
                    throw new ApiMessageInterceptionException(operr("could not attached l3 network to vpc router[uuid:%s] because its state is not running or stopped",
                            msg.getVmInstanceUuid()));
                }
            } else {
                String peerUuid = haBackend.getVirtualRouterPeerUuid(virtualRouterVmVO.getUuid());
                if (peerUuid == null) {
                    if (virtualRouterVmVO.getState() == VmInstanceState.Stopped) {
                        msg.setApplyToBackend(false);
                    } else if (virtualRouterVmVO.getState() != VmInstanceState.Running) {
                        throw new ApiMessageInterceptionException(operr("could not attached l3 network to vpc router[uuid:%s] because its state is not running or stopped",
                                msg.getVmInstanceUuid()));
                    }
                } else {
                    VirtualRouterVmVO peerVo = dbf.findByUuid(peerUuid, VirtualRouterVmVO.class);
                    if (peerVo.getState() == VmInstanceState.Stopped && virtualRouterVmVO.getState() == VmInstanceState.Stopped) {
                        msg.setApplyToBackend(false);
                    } else if (!(virtualRouterVmVO.getState() == VmInstanceState.Running && peerVo.getState() == VmInstanceState.Running)) {
                        throw new ApiMessageInterceptionException(operr("could not attached l3 network to vpc router[uuid:%s] because both its state and it peer state is not running or stopped",
                                msg.getVmInstanceUuid()));
                    }
                }
            }
        }

        List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(l3NetworkVO);
        if (iprs.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("no ip ranges attached with l3 network[uuid:%s]", l3NetworkVO.getUuid()));
        }

        L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid()).find();
        if (l2NetworkVO.getAttachedClusterRefs() == null || l2NetworkVO.getAttachedClusterRefs().isEmpty()) {
            throw new ApiMessageInterceptionException(operr(String.format("l2 network[uuid: %s] of l3 network[uuid: %s] not attached to cluster[uuid: %s]",
                    l2NetworkVO.getUuid(), l3NetworkVO.getUuid(), vmInstanceVO.getClusterUuid())));
        }
        DebugUtils.Assert(l2NetworkVO.getAttachedClusterRefs().stream().anyMatch(c -> c.getClusterUuid().equals(vmInstanceVO.getClusterUuid())),
                String.format("l2 network[uuid: %s] of l3 network[uuid: %s] not attached to cluster[uuid: %s]",
                        l2NetworkVO.getUuid(), l3NetworkVO.getUuid(), vmInstanceVO.getClusterUuid()));

        if (l3NetworkVO.getType().equals(L3NetworkCategory.Private.toString())) {
            List<VipVO> vips = SQL.New("select vip from VipVO vip, VipPeerL3NetworkRefVO ref " +
                    "where ref.vipUuid = vip.uuid " +
                    "and ref.l3NetworkUuid = :l3Uuid")
                    .param("l3Uuid", msg.getL3NetworkUuid())
                    .list();
            if (vips != null && !vips.isEmpty()) {
                Set<String> l3Uuids = vmInstanceVO.getVmNics().stream()
                        .map(VmNicVO::getL3NetworkUuid)
                        .collect(Collectors.toSet());
                for (VipVO vip : vips) {
                    if (!l3Uuids.contains(vip.getL3NetworkUuid())) {
                        throw new ApiMessageInterceptionException(operr(
                                "public network[uuid: %s] vip[uuid: %s, ip: %s] peer with l3network[uuid: %s] not on vpc vr[uuid: %s]",
                                vip.getL3NetworkUuid(), vip.getUuid(), vip.getIp(), msg.getL3NetworkUuid(), vmInstanceVO.getUuid()
                        ));
                    }
                }
            }
        }

        final List<String> gateways = new ArrayList<>();
        List<IpRangeInventory> ip4rs = iprs.stream().filter(r -> r.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeInventory> ip6rs = iprs.stream().filter(r -> r.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
        if (!ip4rs.isEmpty()) {
            gateways.add(ip4rs.get(0).getGateway());
        }
        if (!ip6rs.isEmpty()) {
            gateways.add(ip6rs.get(0).getGateway());
        }

        if (vmInstanceVO.getVmNics() != null && !vmInstanceVO.getVmNics().isEmpty()) {
            if (vmInstanceVO.getVmNics().stream().anyMatch(n -> gateways.contains(n.getGateway()))) {
                throw new ApiMessageInterceptionException(operr("the gateway[ip:%s] of l3[uuid:%s] has been occupied",
                        gateways, msg.getL3NetworkUuid()));
            }
        }

        if (isVpcVrouterVm) {
            /* only vpc router nic ip is gateway ip */
            if (msg.getStaticIp() == null || gateways.contains(msg.getStaticIp())) {
                if (msg.getStaticIpMap().get(msg.getL3NetworkUuid()) != null) {
                    msg.getStaticIpMap().remove(msg.getL3NetworkUuid());
                }
                List<String> tagedGateways = gateways.stream().map(IPv6NetworkUtils::ipv6AddessToTagValue).collect(Collectors.toList());

                Map<String, List<String>> systemTagStaticIps = new StaticIpOperator().getStaticIpBySystemTag(msg.getSystemTags());
                if (systemTagStaticIps.isEmpty()) {
                    msg.getStaticIpMap().put(msg.getL3NetworkUuid(), tagedGateways);
                } else {
                    for (Map.Entry<String, List<String>> e : systemTagStaticIps.entrySet()) {
                        if (e.getValue().size() == 2) {
                            msg.getStaticIpMap().put(e.getKey(), e.getValue());
                        } else {
                            if (NetworkUtils.isIpv4Address(e.getValue().get(0))) {
                                ArrayList AddList = new ArrayList<String>();
                                tagedGateways.forEach(i->{
                                    if (!NetworkUtils.isIpv4Address(i)) {
                                        AddList.add(i);
                                    }
                                });
                                AddList.add(e.getValue().get(0));
                                msg.getStaticIpMap().put(msg.getL3NetworkUuid(), AddList);
                            } else {
                                ArrayList AddList = new ArrayList<String>();
                                tagedGateways.forEach(i->{
                                    if (NetworkUtils.isIpv4Address(i)) {
                                        AddList.add(i);
                                    }
                                });
                                AddList.add(e.getValue().get(0));
                                msg.getStaticIpMap().put(msg.getL3NetworkUuid(), AddList);
                            }
                        }
                    }
                }

            } else {
                throw new ApiMessageInterceptionException(operr("the static ip[%s] specified in message not equals to" +
                        " gateway ips[%s] of l3 network[uuid:%s]", msg.getStaticIp(), gateways, l3NetworkVO.getUuid()));
            }

            /* SLB backend network doesn't need this check */
            List<String> vipL3Uuids = SQL.New("select distinct vip.l3NetworkUuid from VipVO vip, VipPeerL3NetworkRefVO ref " +
                    " where ref.vipUuid = vip.uuid and ref.l3NetworkUuid = :l3Uuid")
                    .param("l3Uuid", msg.getL3NetworkUuid()).list();
            vipL3Uuids.remove(msg.getL3NetworkUuid());
            List<String> vrUuids = vmInstanceVO.getVmNics().stream().map(VmNicVO::getL3NetworkUuid).collect(Collectors.toList());
            for (String vipL3Uuid : vipL3Uuids) {
                if (!vrUuids.contains(vipL3Uuid)) {
                    throw new ApiMessageInterceptionException(operr("l3 network [uuid:%s] must be attached first, " +
                            "because there is vip on that l3 network", vipL3Uuid));
                }
            }
        }
    }

    private void validate(APIAddDnsToVpcRouterMsg msg) {
        if (NetworkUtils.isNotIpAddress(msg.getDns()) ) {
            throw new ApiMessageInterceptionException(argerr("dns[%s] is not a IP address", msg.getDns()));
        }
    }

    private void validate(APIRemoveDnsFromVpcRouterMsg msg) {
        List<String> dns = vpcManager.getAllDnsFromVpcRouter(msg.getUuid());
        if (!dns.contains(msg.getDns())) {
            throw new ApiMessageInterceptionException(operr("dns address [%s] is not added to vpc router [uuid:%s]", msg.getDns(), msg.getUuid()));
        }
    }

    private void validateVpcNic(IpRangeInventory ipr) {
        List<String> vpcUuids = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, ipr.getL3NetworkUuid())
                .notNull(VmNicVO_.metaData).select(VmNicVO_.vmInstanceUuid).listValues();
        if (vpcUuids.isEmpty()) {
            return;
        }

        for (String uuid : vpcUuids) {
            VirtualRouterVmVO virtualRouterVmVO = dbf.findByUuid(uuid, VirtualRouterVmVO.class);
            for (VmNicVO nic: virtualRouterVmVO.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(ipr.getL3NetworkUuid())) {
                    continue;
                }

                NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                        .eq(NormalIpRangeVO_.ipVersion, ipr.getIpVersion()).limit(1).find();
                if (ipRangeVO == null) {
                    continue;
                }

                if (ipRangeVO.getIpVersion() == IPv6Constants.IPv4 && NetworkUtils.isCidrOverlap(ipr.getNetworkCidr(),ipRangeVO.getNetworkCidr())) {
                    throw new ApiMessageInterceptionException(operr("could not add ip range to l3 network[uuid:%s], because it's overlap with cidr [%s] of vRouter [uuid:%s]",
                            ipr.getL3NetworkUuid(), ipRangeVO.getNetworkCidr(), uuid));
                } else if (ipRangeVO.getIpVersion() == IPv6Constants.IPv6
                        && (IPv6NetworkUtils.isIpv6InCidrRange(ipr.getStartIp(), ipRangeVO.getNetworkCidr())
                          ||IPv6NetworkUtils.isIpv6InCidrRange(ipRangeVO.getStartIp(), ipr.getNetworkCidr()))){
                    throw new ApiMessageInterceptionException(operr("could not add ipv6 range to l3 network[uuid:%s], because it's overlap with cidr [%s] of vRouter [uuid:%s]",
                            ipr.getL3NetworkUuid(), ipRangeVO.getNetworkCidr(), uuid));
                }
            }
        }
    }

    private void validate(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateVpcNic(ipr);
    }

    private void validate(APIAddIpv6RangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateVpcNic(ipr);
    }

    private void validate(APIAddIpRangeByNetworkCidrMsg msg) {
        List<IpRangeInventory> iprs = IpRangeInventory.fromMessage(msg);
        validateVpcNic(iprs.get(0));
    }

    private void validate(APIAddIpv6RangeByNetworkCidrMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        validateVpcNic(ipr);
    }
}
