package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetHostNetworkInterfaceByIpMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private List<String> ipAddresses;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
}
