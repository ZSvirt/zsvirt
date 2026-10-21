package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class VirtualRouterSystemTags {
    public static String PARALLELISM_DEGREE_TOKEN = "parallelismDegree";
    public static SystemTag VR_OFFERING_PARALLELISM_DEGREE = new SystemTag(String.format("commandsParallelismDegree::{%s}", PARALLELISM_DEGREE_TOKEN), InstanceOfferingVO.class);

    public static SystemTag VR_PARALLELISM_DEGREE = new SystemTag(String.format("commandsParallelismDegree::{%s}", PARALLELISM_DEGREE_TOKEN), VmInstanceVO.class);

    public static String VR_OFFERING_GUEST_NETWORK_TOKEN = "guestL3NetworkUuid";
    public static SystemTag VR_OFFERING_GUEST_NETWORK = new SystemTag(String.format("guestL3Network::{%s}", VR_OFFERING_GUEST_NETWORK_TOKEN), InstanceOfferingVO.class);

    public static SystemTag VR_DHCP_ROLE = new SystemTag("role::DHCP", VmInstanceVO.class);
    public static SystemTag VR_DNS_ROLE = new SystemTag("role::DNS", VmInstanceVO.class);
    public static SystemTag VR_SNAT_ROLE = new SystemTag("role::SNAT", VmInstanceVO.class);
    public static SystemTag VR_EIP_ROLE = new SystemTag("role::EIP", VmInstanceVO.class);
    public static SystemTag VR_PORT_FORWARDING_ROLE = new SystemTag("role::PortForwarding", VmInstanceVO.class);
    public static SystemTag VR_LB_ROLE = new SystemTag("role::LoadBalancer", VmInstanceVO.class);

    public static SystemTag DEDICATED_ROLE_VR = new SystemTag("dedicatedRole", VmInstanceVO.class);

    public static SystemTag VYOS_OFFERING = new SystemTag("vrouter", InstanceOfferingVO.class);
    public static SystemTag VR_DISABLE_NETWORK_SERVICE_SNAT =
            new SystemTag(String.format("disabledService::SNAT"), VirtualRouterVmVO.class);

    public static String VIRTUAL_ROUTER_OFFERING_TOKEN = "routerOffering";
    public static SystemTag VIRTUAL_ROUTER_OFFERING = new SystemTag(String.format("virtualRouterOffering::{%s}", VIRTUAL_ROUTER_OFFERING_TOKEN), L3NetworkVO.class);

    public static String VIRTUAL_ROUTER_LOGIN_USER_TOKEN = "loginUser";
    public static SystemTag VIRTUAL_ROUTER_LOGIN_USER = new SystemTag(String.format(
                    "loginUser::{%s}", VIRTUAL_ROUTER_LOGIN_USER_TOKEN ),
                    VirtualRouterVmVO.class);
}
