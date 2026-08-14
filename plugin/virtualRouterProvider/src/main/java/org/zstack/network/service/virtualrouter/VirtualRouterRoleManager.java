package org.zstack.network.service.virtualrouter;

import org.zstack.tag.SystemTagCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 8/10/2015.
 */
public class VirtualRouterRoleManager {
    public void makeDhcpRole(String vrUuid) {
        SystemTagCreator creator = VirtualRouterSystemTags.VR_DHCP_ROLE.newSystemTagCreator(vrUuid);
        creator.ignoreIfExisting = true;
        creator.create();
    }

    public void makeDnsRole(String vrUuid) {
        SystemTagCreator creator = VirtualRouterSystemTags.VR_DNS_ROLE.newSystemTagCreator(vrUuid);
        creator.ignoreIfExisting = true;
        creator.create();
    }

    public void makeSnatRole(String vrUuid) {
        SystemTagCreator creator = VirtualRouterSystemTags.VR_SNAT_ROLE.newSystemTagCreator(vrUuid);
        creator.ignoreIfExisting = true;
        creator.create();
    }

    public void makeEipRole(String vrUuid) {
        SystemTagCreator creator = VirtualRouterSystemTags.VR_EIP_ROLE.newSystemTagCreator(vrUuid);
        creator.ignoreIfExisting = true;
        creator.create();
    }

    public void makePortForwardingRole(String vrUuid) {
        SystemTagCreator creator = VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.newSystemTagCreator(vrUuid);
        creator.ignoreIfExisting = true;
        creator.create();
    }

    public void makeLoadBalancerRole(String vrUuid) {
        SystemTagCreator creator = VirtualRouterSystemTags.VR_LB_ROLE.newSystemTagCreator(vrUuid);
        creator.ignoreIfExisting = true;
        creator.create();
    }

    public List<String> getAllRoles(String vrUuid) {
        List<String> roles = new ArrayList<String>();
        if (VirtualRouterSystemTags.VR_DHCP_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_DHCP_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_DNS_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_DNS_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_SNAT_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_SNAT_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_EIP_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_EIP_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.getTagFormat());
        }
        if (VirtualRouterSystemTags.VR_LB_ROLE.hasTag(vrUuid)) {
            roles.add(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat());
        }
        return roles;
    }
}
