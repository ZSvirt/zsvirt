package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class ConfigPrimaryVmMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private List<VmNicRedirectConfig> configs;
    private String hostIp;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<VmNicRedirectConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<VmNicRedirectConfig> configs) {
        this.configs = configs;
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
