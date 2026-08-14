package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeHostNetworkInterfaceStateMsg extends NeedReplyMessage implements HostMessage {

    private String hostUuid;
    private String fromBond;
    private String interfaceName;
    private String interfaceStatus;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getFromBond() {
        return fromBond;
    }

    public void setFromBond(String fromBond) {
        this.fromBond = fromBond;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceStatus() {
        return interfaceStatus;
    }

    public void setInterfaceStatus(String interfaceStatus) {
        this.interfaceStatus = interfaceStatus;
    }
}
