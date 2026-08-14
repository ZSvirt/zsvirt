package org.zstack.header.host;

import org.zstack.core.db.Q;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;

public class SetIpOnHostNetworkBondingMsg extends NeedReplyMessage implements HostMessage {
    private String bondingUuid;
    private String ipAddress;
    private String netmask;

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
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
        return Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.hostUuid)
                .eq(HostNetworkBondingVO_.uuid, bondingUuid).findValue();

    }
}
