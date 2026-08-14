package org.zstack.network.securitygroup;

import java.util.List;

/**
 * Created by MaJin on 2017-06-29.
 */
public class HostSecurityGroupMembersTO {
    private List<String> hostUuids;
    private String hypervisorType;
    private SecurityGroupMembersTO groupMembersTO;

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setGroupMembersTO(SecurityGroupMembersTO groupMembersTO) {
        this.groupMembersTO = groupMembersTO;
    }

    public SecurityGroupMembersTO getGroupMembersTO() {
        return groupMembersTO;
    }
}
