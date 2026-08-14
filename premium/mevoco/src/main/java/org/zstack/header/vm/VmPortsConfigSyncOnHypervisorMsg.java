package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class VmPortsConfigSyncOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    private String vmInstanceUuid;

    private List<VmConfigSyncStruct.VmPortConfig> ports;

    private String hostname;

    private String defaultIP;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<VmConfigSyncStruct.VmPortConfig> getPorts() {return ports;}

    public void setPorts(List<VmConfigSyncStruct.VmPortConfig> ports) {this.ports = ports;}

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDefaultIP() {
        return defaultIP;
    }

    public void setDefaultIP(String defaultIP) {
        this.defaultIP = defaultIP;
    }
}
