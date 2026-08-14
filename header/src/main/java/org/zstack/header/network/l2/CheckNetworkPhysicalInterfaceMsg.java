package org.zstack.header.network.l2;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class CheckNetworkPhysicalInterfaceMsg extends NeedReplyMessage implements HostMessage {
    private String physicalInterface;
    private String hostUuid;

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
