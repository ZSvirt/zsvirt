package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RefreshSecurityGroupRulesOnVmMsg extends NeedReplyMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private boolean deleteAllRules;
    private List<String> vNicUuids;
    private List<String> sgUuids;

    public List<String> getNicUuids() { return vNicUuids; }
    public void setNicUuids(List<String> uuids) { this.vNicUuids = uuids; }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isDeleteAllRules() {
        return deleteAllRules;
    }

    public void setDeleteAllRules(boolean deleteAllRules) {
        this.deleteAllRules = deleteAllRules;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<String> getSgUuids() {
        return sgUuids;
    }

    public void setSgUuids(List<String> sgUuids) {
        this.sgUuids = sgUuids;
    }
}
