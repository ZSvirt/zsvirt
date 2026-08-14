package org.zstack.header.network.l2;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class BatchCheckNetworkPhysicalInterfaceMsg extends NeedReplyMessage implements HostMessage {
    private List<String> physicalInterfaces;
    private String hostUuid;

    public List<String> getPhysicalInterfaces() {
        return physicalInterfaces;
    }

    public void setPhysicalInterfaces(List<String> physicalInterfaces) {
        this.physicalInterfaces = physicalInterfaces;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
