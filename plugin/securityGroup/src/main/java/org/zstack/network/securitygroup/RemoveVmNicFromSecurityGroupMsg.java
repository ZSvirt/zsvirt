package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 */
public class RemoveVmNicFromSecurityGroupMsg extends NeedReplyMessage {
    private String securityGroupUuid;
    private List<String> vmNicUuids;

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
}
