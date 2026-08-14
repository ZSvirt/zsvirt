package org.zstack.header.host;

import org.zstack.core.db.Q;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;

public class SetIpOnHostNetworkInterfaceMsg extends NeedReplyMessage implements HostMessage {
    private String interfaceUuid;
    private String ipAddress;
    private String netmask;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getHostUuid() {
        return Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.hostUuid)
                .eq(HostNetworkInterfaceVO_.uuid, interfaceUuid).findValue();

    }
}
