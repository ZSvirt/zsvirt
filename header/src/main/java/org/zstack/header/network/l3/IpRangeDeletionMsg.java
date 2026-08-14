package org.zstack.header.network.l3;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class IpRangeDeletionMsg extends DeletionMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private String ipRangeUuid;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }
}
