package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.*;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.lb.APIAddBackendServerToServerGroupMsg;
import org.zstack.network.service.lb.LoadBalancerType;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.network.service.lb.LoadBalancerVO_;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO_;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.list;

/**
 */
@InterceptorForService("virtualRouter")
public class VirtualRouterApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    private void setServiceId(APIMessage msg) {
        /* APIReconnectVirtualRouterMsg, APIUpdateVirtualRouterMsg, APIFlushConfigToVirtualRouterMsg
        * is handled in vmInstanceManagerImpl, then call handler in virtualrouter */
        if (msg instanceof APIReconnectVirtualRouterMsg) {
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        } else if (msg instanceof APIUpdateVirtualRouterMsg) {
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        } else if (msg instanceof APIProvisionVirtualRouterConfigMsg) {
            APIProvisionVirtualRouterConfigMsg vmsg = (APIProvisionVirtualRouterConfigMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        } else if (msg instanceof VmInstanceMessage){
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VirtualRouterConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIQueryVirtualRouterOfferingMsg) {
            validate((APIQueryVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APICreateVirtualRouterOfferingMsg) {
            validate((APICreateVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterOfferingMsg) {
            validate((APIUpdateVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterMsg) {
            validate((APIUpdateVirtualRouterMsg) msg);
        } else if (msg instanceof APIReconnectVirtualRouterMsg) {
            validate((APIReconnectVirtualRouterMsg) msg);
        } else if (msg instanceof APIAddBackendServerToServerGroupMsg) {
            validate((APIAddBackendServerToServerGroupMsg)msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIAddBackendServerToServerGroupMsg msg) {
        String type = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, msg.getLoadBalancerUuid()).select(LoadBalancerVO_.type).findValue();
        if (!(type == LoadBalancerType.Shared.toString())) {
            return;
        }

        if (msg.getVmNics().isEmpty()) {
            boolean vrExist = Q.New(VirtualRouterLoadBalancerRefVO.class).eq(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid, msg.getLoadBalancerUuid()).isExists();
            if (!vrExist) {
                throw new ApiMessageInterceptionException(argerr("could not add server ip to load balancer server group, because share lb has no service provider, please add vmnic first"));
            }
        }
    }

    private void validate(APIUpdateVirtualRouterMsg msg) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(msg.getVmInstanceUuid(), VirtualRouterVmVO.class);

        if (msg.getDefaultRouteL3NetworkUuid().equals(vrVO.getDefaultRouteL3NetworkUuid())) {
            throw new ApiMessageInterceptionException(argerr("l3 uuid[:%s] is same to default network of virtual router [uuid:%s]",
                    msg.getDefaultRouteL3NetworkUuid(), msg.getVmInstanceUuid()));
        }

        VmNicVO target = null;
        for (VmNicVO nic : vrVO.getVmNics()) {
            if (VmNicHelper.getL3Uuids(nic).contains(msg.getDefaultRouteL3NetworkUuid())) {
                target = nic;
                break;
            }
        }

        if (target == null) {
            throw new ApiMessageInterceptionException(argerr("l3 uuid[:%s] is not attached to virtual router [uuid:%s]", msg.getDefaultRouteL3NetworkUuid(), msg.getVmInstanceUuid()));
        }

        if (!VirtualRouterNicMetaData.isPublicNic(target) && !VirtualRouterNicMetaData.isAddinitionalPublicNic(target)) {
            if (VirtualRouterNicMetaData.isManagementNic(target)) {
                throw new ApiMessageInterceptionException(argerr("could not set the default network, because l3 uuid[:%s] is management network", msg.getDefaultRouteL3NetworkUuid()));
            } else {
                throw new ApiMessageInterceptionException(argerr("could not set the default network, because l3 uuid[:%s] is not public network", msg.getDefaultRouteL3NetworkUuid()));
            }
        }
    }

    private void validate(APIUpdateVirtualRouterOfferingMsg msg) {
        if (msg.getIsDefault() != null) {
            if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                        "cannot change the default field of a virtual router offering; only admin can do the operation"
                ));
            }
        }

        if (msg.getImageUuid() != null) {
            SimpleQuery<ImageVO> q = dbf.createQuery(ImageVO.class);
            q.select(ImageVO_.mediaType, ImageVO_.format);
            q.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
            Tuple t = q.findTuple();
            ImageMediaType type = t.get(0, ImageMediaType.class);
            String format = t.get(1, String.class);

            if (type != ImageMediaType.RootVolumeTemplate) {
                throw new ApiMessageInterceptionException(argerr("image[uuid:%s]'s mediaType is %s, the mediaType of a virtual router image must be %s",
                                msg.getImageUuid(), type, ImageMediaType.RootVolumeTemplate));
            }

            if (ImageConstant.ISO_FORMAT_STRING.equals(format)) {
                throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is of format %s, cannot be used for virtual router", msg.getImageUuid(), format));
            }
        }
    }

    private boolean isIpv4RangeInSameCidr(List<IpRangeInventory> ipr1, List<IpRangeInventory> ipr2) {
        /* both has no ipv4 */
        if (ipr1.isEmpty() || ipr2.isEmpty()) {
            return false;
        }

        return NetworkUtils.isCidrOverlap(ipr1.get(0).getNetworkCidr(), ipr2.get(0).getNetworkCidr());
    }

    private boolean isIpv6RangeInSameCidr(List<IpRangeInventory> ipr1, List<IpRangeInventory> ipr2) {
        if (ipr1.isEmpty() || ipr2.isEmpty()) {
            return false;
        }

        return IPv6NetworkUtils.isIpv6CidrEqual(ipr1.get(0).getNetworkCidr(), ipr2.get(0).getNetworkCidr());
    }

    private boolean isNetworkAddressInCidr(String networkUuid1, String networkUuid2) {
        L3NetworkVO l3vo1 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, networkUuid1).find();
        L3NetworkVO l3vo2 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, networkUuid2).find();
        List<IpRangeInventory> ipInvs1 = IpRangeHelper.getNormalIpRanges(l3vo1);
        List<IpRangeInventory> ipInvs2 = IpRangeHelper.getNormalIpRanges(l3vo2);
        List<IpRangeInventory> ip4Invs1 = ipInvs1.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeInventory> ip4Invs2 = ipInvs2.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeInventory> ip6Invs1 = ipInvs1.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
        List<IpRangeInventory> ip6Invs2 = ipInvs2.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());

        return isIpv4RangeInSameCidr(ip4Invs1, ip4Invs2) || isIpv6RangeInSameCidr(ip6Invs1, ip6Invs2);
    }
    private void validate(APICreateVirtualRouterOfferingMsg msg) {
        if (msg.isDefault() != null) {
            if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                        "cannot create a virtual router offering with the default field set; only admin can do the operation"
                ));
            }
        }

        if (msg.getPublicNetworkUuid() == null) {
            msg.setPublicNetworkUuid(msg.getManagementNetworkUuid());
        }

        L3NetworkVO mgtL3 = dbf.findByUuid(msg.getManagementNetworkUuid(), L3NetworkVO.class);
        if (!mgtL3.getZoneUuid().equals(msg.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(argerr("management network[uuid:%s] is not in the same zone[uuid:%s] this offering is going to create",
                            msg.getManagementNetworkUuid(), msg.getZoneUuid()));
        }
        /* mgt network does not support ipv6 yet, TODO, will be implemented soon */
        if (mgtL3.getIpVersions().contains(IPv6Constants.IPv6) && !mgtL3.getIpVersions().contains(IPv6Constants.IPv4)) {
            throw new ApiMessageInterceptionException(argerr("can not create virtual router offering, because management network doesn't support ipv6 yet"));
        }

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            checkIfManagementNetworkReachable(msg.getManagementNetworkUuid());
        }

        SimpleQuery<L3NetworkVO> q = dbf.createQuery(L3NetworkVO.class);
        q.select(L3NetworkVO_.zoneUuid);
        q.add(L3NetworkVO_.uuid, Op.EQ, msg.getPublicNetworkUuid());
        String zoneUuid = q.findValue();
        if (!zoneUuid.equals(msg.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(argerr("public network[uuid:%s] is not in the same zone[uuid:%s] this offering is going to create",
                            msg.getManagementNetworkUuid(), msg.getZoneUuid()));
        }

        SimpleQuery<ImageVO> imq = dbf.createQuery(ImageVO.class);
        imq.select(ImageVO_.mediaType, ImageVO_.format);
        imq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        Tuple t = imq.findTuple();

        ImageMediaType type = t.get(0, ImageMediaType.class);
        if (type != ImageMediaType.RootVolumeTemplate) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s]'s mediaType is %s, the mediaType of a virtual router image must be %s",
                            msg.getImageUuid(), type, ImageMediaType.RootVolumeTemplate));
        }

        String format = t.get(1, String.class);
        if (ImageConstant.ISO_FORMAT_STRING.equals(format)) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is of format %s, cannot be used for virtual router", msg.getImageUuid(), format));
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.IN, list(msg.getPublicNetworkUuid(), msg.getManagementNetworkUuid()));
        List<NetworkServiceL3NetworkRefVO> nrefs= nq.list();
        for (NetworkServiceL3NetworkRefVO nref : nrefs) {
            if (NetworkServiceType.SNAT.toString().equals(nref.getNetworkServiceType())) {
                if (nref.getL3NetworkUuid().equals(msg.getManagementNetworkUuid())) {
                    throw new ApiMessageInterceptionException(argerr("the L3 network[uuid: %s] has the SNAT service enabled, it cannot be used as a management network", msg.getManagementNetworkUuid()));
                } else if (nref.getL3NetworkUuid().equals(msg.getPublicNetworkUuid())) {
                    throw new ApiMessageInterceptionException(argerr("the L3 network[uuid: %s] has the SNAT service enabled, it cannot be used as a public network", msg.getPublicNetworkUuid()));
                }
            }
        }

        if (!msg.getManagementNetworkUuid().equals(msg.getPublicNetworkUuid())) {
            if (isNetworkAddressInCidr(msg.getManagementNetworkUuid(), msg.getPublicNetworkUuid())) {
     throw new ApiMessageInterceptionException(argerr("the L3 network[uuid: %s] is same network address with [uuid: %s], it cannot be used for virtual router", msg.getManagementNetworkUuid(),msg.getPublicNetworkUuid()));
            }
        }
    }

    private void checkIfManagementNetworkReachable(String managementNetworkUuid) {
        SimpleQuery<NormalIpRangeVO> q = dbf.createQuery(NormalIpRangeVO.class);
        q.add(NormalIpRangeVO_.l3NetworkUuid, Op.EQ, managementNetworkUuid);
        List<NormalIpRangeVO> iprs = q.list();
        if (iprs.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("the management network[uuid:%s] doesn't have any IP range", managementNetworkUuid));
        }

        String startIp = iprs.get(0).getStartIp();
        if (!NetworkUtils.isIpRoutedByDefaultGateway(startIp)) {
            // the mgmt server is in the same subnet of the mgmt network
            return;
        }

        String gateway = iprs.get(0).getGateway();
        for (int i=0; i<3; i++) {
            if (NetworkUtils.isReachable(gateway, 2000)) {
                return;
            }

            ShellResult ret = ShellUtils.runAndReturn(String.format("ping -c 1 -W 2 %s", gateway));
            if (ret.isReturnCode(0)) {
                return;
            }
        }

        throw new ApiMessageInterceptionException(argerr("the management network[uuid:%s, gateway:%s] is not reachable", managementNetworkUuid, gateway));
    }

    private void validate(APIQueryVirtualRouterOfferingMsg msg) {
        boolean found = false;
        for (QueryCondition qcond : msg.getConditions()) {
            if ("type".equals(qcond.getName())) {
                qcond.setOp(QueryOp.EQ.toString());
                qcond.setValue(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
                found = true;
                break;
            }
        }

        if (!found) {
            msg.addQueryCondition("type", QueryOp.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
        }
    }

    private void validate(APIReconnectVirtualRouterMsg msg) {
        String vmInstanceUuid = msg.getVmInstanceUuid();
        if (UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            AgentVersionVO agentVersionVO = dbf.findByUuid(msg.getVmInstanceUuid(), AgentVersionVO.class);
            if (agentVersionVO == null) {
                agentVersionVO = new AgentVersionVO();
                agentVersionVO.setUuid(vmInstanceUuid);
                agentVersionVO.setAgentType("zvr-agent");
                dbf.persist(agentVersionVO);
            }
        }
    }


    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIReconnectVirtualRouterMsg.class,
            APIUpdateVirtualRouterMsg.class,
            APIProvisionVirtualRouterConfigMsg.class,
            APIAddBackendServerToServerGroupMsg.class,
            APICreateVirtualRouterOfferingMsg.class,
            APIQueryVirtualRouterOfferingMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
