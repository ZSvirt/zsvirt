package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * author:kaicai.hu
 * Date:2021/8/5
 */
public class LocateHostNetworkInterfaceMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String networkInterfaceName;
    private Long interval = 15L;

    public String getNetworkInterfaceName() {
        return networkInterfaceName;
    }

    public void setNetworkInterfaceName(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }
}
