package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class VirtualRouterSystemTags {
    public static String PARALLELISM_DEGREE_TOKEN = "parallelismDegree";
    public static PatternedSystemTag VR_OFFERING_PARALLELISM_DEGREE = new PatternedSystemTag(String.format("commandsParallelismDegree::{%s}", PARALLELISM_DEGREE_TOKEN), InstanceOfferingVO.class);

    public static PatternedSystemTag VR_PARALLELISM_DEGREE = new PatternedSystemTag(String.format("commandsParallelismDegree::{%s}", PARALLELISM_DEGREE_TOKEN), VmInstanceVO.class);

    public static String VR_OFFERING_GUEST_NETWORK_TOKEN = "guestL3NetworkUuid";
    public static PatternedSystemTag VR_OFFERING_GUEST_NETWORK = new PatternedSystemTag(String.format("guestL3Network::{%s}", VR_OFFERING_GUEST_NETWORK_TOKEN), InstanceOfferingVO.class);

    public static PatternedSystemTag VR_DHCP_ROLE = new PatternedSystemTag("role::DHCP", VmInstanceVO.class);
    public static PatternedSystemTag VR_DNS_ROLE = new PatternedSystemTag("role::DNS", VmInstanceVO.class);
    public static PatternedSystemTag VR_SNAT_ROLE = new PatternedSystemTag("role::SNAT", VmInstanceVO.class);
    public static PatternedSystemTag VR_EIP_ROLE = new PatternedSystemTag("role::EIP", VmInstanceVO.class);
    public static PatternedSystemTag VR_PORT_FORWARDING_ROLE = new PatternedSystemTag("role::PortForwarding", VmInstanceVO.class);
    public static PatternedSystemTag VR_LB_ROLE = new PatternedSystemTag("role::LoadBalancer", VmInstanceVO.class);

    public static PatternedSystemTag DEDICATED_ROLE_VR = new PatternedSystemTag("dedicatedRole", VmInstanceVO.class);

    public static SystemTag VYOS_OFFERING = new PatternedSystemTag("vrouter", InstanceOfferingVO.class);
    public static PatternedSystemTag VR_DISABLE_NETWORK_SERVICE_SNAT =
            new PatternedSystemTag(String.format("disabledService::SNAT"), VirtualRouterVmVO.class);

    public static String VIRTUAL_ROUTER_OFFERING_TOKEN = "routerOffering";
    public static PatternedSystemTag VIRTUAL_ROUTER_OFFERING = new PatternedSystemTag(String.format("virtualRouterOffering::{%s}", VIRTUAL_ROUTER_OFFERING_TOKEN), L3NetworkVO.class);

    public static String VIRTUAL_ROUTER_LOGIN_USER_TOKEN = "loginUser";
    public static PatternedSystemTag VIRTUAL_ROUTER_LOGIN_USER = new PatternedSystemTag(String.format(
                    "loginUser::{%s}", VIRTUAL_ROUTER_LOGIN_USER_TOKEN ),
                    VirtualRouterVmVO.class);
}
