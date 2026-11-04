package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class ConfigPrimaryVmMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private List<VmNicRedirectConfig> redirectConfigs;
    private String hostIp;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<VmNicRedirectConfig> getRedirectConfigs() {
        return redirectConfigs;
    }

    public void setRedirectConfigs(List<VmNicRedirectConfig> redirectConfigs) {
        this.redirectConfigs = redirectConfigs;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }
}
